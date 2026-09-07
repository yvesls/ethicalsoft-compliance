package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.mapper.QuestionnaireQuestionMapper;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.ProjectRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.RepresentativeRepositoryPort;
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
    private final RepresentativeRepositoryPort representativeRepositoryPort;

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
            java.util.List<Long> roleIds = filter != null ? filter.getRoleIds() : null;

            boolean hasTextFilters = StringUtils.hasText(questionText) || StringUtils.hasText(roleName);
            boolean hasRoleIds = roleIds != null && !roleIds.isEmpty();

            Page<Question> page;
            if (hasTextFilters && hasRoleIds) {
                page = questionRepositoryPort.searchByQuestionnaireIdAndRoleIds(questionnaireId, questionText, roleName, roleIds, pageable);
            } else if (hasTextFilters) {
                page = questionRepositoryPort.searchByQuestionnaireId(questionnaireId, questionText, roleName, pageable);
            } else if (hasRoleIds) {
                page = questionRepositoryPort.findByQuestionnaireIdAndRoleIds(questionnaireId, roleIds, pageable);
            } else {
                page = questionRepositoryPort.findByQuestionnaireIdOrderByIdAsc(questionnaireId, pageable);
            }

            return page.map(questionnaireQuestionMapper::toDto);
        } catch (Exception ex) {
            log.error("[questionnaire-query] Falha ao buscar perguntas questionnaire={}", questionnaireId, ex);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Long> findRepresentativeRoleIds(Long projectId, Long representativeId) {
        Representative representative = representativeRepositoryPort.findById(representativeId)
                .orElseThrow(() -> new EntityNotFoundException("Representante não encontrado: " + representativeId));
        if (representative.getProject() == null || !java.util.Objects.equals(representative.getProject().getId(), projectId)) {
            throw new EntityNotFoundException("Representante não pertence ao projeto: " + projectId);
        }
        return java.util.Optional.ofNullable(representative.getRoles())
                .orElse(java.util.Collections.emptySet())
                .stream()
                .map(role -> role.getId())
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
