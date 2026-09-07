package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.RepresentativeQuestionnaireResponseCommandPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RepresentativeQuestionnaireResponseMongoAdapter implements RepresentativeQuestionnaireResponseCommandPort {

    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    @Override
    public void createResponsesForRepresentative(Project project, Representative representative) {
        if (project == null || representative == null) {
            return;
        }

        var questionnaires = Optional.ofNullable(project.getQuestionnaires()).orElse(Collections.emptySet());
        if (questionnaires.isEmpty()) {
            return;
        }

        Set<Long> representativeRoleIds = Optional.ofNullable(representative.getRoles())
                .orElse(Collections.emptySet())
                .stream()
                .map(role -> role.getId())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        for (Questionnaire questionnaire : questionnaires) {

            if (questionnaireResponseRepository
                    .findByProjectIdAndQuestionnaireIdAndRepresentativeId(project.getId(), questionnaire.getId(), representative.getId())
                    .isPresent()) {
                continue;
            }

            QuestionnaireResponse base = questionnaireResponseRepository
                    .findByProjectIdAndQuestionnaireIdAndRepresentativeId(project.getId(), questionnaire.getId(), null)
                    .orElse(null);

            List<QuestionnaireResponse.AnswerDocument> template;
            Integer stageId;
            if (base != null) {
                template = filterTemplateByRoles(
                        cloneAnswerTemplate(Optional.ofNullable(base.getAnswers()).orElse(Collections.emptyList())),
                        representativeRoleIds
                );
                stageId = base.getStageId();
            } else {
                List<Question> questions = Optional.ofNullable(questionnaire.getQuestions()).map(ArrayList::new).orElseGet(ArrayList::new);
                template = buildAnswerTemplate(questions, representativeRoleIds);
                template = cloneAnswerTemplate(template);
                stageId = questionnaire.getStage() != null ? questionnaire.getStage().getId() : null;
            }

            if (template.isEmpty()) {
                throw new BusinessException("Representante sem perguntas vinculadas ao questionário '" + questionnaire.getName() + "'.");
            }

            QuestionnaireResponse response = new QuestionnaireResponse();
            response.setProjectId(project.getId());
            response.setQuestionnaireId(questionnaire.getId());
            response.setRepresentativeId(representative.getId());
            response.setStageId(stageId);
            response.setStatus(QuestionnaireResponseStatus.PENDING);
            response.setAnswers(template);
            questionnaireResponseRepository.save(response);
        }
    }

    private List<QuestionnaireResponse.AnswerDocument> buildAnswerTemplate(List<Question> persistedQuestions,
                                                                          Set<Long> representativeRoleIds) {
        if (persistedQuestions == null || persistedQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionnaireResponse.AnswerDocument> result = new ArrayList<>();
        for (Question question : persistedQuestions) {
            if (!hasMatchingRole(question, representativeRoleIds)) {
                continue;
            }
            QuestionnaireResponse.AnswerDocument answer = new QuestionnaireResponse.AnswerDocument();
            answer.setQuestionId(question.getId().longValue());
            answer.setQuestionText(question.getValue());
            answer.setStageIds(Optional.ofNullable(question.getStages())
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(stage -> stage.getId())
                    .filter(Objects::nonNull)
                    .toList());

            if (question.getRoles() != null) {
                answer.setRoleIds(question.getRoles().stream().map(role -> role.getId()).filter(Objects::nonNull).toList());
            }
            result.add(answer);
        }
        return result;
    }

    private List<QuestionnaireResponse.AnswerDocument> filterTemplateByRoles(List<QuestionnaireResponse.AnswerDocument> template,
                                                                             Set<Long> representativeRoleIds) {
        if (template == null || template.isEmpty()) {
            return Collections.emptyList();
        }
        if (representativeRoleIds == null || representativeRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return template.stream()
                .filter(answer -> answer.getRoleIds() != null && answer.getRoleIds().stream().anyMatch(representativeRoleIds::contains))
                .toList();
    }

    private boolean hasMatchingRole(Question question, Set<Long> representativeRoleIds) {
        if (question == null || representativeRoleIds == null || representativeRoleIds.isEmpty()) {
            return false;
        }
        return Optional.ofNullable(question.getRoles())
                .orElse(Collections.emptySet())
                .stream()
                .map(role -> role.getId())
                .filter(Objects::nonNull)
                .anyMatch(representativeRoleIds::contains);
    }

    private List<QuestionnaireResponse.AnswerDocument> cloneAnswerTemplate(List<QuestionnaireResponse.AnswerDocument> template) {
        if (template == null || template.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionnaireResponse.AnswerDocument> clones = new ArrayList<>();
        for (QuestionnaireResponse.AnswerDocument original : template) {
            QuestionnaireResponse.AnswerDocument copy = new QuestionnaireResponse.AnswerDocument();
            copy.setQuestionId(original.getQuestionId());
            copy.setQuestionText(original.getQuestionText());
            copy.setStageIds(original.getStageIds() != null ? new ArrayList<>(original.getStageIds()) : null);
            copy.setRoleIds(new ArrayList<>(Optional.ofNullable(original.getRoleIds()).orElse(Collections.emptyList())));
            copy.setResponse(null);
            copy.setJustification(null);
            copy.setEvidence(null);
            copy.setAttachments(new ArrayList<>());
            clones.add(copy);
        }
        return clones;
    }
}
