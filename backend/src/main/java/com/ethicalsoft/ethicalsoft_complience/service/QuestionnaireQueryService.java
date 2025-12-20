package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireQueryService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ProjectRepository projectRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public QuestionnaireRawResponseDTO getQuestionnaireRaw(Long projectId, Integer questionnaireId) {
        try {
            log.info("[questionnaire-query] Buscando questionnaire(raw) projeto={} questionnaire={}", projectId, questionnaireId);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));
            assertUserCanAccessProject(project);

            Questionnaire questionnaire = questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            return QuestionnaireRawResponseDTO.builder()
                    .id(questionnaire.getId())
                    .name(questionnaire.getName())
                    .iteration(questionnaire.getIteration())
                    .weight(questionnaire.getWeight())
                    .applicationStartDate(questionnaire.getApplicationStartDate())
                    .applicationEndDate(questionnaire.getApplicationEndDate())
                    .projectId(questionnaire.getProject() != null ? questionnaire.getProject().getId() : null)
                    .stageId(questionnaire.getStage() != null ? questionnaire.getStage().getId() : null)
                    .iterationId(questionnaire.getIterationRef() != null ? questionnaire.getIterationRef().getId() : null)
                    .status(questionnaire.getStatus())
                    .build();
        } catch (Exception ex) {
            log.error("[questionnaire-query] Falha ao buscar questionnaire(raw) projeto={} questionnaire={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<QuestionnaireQuestionResponseDTO> searchQuestions(Long projectId,
                                                                  Integer questionnaireId,
                                                                  QuestionSearchFilterDTO filter,
                                                                  Pageable pageable) {
        try {
            log.info("[questionnaire-query] Buscando perguntas projeto={} questionnaire={} page={} size={} filtroTexto={} filtroRole={}",
                    projectId,
                    questionnaireId,
                    pageable != null ? pageable.getPageNumber() : null,
                    pageable != null ? pageable.getPageSize() : null,
                    filter != null ? filter.getQuestionText() : null,
                    filter != null ? filter.getRoleName() : null);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));
            assertUserCanAccessProject(project);

            // valida pertencimento
            questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            String questionText = filter != null ? filter.getQuestionText() : null;
            String roleName = filter != null ? filter.getRoleName() : null;

            return questionRepository.searchByQuestionnaireId(questionnaireId, questionText, roleName, pageable)
                    .map(this::toQuestionResponse);
        } catch (Exception ex) {
            log.error("[questionnaire-query] Falha ao buscar perguntas projeto={} questionnaire={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    private QuestionnaireQuestionResponseDTO toQuestionResponse(Question question) {
        List<Integer> stageIds = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream().map(s -> s.getId()).toList())
                .orElseGet(List::of);

        List<String> stageNames = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream()
                        .sorted(Comparator.comparing(s -> s.getName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(s -> s.getName())
                        .toList())
                .orElseGet(List::of);

        List<Long> roleIds = Optional.ofNullable(question.getRoles())
                .map(roles -> roles.stream().map(r -> r.getId()).toList())
                .orElseGet(List::of);

        return QuestionnaireQuestionResponseDTO.builder()
                .id(question.getId() != null ? question.getId().longValue() : null)
                .text(question.getValue())
                .stageIds(stageIds)
                .stageNames(stageNames)
                .roleIds(roleIds)
                .order(question.getId())
                .build();
    }

    private void assertUserCanAccessProject(Project project) {
        var currentUser = authService.getAuthenticatedUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        if (UserRoleEnum.ADMIN.equals(currentUser.getRole())) {
            return;
        }

        boolean isRepresentative = project.getRepresentatives() != null
                && project.getRepresentatives().stream().anyMatch(rep -> rep.getUser() != null && Objects.equals(rep.getUser().getId(), currentUser.getId()));

        if (!isRepresentative) {
            throw new AccessDeniedException("Usuário não possui acesso ao projeto " + project.getId());
        }
    }
}

