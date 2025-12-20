package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.mapper.QuestionnaireQuestionMapper;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ListQuestionnaireQuestionsUseCase {

    private final QuestionRepositoryPort questionRepositoryPort;
    private final QuestionnaireQuestionMapper questionnaireQuestionMapper;

    public Page<QuestionnaireQuestionResponseDTO> execute(Integer questionnaireId,
                                                          Pageable pageable,
                                                          String questionText,
                                                          String roleName) {
        boolean hasFilters = StringUtils.hasText(questionText) || StringUtils.hasText(roleName);

        if (hasFilters) {
            return questionRepositoryPort.searchByQuestionnaireId(questionnaireId, questionText, roleName, pageable)
                    .map(questionnaireQuestionMapper::toDto);
        }
        return questionRepositoryPort.findByQuestionnaireIdOrderByIdAsc(questionnaireId, pageable)
                .map(questionnaireQuestionMapper::toDto);
    }
}
