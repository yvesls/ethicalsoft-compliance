package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.LinkDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswerRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionnaireAnswerPolicy {

    private final LinkMapper linkMapper;

    public boolean syncAnswers(QuestionnaireResponse response, Questionnaire questionnaire) {
        Set<Question> pgQuestions = questionnaire.getQuestions();
        if (pgQuestions == null || pgQuestions.isEmpty()) {
            return false;
        }

        Set<Long> validQuestionIds = pgQuestions.stream()
                .map(q -> q.getId().longValue())
                .collect(Collectors.toSet());

        List<QuestionnaireResponse.AnswerDocument> answers = response.getAnswers();
        if (answers == null) {
            answers = new ArrayList<>();
            response.setAnswers(answers);
        } else if (!(answers instanceof ArrayList)) {
            answers = new ArrayList<>(answers);
            response.setAnswers(answers);
        }

        boolean changed = false;

        Iterator<QuestionnaireResponse.AnswerDocument> it = answers.iterator();
        while (it.hasNext()) {
            QuestionnaireResponse.AnswerDocument a = it.next();
            if (!validQuestionIds.contains(a.getQuestionId())) {
                log.info("[answer-policy] Removendo resposta órfã: questionId={} do questionário={}",
                        a.getQuestionId(), response.getQuestionnaireId());
                it.remove();
                changed = true;
            }
        }

        Set<Long> existingIds = answers.stream()
                .map(answer -> answer.getQuestionId())
                .collect(Collectors.toSet());

        for (Question q : pgQuestions) {
            Long qId = q.getId().longValue();
            if (!existingIds.contains(qId)) {
                QuestionnaireResponse.AnswerDocument newAnswer = new QuestionnaireResponse.AnswerDocument();
                newAnswer.setQuestionId(qId);
                newAnswer.setQuestionText(q.getValue());
                newAnswer.setStageIds(Optional.ofNullable(q.getStages())
                        .orElse(Collections.emptySet())
                        .stream()
                        .map(stage -> stage.getId())
                        .filter(Objects::nonNull)
                        .toList());
                newAnswer.setRoleIds(Optional.ofNullable(q.getRoles())
                        .orElse(Collections.emptySet())
                        .stream()
                        .map(role -> role.getId())
                        .filter(Objects::nonNull)
                        .toList());
                answers.add(newAnswer);
                changed = true;
                log.info("[answer-policy] Adicionando nova pergunta: questionId={} ao questionário={}",
                        qId, response.getQuestionnaireId());
            }
        }

        if (changed) {
            log.info("[answer-policy] Sincronização concluída questionário={}: {} respostas válidas",
                    response.getQuestionnaireId(), answers.size());
        }

        return changed;
    }

    public void applyAnswer(QuestionnaireAnswerRequestDTO dto,
                            Map<Long, QuestionnaireResponse.AnswerDocument> answerMap) {
        applyAnswer(dto, answerMap, false);
    }

    public void applyAnswer(QuestionnaireAnswerRequestDTO dto,
                            Map<Long, QuestionnaireResponse.AnswerDocument> answerMap,
                            boolean draft) {
        QuestionnaireResponse.AnswerDocument answer = answerMap.get(dto.getQuestionId());
        if (answer == null) {
            throw new BusinessException("Questão inválida para este questionário");
        }

        if (!draft) {
            if (dto.getResponse() == null) {
                throw new BusinessException("Resposta é obrigatória quando não estiver em modo rascunho.");
            }
            if (dto.getResponse() && CollectionUtils.isEmpty(dto.getAttachments())) {
                throw new BusinessException("Anexos são obrigatórios quando a resposta é 'Sim'.");
            }
            if (!dto.getResponse() && ObjectUtils.isNullOrEmpty(dto.getJustification())) {
                throw new BusinessException("Justificativa é obrigatória quando a resposta é 'Não'.");
            }
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
