package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProjectUpdateValidationPolicy {

    public ValidationResult validate(Project project,
                                     boolean hasProjectIsepResult,
                                     Map<Integer, Boolean> questionnaireHasResult,
                                     Map<Integer, List<QuestionnaireResponse>> responsesByQuestionnaire) {

        List<String> blocked = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (project.getStatus() != ProjectStatusEnum.ABERTO && project.getStatus() != ProjectStatusEnum.RASCUNHO) {
            blocked.add("Somente projetos com status ABERTO ou RASCUNHO podem ser atualizados. Status atual: " + project.getStatus());
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
        return validateQuestionRemoval(question, questionnaire, responses, Collections.emptySet());
    }

    public List<String> validateQuestionRemoval(Question question,
                                                Questionnaire questionnaire,
                                                List<QuestionnaireResponse> responses,
                                                Set<Representative> representatives) {
        List<String> blocked = new ArrayList<>();

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Não é possível remover pergunta id=" + question.getId() + " de questionário CONCLUÍDO.");
            return blocked;
        }

        Set<Long> questionRoleIds = question.getRoles() != null
                ? question.getRoles().stream().map(role -> role.getId()).collect(Collectors.toSet())
                : Set.of();

        if (responses != null) {
            for (QuestionnaireResponse resp : responses) {
                if (resp.getStatus() == QuestionnaireResponseStatus.COMPLETED) {
                    boolean answered = resp.getAnswers() != null && resp.getAnswers().stream()
                            .anyMatch(a -> Objects.equals(a.getQuestionId(), Long.valueOf(question.getId())) && a.getResponse() != null);
                    if (answered && representativeHasQuestionRole(resp.getRepresentativeId(), questionRoleIds, representatives)) {
                        blocked.add("Pergunta id=" + question.getId() + " já foi respondida por representante com papel compatível (resposta COMPLETED). Não pode ser removida.");
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

    public List<String> validateQuestionnaireUpdate(Questionnaire questionnaire,
                                                    boolean hasResult,
                                                    List<QuestionnaireResponse> responses,
                                                    boolean isStructuralChange) {
        List<String> blocked = new ArrayList<>();

        if (hasResult) {
            blocked.add("Questionário '" + questionnaire.getName() + "' já possui resultado ISEP. Não pode ser editado.");
            return blocked;
        }

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Questionário '" + questionnaire.getName() + "' já está CONCLUÍDO. Não pode ser editado.");
            return blocked;
        }

        if (isStructuralChange && responses != null) {
            boolean hasCompleted = responses.stream()
                    .anyMatch(r -> r.getStatus() == QuestionnaireResponseStatus.COMPLETED);
            if (hasCompleted) {
                blocked.add("Questionário '" + questionnaire.getName() + "' possui respostas COMPLETED. Mudanças estruturais (peso) não são permitidas.");
            }
        }

        return blocked;
    }

    public List<String> validateQuestionUpdate(Question question,
                                               Questionnaire questionnaire,
                                               List<QuestionnaireResponse> responses,
                                               boolean isTextChange,
                                               boolean isRoleChange) {
        return validateQuestionUpdate(question, questionnaire, responses, isTextChange, isRoleChange, Collections.emptySet());
    }

    public List<String> validateQuestionUpdate(Question question,
                                               Questionnaire questionnaire,
                                               List<QuestionnaireResponse> responses,
                                               boolean isTextChange,
                                               boolean isRoleChange,
                                               Set<Representative> representatives) {
        List<String> blocked = new ArrayList<>();

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            blocked.add("Não é possível editar pergunta id=" + question.getId() + " de questionário CONCLUÍDO '" + questionnaire.getName() + "'.");
            return blocked;
        }

        Set<Long> questionRoleIds = question.getRoles() != null
                ? question.getRoles().stream().map(role -> role.getId()).collect(Collectors.toSet())
                : Set.of();

        if (responses != null) {
            boolean questionAnsweredByMatchingRole = responses.stream()
                    .filter(r -> r.getStatus() == QuestionnaireResponseStatus.COMPLETED)
                    .filter(r -> representativeHasQuestionRole(r.getRepresentativeId(), questionRoleIds, representatives))
                    .anyMatch(r -> r.getAnswers() != null && r.getAnswers().stream()
                            .anyMatch(a -> Objects.equals(a.getQuestionId(), Long.valueOf(question.getId())) && a.getResponse() != null));

            if (questionAnsweredByMatchingRole && isTextChange) {
                blocked.add("Pergunta id=" + question.getId() + " já foi respondida. O texto não pode ser alterado.");
            }

            if (questionAnsweredByMatchingRole && isRoleChange) {
                blocked.add("Pergunta id=" + question.getId() + " já foi respondida. Os papéis vinculados não podem ser alterados.");
            }
        }

        return blocked;
    }

    public List<String> validateDatesChange(Questionnaire questionnaire,
                                            LocalDate newEndDate) {
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
    
    private boolean representativeHasQuestionRole(Long representativeId, Set<Long> questionRoleIds, Set<Representative> representatives) {
        if (representatives == null || representatives.isEmpty() || questionRoleIds.isEmpty()) {
            return true;
        }
        return representatives.stream()
                .filter(r -> Objects.equals(r.getId(), representativeId))
                .findFirst()
                .map(rep -> rep.getRoles() != null && rep.getRoles().stream()
                        .anyMatch(role -> questionRoleIds.contains(role.getId())))
                .orElse(false);
    }


    public record ValidationResult(List<String> blocked, List<String> warnings) {
        public boolean isBlocked() {
            return blocked != null && !blocked.isEmpty();
        }
    }
}

