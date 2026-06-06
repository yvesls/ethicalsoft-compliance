package com.ethicalsoft.ethicalsoft_complience.application.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse.AnswerDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.RepresentativeQuestionnaireResponseCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectUpdateResponseSyncService {

    private final QuestionnaireResponseRepository responseRepository;
    private final RepresentativeQuestionnaireResponseCommandPort responseCommandPort;

    public int createResponsesForNewRepresentative(Project project, Representative representative) {
        log.info("[response-sync] Criando respostas para novo representante id={} projeto id={}",
                representative.getId(), project.getId());
        responseCommandPort.createResponsesForRepresentative(project, representative);
        return countResponsesForRepresentative(project.getId(), representative.getId());
    }

    public int deleteResponsesForRemovedRepresentative(Long projectId, Long representativeId) {
        log.info("[response-sync] Removendo respostas do representante id={} do projeto id={}", representativeId, projectId);
        List<QuestionnaireResponse> responses = responseRepository.findByProjectId(projectId).stream()
                .filter(r -> Objects.equals(r.getRepresentativeId(), representativeId))
                .filter(r -> r.getStatus() != QuestionnaireResponseStatus.COMPLETED)
                .toList();

        responses.forEach(r -> responseRepository.deleteById(r.getId()));
        log.info("[response-sync] {} respostas removidas para representante id={}", responses.size(), representativeId);
        return responses.size();
    }

    public int addQuestionToResponses(Integer questionnaireId, Question question, Set<Representative> representatives) {
        log.info("[response-sync] Adicionando pergunta id={} às respostas do questionário id={}", question.getId(), questionnaireId);

        Set<Long> questionRoleIds = question.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        List<QuestionnaireResponse> responses = responseRepository.findByQuestionnaireId(questionnaireId);
        int updated = 0;

        for (QuestionnaireResponse resp : responses) {
            if (resp.getStatus() == QuestionnaireResponseStatus.COMPLETED) continue;

            Representative rep = representatives.stream()
                    .filter(r -> Objects.equals(r.getId(), resp.getRepresentativeId()))
                    .findFirst().orElse(null);

            if (rep == null) continue;

            boolean repHasRole = rep.getRoles().stream()
                    .anyMatch(r -> questionRoleIds.contains(r.getId()));

            if (!repHasRole) continue;

            boolean alreadyHas = resp.getAnswers() != null && resp.getAnswers().stream()
                    .anyMatch(a -> Objects.equals(a.getQuestionId(), Long.valueOf(question.getId())));

            if (alreadyHas) continue;

            AnswerDocument newAnswer = new AnswerDocument();
            newAnswer.setQuestionId(Long.valueOf(question.getId()));
            newAnswer.setQuestionText(question.getValue());
            newAnswer.setStageIds(question.getStages() != null
                    ? question.getStages().stream().map(Stage::getId).toList()
                    : List.of());
            newAnswer.setRoleIds(new ArrayList<>(questionRoleIds));
            newAnswer.setResponse(null);

            if (resp.getAnswers() == null) {
                resp.setAnswers(new ArrayList<>());
            }
            resp.getAnswers().add(newAnswer);
            responseRepository.save(resp);
            updated++;
        }

        log.info("[response-sync] Pergunta id={} adicionada a {} respostas", question.getId(), updated);
        return updated;
    }

    public int removeQuestionFromResponses(Integer questionnaireId, Integer questionId) {
        log.info("[response-sync] Removendo pergunta id={} das respostas do questionário id={}", questionId, questionnaireId);

        List<QuestionnaireResponse> responses = responseRepository.findByQuestionnaireId(questionnaireId);
        int updated = 0;

        for (QuestionnaireResponse resp : responses) {
            if (resp.getStatus() == QuestionnaireResponseStatus.COMPLETED) continue;
            if (resp.getAnswers() == null) continue;

            boolean removed = resp.getAnswers().removeIf(
                    a -> Objects.equals(a.getQuestionId(), Long.valueOf(questionId)));

            if (removed) {
                responseRepository.save(resp);
                updated++;
            }
        }

        log.info("[response-sync] Pergunta id={} removida de {} respostas", questionId, updated);
        return updated;
    }

    public void updateQuestionTextInResponses(Integer questionnaireId, Integer questionId, String newText) {
        List<QuestionnaireResponse> responses = responseRepository.findByQuestionnaireId(questionnaireId);
        int updated = 0;

        for (QuestionnaireResponse resp : responses) {
            if (resp.getAnswers() == null) continue;
            boolean changed = false;
            for (AnswerDocument answer : resp.getAnswers()) {
                if (Objects.equals(answer.getQuestionId(), Long.valueOf(questionId))) {
                    answer.setQuestionText(newText);
                    changed = true;
                }
            }
            if (changed) {
                responseRepository.save(resp);
                updated++;
            }
        }

    }

    public int deleteResponsesForQuestionnaire(Long projectId, Integer questionnaireId) {
        List<QuestionnaireResponse> responses = responseRepository.findByProjectIdAndQuestionnaireId(projectId, questionnaireId);
        int count = 0;
        for (QuestionnaireResponse resp : responses) {
            if (resp.getStatus() != QuestionnaireResponseStatus.COMPLETED) {
                responseRepository.deleteById(resp.getId());
                count++;
            }
        }
        return count;
    }

    private int countResponsesForRepresentative(Long projectId, Long representativeId) {
        return (int) responseRepository.findByProjectId(projectId).stream()
                .filter(r -> Objects.equals(r.getRepresentativeId(), representativeId))
                .count();
    }
}

