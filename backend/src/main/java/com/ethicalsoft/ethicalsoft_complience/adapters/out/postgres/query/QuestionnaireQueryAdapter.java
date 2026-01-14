package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.mapper.QuestionnaireQuestionMapper;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.ProjectRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireQueryAdapter implements QuestionnaireQueryPort {

    private final QuestionnaireRepositoryPort questionnaireRepository;
    private final ProjectRepositoryPort projectRepository;
    private final QuestionRepositoryPort questionRepositoryPort;
    private final QuestionnaireQuestionMapper questionnaireQuestionMapper;

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
                    .projectId(project != null ? project.getId() : null)
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
    public Page<QuestionnaireQuestionResponseDTO> searchQuestions(Integer questionnaireId,
                                                                  QuestionSearchFilterDTO filter,
                                                                  Pageable pageable) {
        try {
            log.info("[questionnaire-query] Buscando perguntas questionnaire={} page={} size={} filtroTexto={} filtroRole={}",
                    questionnaireId,
                    pageable != null ? pageable.getPageNumber() : null,
                    pageable != null ? pageable.getPageSize() : null,
                    filter != null ? filter.getQuestionText() : null,
                    filter != null ? filter.getRoleName() : null);

            questionnaireRepository.findById(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            String questionText = filter != null ? filter.getQuestionText() : null;
            String roleName = filter != null ? filter.getRoleName() : null;

            boolean hasFilters = StringUtils.hasText(questionText) || StringUtils.hasText(roleName);

            Page<Question> page = hasFilters
                    ? questionRepositoryPort.searchByQuestionnaireId(questionnaireId, questionText, roleName, pageable)
                    : questionRepositoryPort.findByQuestionnaireIdOrderByIdAsc(questionnaireId, pageable);

            return page.map(questionnaireQuestionMapper::toDto);
        } catch (Exception ex) {
            log.error("[questionnaire-query] Falha ao buscar perguntas questionnaire={}", questionnaireId, ex);
            throw ex;
        }
    }
}
