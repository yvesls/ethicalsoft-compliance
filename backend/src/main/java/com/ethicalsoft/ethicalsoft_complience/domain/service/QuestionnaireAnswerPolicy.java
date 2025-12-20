package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.LinkDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireAnswerRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionnaireAnswerPolicy {

    private final LinkMapper linkMapper;

    public void applyAnswer(QuestionnaireAnswerRequestDTO dto,
                            Map<Long, QuestionnaireResponse.AnswerDocument> answerMap) {
        QuestionnaireResponse.AnswerDocument answer = answerMap.get(dto.getQuestionId());
        if (answer == null) {
            throw new BusinessException("Questão inválida para este questionário");
        }

        if (Boolean.TRUE.equals(dto.getResponse()) && CollectionUtils.isEmpty(dto.getAttachments())) {
            throw new BusinessException("Anexos são obrigatórios quando a resposta é 'Sim'.");
        }
        if (Boolean.FALSE.equals(dto.getResponse()) && ObjectUtil.isNullOrEmpty(dto.getJustification())) {
            throw new BusinessException("Justificativa é obrigatória quando a resposta é 'Não'.");
        }

        answer.setResponse(dto.getResponse());
        answer.setJustification(linkMapper.toDocument(dto.getJustification()));
        answer.setEvidence(linkMapper.toDocument(dto.getEvidence()));
        answer.setAttachments(Optional.ofNullable(dto.getAttachments())
                .map(list -> list.stream()
                        .flatMap(link -> Optional.ofNullable(linkMapper.toDocument(link)).stream())
                        .toList())
                .orElseGet(List::of));
    }

    public QuestionnaireResponse.AnswerDocument toDocument(Long questionId, String questionText, List<Integer> stageIds, List<Long> roleIds) {
        QuestionnaireResponse.AnswerDocument doc = new QuestionnaireResponse.AnswerDocument();
        doc.setQuestionId(questionId);
        doc.setQuestionText(questionText);
        doc.setStageIds(stageIds);
        doc.setRoleIds(roleIds);
        return doc;
    }

    public LinkDTO toDto(QuestionnaireResponse.LinkDocument doc) {
        return linkMapper.toDto(doc);
    }

    public QuestionnaireResponse.LinkDocument toDocument(LinkDTO dto) {
        return linkMapper.toDocument(dto);
    }
}

