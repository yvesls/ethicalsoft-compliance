package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswerPageRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswerResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireResponsePort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireResponseRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.*;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireResponseAdapter implements QuestionnaireResponsePort {

    private final QuestionnaireRepositoryPort questionnaireRepository;
    private final QuestionnaireResponseRepositoryPort questionnaireResponseRepository;
    private final QuestionnaireAnswerPolicy answerPolicy;
    private final QuestionnaireStatusCalculator statusCalculator;
    private final PageSliceResolver pageSliceResolver;
    private final LinkMapper linkMapper;
    private final RepresentativeAccessPolicy representativeAccessPolicy;

    @Override
    public QuestionnaireAnswerPageResponseDTO getAnswerPage(Long projectId,
                                                            Integer questionnaireId,
                                                            Pageable pageable) {
        try {
            log.info("[questionnaire-response] Buscando respostas paginadas projeto={} questionario={} pagina={}", projectId, questionnaireId, pageable.getPageNumber());
            Long effectiveRepresentativeId = representativeAccessPolicy.resolveRepresentativeId(projectId);

            QuestionnaireResponse response = loadResponse(projectId, questionnaireId, effectiveRepresentativeId);
            List<QuestionnaireResponse.AnswerDocument> answers = Optional.ofNullable(response.getAnswers()).orElseGet(List::of);
            if (CollectionUtils.isEmpty(answers)) {
                return QuestionnaireAnswerPageResponseDTO.builder()
                        .pageNumber(pageable.getPageNumber())
                        .pageSize(pageable.getPageSize())
                        .totalPages(0)
                        .completed(false)
                        .answers(List.of())
                        .build();
            }

            int total = answers.size();
            PageSliceResolver.PageSlice slice = pageSliceResolver.resolve(pageable.getPageNumber(), pageable.getPageSize(), total);
            List<QuestionnaireAnswerResponseDTO> pageAnswers = answers.subList(slice.fromIndex(), slice.toIndex()).stream()
                    .map(this::toAnswerResponse)
                    .toList();

            boolean completed = QuestionnaireResponseStatus.COMPLETED.equals(response.getStatus());
            return QuestionnaireAnswerPageResponseDTO.builder()
                    .pageNumber(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalPages(slice.totalPages())
                    .completed(completed)
                    .answers(pageAnswers)
                    .build();
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao buscar respostas paginadas projeto={} questionario={} pagina={}", projectId, questionnaireId, pageable.getPageNumber(), ex);
            throw ex;
        }
    }

    @Override
    public QuestionnaireAnswerPageResponseDTO submitAnswerPage(Long projectId,
                                                               Integer questionnaireId,
                                                               QuestionnaireAnswerPageRequestDTO request) {
        try {
            log.info("[questionnaire-response] Recebendo respostas projeto={} questionario={} pagina={}", projectId, questionnaireId, request.getPageNumber());
            Questionnaire questionnaire = loadQuestionnaire(projectId, questionnaireId);
            Long effectiveRepresentativeId = representativeAccessPolicy.resolveRepresentativeId(projectId);
            QuestionnaireResponse response = loadResponse(projectId, questionnaireId, effectiveRepresentativeId);
            pageSliceResolver.resolve(request.getPageNumber(), request.getPageSize(), response.getAnswers().size());
            Map<Long, QuestionnaireResponse.AnswerDocument> answerMap = response.getAnswers().stream()
                    .collect(Collectors.toMap(QuestionnaireResponse.AnswerDocument::getQuestionId, ans -> ans));

            request.getAnswers().forEach(dto -> answerPolicy.applyAnswer(dto, answerMap));

            QuestionnaireResponseStatus status = statusCalculator.calculateStatus(response.getAnswers());
            response.setStatus(status);
            response.setSubmissionDate(status == QuestionnaireResponseStatus.COMPLETED ? LocalDateTime.now() : null);
            questionnaireResponseRepository.save(response);

            log.info("[questionnaire-response] Respostas registradas projeto={} questionario={} representante={} status={}", projectId, questionnaireId, effectiveRepresentativeId, status);
            return getAnswerPage(projectId, questionnaireId, PageRequest.of(request.getPageNumber(), request.getPageSize()));
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
        if (representativeId == null) {
            return questionnaireResponseRepository
                    .findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, null)
                    .orElseThrow(() -> new BusinessException("Registro de respostas não encontrado"));
        }

        return questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, representativeId)
                .orElseThrow(() -> new BusinessException("Registro de respostas não encontrado"));
    }
}
