package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ProjectUpdateValidationPolicy {

    public ValidationResult validate(Project project,
                                     boolean hasProjectIsepResult,
                                     Map<Integer, Boolean> questionnaireHasResult,
                                     Map<Integer, List<QuestionnaireResponse>> responsesByQuestionnaire) {

        List<String> blocked = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (project.getStatus() != ProjectStatusEnum.ABERTO) {
            blocked.add("Somente projetos com status ABERTO podem ser atualizados. Status atual: " + project.getStatus());
        }

        if (hasProjectIsepResult) {
            blocked.add("Projeto já possui resultado ISEP consolidado. Não é possível editar.");
        }

        return new ValidationResult(blocked, warnings);
    }

    public List<String> validateQuestionnaireRemoval(Questionnaire questionnaire,
                                                     boolean hasResult,
                                                     List<QuestionnaireResponse> responses) {
        List<String> blocked = new ArrayList<>();

        if (hasResult) {
            blocked.add("Questionário '" + questionnaire.getName() + "' já possui resultado ISEP. Não pode ser removido.");
            return blocked;
        }

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Questionário '" + questionnaire.getName() + "' já está CONCLUÍDO. Não pode ser removido.");
            return blocked;
        }

        boolean hasCompleted = responses != null && responses.stream()
                .anyMatch(r -> r.getStatus() == QuestionnaireResponseStatus.COMPLETED);
        if (hasCompleted) {
            blocked.add("Questionário '" + questionnaire.getName() + "' possui respostas COMPLETED. Não pode ser removido.");
        }

        return blocked;
    }

    public List<String> validateQuestionRemoval(Question question,
                                                Questionnaire questionnaire,
                                                List<QuestionnaireResponse> responses) {
        List<String> blocked = new ArrayList<>();

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Não é possível remover pergunta '" + truncate(question.getValue()) + "' de questionário CONCLUÍDO.");
            return blocked;
        }

        if (responses != null) {
            for (QuestionnaireResponse resp : responses) {
                if (resp.getStatus() == QuestionnaireResponseStatus.COMPLETED) {
                    boolean answered = resp.getAnswers() != null && resp.getAnswers().stream()
                            .anyMatch(a -> Objects.equals(a.getQuestionId(), Long.valueOf(question.getId())) && a.getResponse() != null);
                    if (answered) {
                        blocked.add("Pergunta '" + truncate(question.getValue()) + "' já foi respondida por representante (resposta COMPLETED). Não pode ser removida.");
                        break;
                    }
                }
            }
        }

        return blocked;
    }

    public List<String> validateQuestionAddition(Questionnaire questionnaire,
                                                 List<QuestionnaireResponse> responses) {
        List<String> blocked = new ArrayList<>();

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Não é possível adicionar perguntas em questionário CONCLUÍDO '" + questionnaire.getName() + "'.");
            return blocked;
        }

        if (responses != null) {
            boolean hasCompleted = responses.stream()
                    .anyMatch(r -> r.getStatus() == QuestionnaireResponseStatus.COMPLETED);
            if (hasCompleted && (questionnaire.getStatus() == TimelineStatusEnum.EM_ANDAMENTO
                    || questionnaire.getStatus() == TimelineStatusEnum.ATRASADO)) {
                blocked.add("Questionário '" + questionnaire.getName() + "' possui respostas COMPLETED. Não é possível adicionar novas perguntas.");
            }
        }

        return blocked;
    }

    public List<String> validateRepresentativeRemoval(Representative rep,
                                                      Map<Integer, List<QuestionnaireResponse>> responsesByQuestionnaire,
                                                      Map<Integer, Boolean> questionnaireHasResult) {
        List<String> blocked = new ArrayList<>();

        if (responsesByQuestionnaire != null) {
            for (var entry : responsesByQuestionnaire.entrySet()) {
                Integer qId = entry.getKey();
                List<QuestionnaireResponse> responses = entry.getValue();
                boolean hasResult = questionnaireHasResult.getOrDefault(qId, false);

                boolean repHasCompleted = responses.stream()
                        .anyMatch(r -> Objects.equals(r.getRepresentativeId(), rep.getId())
                                && r.getStatus() == QuestionnaireResponseStatus.COMPLETED);

                if (repHasCompleted && !hasResult) {
                    blocked.add("Representante '" + rep.getUser().getFirstName()
                            + "' possui resposta COMPLETED no questionário id=" + qId
                            + " sem resultado ISEP. Não pode ser removido.");
                }
            }
        }

        return blocked;
    }

    public List<String> validateDatesChange(Questionnaire questionnaire,
                                            LocalDate newEndDate,
                                            List<QuestionnaireResponse> responses) {
        List<String> blocked = new ArrayList<>();

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Não é possível alterar datas de questionário CONCLUÍDO '" + questionnaire.getName() + "'.");
            return blocked;
        }

        if (newEndDate != null && newEndDate.isBefore(LocalDate.now())) {
            blocked.add("Data final do questionário '" + questionnaire.getName() + "' não pode ser anterior a hoje.");
        }

        return blocked;
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 60 ? text.substring(0, 57) + "..." : text;
    }

    public record ValidationResult(List<String> blocked, List<String> warnings) {
        public boolean isBlocked() {
            return blocked != null && !blocked.isEmpty();
        }
    }
}

