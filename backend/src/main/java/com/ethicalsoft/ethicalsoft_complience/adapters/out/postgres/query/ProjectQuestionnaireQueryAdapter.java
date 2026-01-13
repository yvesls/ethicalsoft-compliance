package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQuestionnaireQueryPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectQuestionnaireQueryAdapter implements ProjectQuestionnaireQueryPort {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireSummaryBuilder questionnaireSummaryBuilder;

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

            QuestionnaireSummaryResponseDTO summary = questionnaireSummaryBuilder.build(questionnaire, representativesById);
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
                    .map(q -> questionnaireSummaryBuilder.build(q, representativesById))
                    .toList(), pageable, page.getTotalElements());
        } catch (Exception ex) {
            log.error("[project-questionnaire] Falha ao listar questionários do projeto={}", projectId, ex);
            throw ex;
        }
    }
}
