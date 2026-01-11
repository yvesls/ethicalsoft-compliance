package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RespondentStatusDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QuestionnaireSummaryBuilder {

    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    public QuestionnaireSummaryResponseDTO build(Questionnaire questionnaire, Map<Long, Representative> representativesById) {
        List<QuestionnaireResponse> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireId(questionnaire.getProject().getId(), questionnaire.getId());

        Map<Long, QuestionnaireResponse> responseByRep = responses.stream()
                .filter(resp -> resp.getRepresentativeId() != null)
                .collect(Collectors.toMap(QuestionnaireResponse::getRepresentativeId, r -> r, (a, b) -> a));

        int totalRespondents = representativesById.size();
        AtomicInteger responded = new AtomicInteger();
        AtomicReference<LocalDateTime> lastResponseAt = new AtomicReference<>();

        List<RespondentStatusDTO> respondentStatus = representativesById.values().stream()
                .map(rep -> {
                    QuestionnaireResponse response = responseByRep.get(rep.getId());
                    QuestionnaireResponseStatus status = response != null ? response.getStatus() : QuestionnaireResponseStatus.PENDING;
                    LocalDateTime completedAt = response != null ? response.getSubmissionDate() : null;

                    if (status == QuestionnaireResponseStatus.COMPLETED) {
                        responded.incrementAndGet();
                        if (completedAt != null && (lastResponseAt.get() == null || completedAt.isAfter(lastResponseAt.get()))) {
                            lastResponseAt.set(completedAt);
                        }
                    }

                    String name = rep.getUser() == null ? null :
                            String.join(" ",
                                    Optional.ofNullable(rep.getUser().getFirstName()).orElse(""),
                                    Optional.ofNullable(rep.getUser().getLastName()).orElse("")
                            ).trim();

                    return RespondentStatusDTO.builder()
                            .representativeId(rep.getId())
                            .name(name)
                            .email(rep.getUser() != null ? rep.getUser().getEmail() : null)
                            .status(status)
                            .completedAt(completedAt)
                            .build();
                })
                .toList();

        int pending = Math.max(totalRespondents - responded.get(), 0);

        QuestionnaireResponseStatus progressStatus;
        if (responded.get() == 0) {
            progressStatus = QuestionnaireResponseStatus.PENDING;
        } else if (responded.get() < totalRespondents) {
            progressStatus = QuestionnaireResponseStatus.IN_PROGRESS;
        } else {
            progressStatus = QuestionnaireResponseStatus.COMPLETED;
        }

        return QuestionnaireSummaryResponseDTO.builder()
                .id(questionnaire.getId())
                .name(questionnaire.getName())
                .status(questionnaire.getStatus())
                .applicationStartDate(questionnaire.getApplicationStartDate())
                .applicationEndDate(questionnaire.getApplicationEndDate())
                .stageName(questionnaire.getStage() != null ? questionnaire.getStage().getName() : null)
                .iterationName(questionnaire.getIterationRef() != null ? questionnaire.getIterationRef().getName() : null)
                .totalRespondents(totalRespondents)
                .respondedRespondents(responded.get())
                .pendingRespondents(pending)
                .lastResponseAt(lastResponseAt.get())
                .progressStatus(progressStatus)
                .respondents(respondentStatus)
                .build();
    }
}
