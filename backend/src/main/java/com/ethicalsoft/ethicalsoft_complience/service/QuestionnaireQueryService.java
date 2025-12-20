package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListQuestionnaireQuestionsUseCase;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.ProjectRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireQueryService implements QuestionnaireQueryPort {

    private final QuestionnaireRepositoryPort questionnaireRepository;
    private final ProjectRepositoryPort projectRepository;
    private final ListQuestionnaireQuestionsUseCase listQuestionnaireQuestionsUseCase;

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireRawResponseDTO getQuestionnaireRaw(Long projectId, Integer questionnaireId) {
        try {
            log.info("[questionnaire-query] Buscando questionnaire(raw) projeto={} questionnaire={}", projectId, questionnaireId);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

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

    @Override
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

            projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            String questionText = filter != null ? filter.getQuestionText() : null;
            String roleName = filter != null ? filter.getRoleName() : null;

            return listQuestionnaireQuestionsUseCase.execute(questionnaireId, pageable, questionText, roleName);
        } catch (Exception ex) {
            log.error("[questionnaire-query] Falha ao buscar perguntas projeto={} questionnaire={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }
}
