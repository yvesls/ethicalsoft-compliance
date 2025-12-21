package com.ethicalsoft.ethicalsoft_complience.adapters.persistence;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RespondentStatusDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectQuestionnaireQueryAdapter implements ProjectQuestionnaireQueryPort {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    @Override
    public ProjectQuestionnaireSummaryDTO getProjectQuestionnaireSummary(Long projectId, Integer questionnaireId) {
        try {
            log.info("[project-questionnaire] Resumindo questionário id={} do projeto {}", questionnaireId, projectId);
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            Set<Representative> projectRepresentatives = Optional.ofNullable(project.getRepresentatives()).orElse(Set.of());
            Map<Long, Representative> representativesById = projectRepresentatives.stream()
                    .collect(Collectors.toMap(Representative::getId, rep -> rep));

            QuestionnaireSummaryResponseDTO summary = buildQuestionnaireSummary(questionnaire, representativesById);
            return ProjectQuestionnaireSummaryDTO.builder()
                    .projectId(projectId)
                    .questionnaireId(questionnaireId)
                    .questionnaireName(summary.getName())
                    .stageName(summary.getStageName())
                    .iterationName(summary.getIterationName())
                    .applicationStartDate(summary.getApplicationStartDate())
                    .applicationEndDate(summary.getApplicationEndDate())
                    .overallStatus(summary.getProgressStatus())
                    .totalRespondents(summary.getTotalRespondents())
                    .responded(summary.getRespondedRespondents())
                    .pending(summary.getPendingRespondents())
                    .lastResponseAt(summary.getLastResponseAt())
                    .respondents(summary.getRespondents())
                    .build();
        } catch (Exception ex) {
            log.error("[project-questionnaire] Falha ao resumir questionário id={} projeto={}", questionnaireId, projectId, ex);
            throw ex;
        }
    }

    @Override
    public Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable, QuestionnaireSearchFilter filter) {
        try {
            log.info("[project-questionnaire] Listando questionários do projeto={} pagina={}", projectId, pageable.getPageNumber());
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            Set<Representative> projectRepresentatives = Optional.ofNullable(project.getRepresentatives()).orElse(Set.of());
            Map<Long, Representative> representativesById = projectRepresentatives.stream()
                    .collect(Collectors.toMap(Representative::getId, rep -> rep));

            Specification<Questionnaire> spec = Specification.where((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
            if (filter != null) {
                if (StringUtils.hasText(filter.name())) {
                    spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.stageName())) {
                    spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("stage").get("name")), filter.stageName().toLowerCase()));
                }
                if (StringUtils.hasText(filter.iterationName())) {
                    spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("iterationRef").get("name")), filter.iterationName().toLowerCase()));
                }
            }

            Page<Questionnaire> page = questionnaireRepository.findAll(spec, pageable);
            return new PageImpl<>(page.stream()
                    .map(q -> buildQuestionnaireSummary(q, representativesById))
                    .toList(), pageable, page.getTotalElements());
        } catch (Exception ex) {
            log.error("[project-questionnaire] Falha ao listar questionários do projeto={}", projectId, ex);
            throw ex;
        }
    }

    private QuestionnaireSummaryResponseDTO buildQuestionnaireSummary(Questionnaire questionnaire,
                                                                      Map<Long, Representative> representativesById) {
        List<QuestionnaireResponse> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireId(questionnaire.getProject().getId(), questionnaire.getId());

        Map<Long, QuestionnaireResponse> responseByRep = responses.stream()
                .filter(resp -> resp.getRepresentativeId() != null)
                .collect(Collectors.toMap(QuestionnaireResponse::getRepresentativeId, r -> r, (a, b) -> a));

        int totalRespondents = representativesById.size();
        AtomicInteger responded = new AtomicInteger();
        AtomicReference<LocalDateTime> lastResponseAt = new AtomicReference<>();
        List<RespondentStatusDTO> respondentStatus = representativesById.values().stream()
                .map(rep -> {
                    QuestionnaireResponse response = responseByRep.get(rep.getId());
                    QuestionnaireResponseStatus status = response != null ? response.getStatus() : QuestionnaireResponseStatus.PENDING;
                    LocalDateTime completedAt = response != null ? response.getSubmissionDate() : null;
                    if (status == QuestionnaireResponseStatus.COMPLETED) {
                        responded.getAndIncrement();
                        if (completedAt != null && (lastResponseAt.get() == null || completedAt.isAfter(lastResponseAt.get()))) {
                            lastResponseAt.set(completedAt);
                        }
                    }
                    return RespondentStatusDTO.builder()
                            .representativeId(rep.getId())
                            .name(rep.getUser().getFirstName() + " " + rep.getUser().getLastName())
                            .email(rep.getUser().getEmail())
                            .status(status)
                            .completedAt(completedAt)
                            .build();
                }).collect(Collectors.toList());

        int pending = Math.max(totalRespondents - responded.get(), 0);
        QuestionnaireResponseStatus progressStatus;
        if (responded.get() == 0) {
            progressStatus = QuestionnaireResponseStatus.PENDING;
        } else if (responded.get() < totalRespondents) {
            progressStatus = QuestionnaireResponseStatus.IN_PROGRESS;
        } else {
            progressStatus = QuestionnaireResponseStatus.COMPLETED;
        }

        return QuestionnaireSummaryResponseDTO.builder()
                .id(questionnaire.getId())
                .name(questionnaire.getName())
                .status(questionnaire.getStatus())
                .applicationStartDate(questionnaire.getApplicationStartDate())
                .applicationEndDate(questionnaire.getApplicationEndDate())
                .stageName(questionnaire.getStage() != null ? questionnaire.getStage().getName() : null)
                .iterationName(questionnaire.getIterationRef() != null ? questionnaire.getIterationRef().getName() : null)
                .totalRespondents(totalRespondents)
                .respondedRespondents(responded.get())
                .pendingRespondents(pending)
                .lastResponseAt(lastResponseAt.get())
                .progressStatus(progressStatus)
                .respondents(respondentStatus)
                .build();
    }
}
