package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswersRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswerResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswersResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireResponsePort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireResponseRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.RepresentativeRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.LinkMapper;
import com.ethicalsoft.ethicalsoft_complience.domain.service.QuestionnaireAnswerPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.QuestionnaireStatusCalculator;
import com.ethicalsoft.ethicalsoft_complience.domain.service.RepresentativeAccessPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireResponseAdapter implements QuestionnaireResponsePort {

    private final QuestionnaireRepositoryPort questionnaireRepository;
    private final QuestionnaireResponseRepositoryPort questionnaireResponseRepository;
    private final RepresentativeRepositoryPort representativeRepository;
    private final QuestionnaireAnswerPolicy answerPolicy;
    private final QuestionnaireStatusCalculator statusCalculator;
    private final LinkMapper linkMapper;
    private final RepresentativeAccessPolicy representativeAccessPolicy;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Override
    public QuestionnaireAnswersResponseDTO getAnswers(Long projectId, Integer questionnaireId) {
        try {
            log.info("[questionnaire-response] Buscando respostas projeto={} questionario={}", projectId, questionnaireId);
            Long effectiveRepresentativeId = representativeAccessPolicy.resolveRepresentativeIdForResponse(projectId);

            QuestionnaireResponse response = loadResponse(projectId, questionnaireId, effectiveRepresentativeId);
            List<QuestionnaireResponse.AnswerDocument> allAnswers = Optional.ofNullable(response.getAnswers()).orElseGet(List::of);

            Set<Long> roleIds = resolveRepresentativeRoleIds(effectiveRepresentativeId);
            List<QuestionnaireResponse.AnswerDocument> answers;
            if (!roleIds.isEmpty()) {
                answers = allAnswers.stream()
                        .filter(ans -> ans.getRoleIds() != null
                                && ans.getRoleIds().stream().anyMatch(roleIds::contains))
                        .toList();
            } else {
                answers = allAnswers;
            }

            boolean completed = QuestionnaireResponseStatus.COMPLETED.equals(response.getStatus());
            List<QuestionnaireAnswerResponseDTO> dtos = answers.stream()
                    .map(this::toAnswerResponse)
                    .toList();

            return QuestionnaireAnswersResponseDTO.builder()
                    .completed(completed)
                    .answers(dtos)
                    .build();
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao buscar respostas projeto={} questionario={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    @Override
    public QuestionnaireAnswersResponseDTO submitAnswers(Long projectId,
                                                          Integer questionnaireId,
                                                          QuestionnaireAnswersRequestDTO request) {
        try {
            boolean draft = request.isDraft();
            log.info("[questionnaire-response] Submetendo respostas projeto={} questionario={} draft={}", projectId, questionnaireId, draft);

            Questionnaire questionnaire = loadQuestionnaire(projectId, questionnaireId);
            Long effectiveRepresentativeId = resolveRepresentativeIdForSubmit(projectId, questionnaire, request);
            QuestionnaireResponse response = loadResponse(projectId, questionnaireId, effectiveRepresentativeId);

            Map<Long, QuestionnaireResponse.AnswerDocument> answerMap = response.getAnswers().stream()
                    .collect(Collectors.toMap(QuestionnaireResponse.AnswerDocument::getQuestionId, ans -> ans));

            request.getAnswers().forEach(dto -> answerPolicy.applyAnswer(dto, answerMap, draft));

            Set<Long> representativeRoleIds = resolveRepresentativeRoleIds(effectiveRepresentativeId);
            QuestionnaireResponseStatus status = statusCalculator.calculateStatus(response.getAnswers(), draft, representativeRoleIds);
            response.setStatus(status);
            response.setSubmissionDate(status == QuestionnaireResponseStatus.COMPLETED ? LocalDateTime.now() : null);
            questionnaireResponseRepository.save(response);

            if (QuestionnaireResponseStatus.COMPLETED.equals(status) && !draft) {
                triggerQuestionnaireSubmittedNotification(questionnaire, response, effectiveRepresentativeId);
            }

            log.info("[questionnaire-response] Respostas registradas projeto={} questionario={} representante={} status={} draft={}",
                    projectId, questionnaireId, effectiveRepresentativeId, status, draft);

            return getAnswers(projectId, questionnaireId);
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao registrar respostas projeto={} questionario={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    @Override
    public List<QuestionnaireResponseSummaryDTO> listSummaries(Long projectId, Integer questionnaireId) {
        try {
            log.info("[questionnaire-response] Listando resumos projeto={} questionario={}", projectId, questionnaireId);
            return questionnaireResponseRepository.findSummariesByProjectAndQuestionnaire(projectId, questionnaireId).stream()
                    .map(resp -> QuestionnaireResponseSummaryDTO.builder()
                            .representativeId(resp.getRepresentativeId())
                            .status(resp.getStatus())
                            .submissionDate(resp.getSubmissionDate())
                            .build())
                    .toList();
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao listar resumos projeto={} questionario={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    private QuestionnaireAnswerResponseDTO toAnswerResponse(QuestionnaireResponse.AnswerDocument answer) {
        return QuestionnaireAnswerResponseDTO.builder()
                .questionId(answer.getQuestionId())
                .response(answer.getResponse())
                .justification(linkMapper.toDto(answer.getJustification()))
                .evidence(linkMapper.toDto(answer.getEvidence()))
                .attachments(Optional.ofNullable(answer.getAttachments())
                        .map(list -> list.stream().map(linkMapper::toDto).toList())
                        .orElseGet(List::of))
                .build();
    }

    private Questionnaire loadQuestionnaire(Long projectId, Integer questionnaireId) {
        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new BusinessException("Questionário não encontrado"));
        if (!Objects.equals(questionnaire.getProject().getId(), projectId)) {
            throw new BusinessException("Questionário não pertence ao projeto informado");
        }
        return questionnaire;
    }

    private QuestionnaireResponse loadResponse(Long projectId,
                                                Integer questionnaireId,
                                                Long representativeId) {
        return questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, representativeId)
                .orElseThrow(() -> new BusinessException("Registro de respostas não encontrado"));
    }

    private void triggerQuestionnaireSubmittedNotification(Questionnaire questionnaire,
                                                           QuestionnaireResponse response,
                                                           Long representativeId) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("projectId", questionnaire.getProject() != null ? questionnaire.getProject().getId() : null);
        context.put("questionnaireId", questionnaire.getId());
        context.put("representativeId", representativeId);
        context.put("submittedAt", response.getSubmissionDate());
        sendNotificationUseCase.execute(new SendNotificationCommand(NotificationType.QUESTIONNAIRE_SUBMITTED, context));
    }

    private Long resolveRepresentativeIdForSubmit(Long projectId,
                                                  Questionnaire questionnaire,
                                                  QuestionnaireAnswersRequestDTO request) {
        Long resolved = representativeAccessPolicy.resolveRepresentativeIdForResponse(projectId);
        if (resolved != null) {
            return resolved;
        }
        Long requestedRepresentativeId = request.getRepresentativeId();
        if (requestedRepresentativeId == null) {
            return null;
        }
        representativeAccessPolicy.ensureRepresentativeBelongsToProject(requestedRepresentativeId, questionnaire.getProject());
        return requestedRepresentativeId;
    }

    private Set<Long> resolveRepresentativeRoleIds(Long representativeId) {
        if (representativeId == null) {
            return Collections.emptySet();
        }
        return representativeRepository.findById(representativeId)
                .map(rep -> Optional.ofNullable(rep.getRoles())
                        .orElse(Collections.emptySet())
                        .stream()
                        .map(Role::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }
}
