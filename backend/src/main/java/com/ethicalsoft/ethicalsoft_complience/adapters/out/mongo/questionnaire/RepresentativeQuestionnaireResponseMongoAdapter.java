package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.RepresentativeQuestionnaireResponseCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        for (Questionnaire questionnaire : questionnaires) {
            List<Question> questions = Optional.ofNullable(questionnaire.getQuestions()).map(ArrayList::new).orElseGet(ArrayList::new);
            List<QuestionnaireResponse.AnswerDocument> template = buildAnswerTemplate(questions);

            QuestionnaireResponse response = new QuestionnaireResponse();
            response.setProjectId(project.getId());
            response.setQuestionnaireId(questionnaire.getId());
            response.setRepresentativeId(representative.getId());
            response.setStageId(questionnaire.getStage() != null ? questionnaire.getStage().getId() : null);
            response.setStatus(QuestionnaireResponseStatus.PENDING);
            response.setAnswers(cloneAnswerTemplate(template));
            questionnaireResponseRepository.save(response);
        }
    }

    private List<QuestionnaireResponse.AnswerDocument> buildAnswerTemplate(List<Question> persistedQuestions) {
        if (persistedQuestions == null || persistedQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionnaireResponse.AnswerDocument> result = new ArrayList<>();
        for (Question question : persistedQuestions) {
            QuestionnaireResponse.AnswerDocument answer = new QuestionnaireResponse.AnswerDocument();
            answer.setQuestionId(question.getId().longValue());
            answer.setQuestionText(question.getValue());
            answer.setStageIds(Optional.ofNullable(question.getStages())
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(Stage::getId)
                    .filter(Objects::nonNull)
                    .toList());

            if (question.getRoles() != null) {
                answer.setRoleIds(question.getRoles().stream().map(Role::getId).filter(Objects::nonNull).toList());
            }
            result.add(answer);
        }
        return result;
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

