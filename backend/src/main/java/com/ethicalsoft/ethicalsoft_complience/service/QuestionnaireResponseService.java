package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.LinkDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireAnswerPageRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireAnswerRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireAnswerResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireResponseService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ProjectRepository projectRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final RepresentativeRepository representativeRepository;
    private final AuthService authService;

    public Page<QuestionnaireQuestionResponseDTO> listQuestions(Long projectId,
                                                                Integer questionnaireId,
                                                                Pageable pageable) {
        try {
            log.info("[questionnaire-response] Listando perguntas projeto={} questionario={} pagina={}", projectId, questionnaireId, pageable.getPageNumber());
            Questionnaire questionnaire = loadQuestionnaire(projectId, questionnaireId);
            Long effectiveRepresentativeId = resolveRepresentativeId(projectId);
            ensureRepresentativeBelongsToProject(effectiveRepresentativeId, questionnaire.getProject());

            Page<Question> pageResult = questionRepository.findByQuestionnaireIdOrderByIdAsc(questionnaireId, pageable);
            List<QuestionnaireQuestionResponseDTO> content = pageResult.stream()
                    .map(this::toQuestionResponse)
                    .toList();
            return new PageImpl<>(content, pageable, pageResult.getTotalElements());
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao listar perguntas projeto={} questionario={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    public QuestionnaireAnswerPageResponseDTO getAnswerPage(Long projectId,
                                                            Integer questionnaireId,
                                                            Pageable pageable) {
        try {
            log.info("[questionnaire-response] Buscando respostas paginadas projeto={} questionario={} pagina={}", projectId, questionnaireId, pageable.getPageNumber());
            Long effectiveRepresentativeId = resolveRepresentativeId(projectId);

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
            PageSlice slice = resolveSlice(pageable.getPageNumber(), pageable.getPageSize(), total);
            List<QuestionnaireAnswerResponseDTO> pageAnswers = answers.subList(slice.fromIndex, slice.toIndex).stream()
                    .map(this::toAnswerResponse)
                    .toList();

            boolean completed = QuestionnaireResponseStatus.COMPLETED.equals(response.getStatus());
            return QuestionnaireAnswerPageResponseDTO.builder()
                    .pageNumber(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalPages(slice.totalPages)
                    .completed(completed)
                    .answers(pageAnswers)
                    .build();
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao buscar respostas paginadas projeto={} questionario={} pagina={}", projectId, questionnaireId, pageable.getPageNumber(), ex);
            throw ex;
        }
    }

    public QuestionnaireAnswerPageResponseDTO submitAnswerPage(Long projectId,
                                                               Integer questionnaireId,
                                                               QuestionnaireAnswerPageRequestDTO request) {
        try {
            log.info("[questionnaire-response] Recebendo respostas projeto={} questionario={} pagina={}", projectId, questionnaireId, request.getPageNumber());
            Questionnaire questionnaire = loadQuestionnaire(projectId, questionnaireId);
            Long effectiveRepresentativeId = resolveRepresentativeId(projectId);
            QuestionnaireResponse response = loadResponse(projectId, questionnaireId, effectiveRepresentativeId);
            PageSlice slice = resolveSlice(request.getPageNumber(), request.getPageSize(), response.getAnswers().size());
            Map<Long, QuestionnaireResponse.AnswerDocument> answerMap = response.getAnswers().stream()
                    .collect(Collectors.toMap(QuestionnaireResponse.AnswerDocument::getQuestionId, ans -> ans));

            request.getAnswers().forEach(dto -> applyAnswer(dto, answerMap));

            QuestionnaireResponseStatus status = resolveStatus(response.getAnswers());
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

    public List<QuestionnaireResponseSummaryDTO> listSummaries(Long projectId, Integer questionnaireId) {
        try {
            log.info("[questionnaire-response] Listando resumos projeto={} questionario={}", projectId, questionnaireId);
            return questionnaireResponseRepository.findSummariesByProjectAndQuestionnaire(projectId, questionnaireId).stream()
                    .map(resp -> QuestionnaireResponseSummaryDTO.builder()
                            .representativeId(resp.getRepresentativeId())
                            .status(resp.getStatus())
                            .submissionDate(resp.getSubmissionDate())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("[questionnaire-response] Falha ao listar resumos projeto={} questionario={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    private QuestionnaireQuestionResponseDTO toQuestionResponse(Question question) {
        List<Integer> stageIds = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream().map(Stage::getId).toList())
                .orElseGet(List::of);
        List<String> stageNames = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream()
                        .sorted(Comparator.comparing(Stage::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(Stage::getName)
                        .toList())
                .orElseGet(List::of);
        List<Long> roleIds = Optional.ofNullable(question.getRoles())
                .map(roles -> roles.stream().map(Role::getId).toList())
                .orElseGet(List::of);

        return QuestionnaireQuestionResponseDTO.builder()
                .id(question.getId().longValue())
                .text(question.getValue())
                .stageIds(stageIds)
                .stageNames(stageNames)
                .roleIds(roleIds)
                .order(question.getId())
                .build();
    }

    private QuestionnaireAnswerResponseDTO toAnswerResponse(QuestionnaireResponse.AnswerDocument answer) {
        return QuestionnaireAnswerResponseDTO.builder()
                .questionId(answer.getQuestionId())
                .response(answer.getResponse())
                .justification(toLinkDTO(answer.getJustification()))
                .evidence(toLinkDTO(answer.getEvidence()))
                .attachments(Optional.ofNullable(answer.getAttachments())
                        .map(list -> list.stream().map(this::toLinkDTO).toList())
                        .orElseGet(List::of))
                .build();
    }

    private void applyAnswer(QuestionnaireAnswerRequestDTO dto,
                              Map<Long, QuestionnaireResponse.AnswerDocument> answerMap) {
        QuestionnaireResponse.AnswerDocument answer = answerMap.get(dto.getQuestionId());

        if (answer == null) {
            throw new BusinessException("Questão inválida para este questionário");
        }

        if (Boolean.FALSE.equals(dto.getResponse()) && (ObjectUtil.isNullOrEmpty(dto.getJustification() ))) {
            throw new BusinessException("Justificativa é obrigatória quando a resposta é 'Não'.");
        }

        answer.setResponse(dto.getResponse());
        answer.setJustification(toLinkDocument(dto.getJustification()));
        answer.setEvidence(toLinkDocument(dto.getEvidence()));
        answer.setAttachments(Optional.ofNullable(dto.getAttachments())
                .map(list -> list.stream()
                        .flatMap(link -> Optional.ofNullable(toLinkDocument(link)).stream())
                        .toList())
                .orElseGet(List::of));
    }

    private QuestionnaireResponseStatus resolveStatus(List<QuestionnaireResponse.AnswerDocument> answers) {
        boolean hasAny = answers.stream().anyMatch(ans -> ans.getResponse() != null);
        boolean allAnswered = hasAny && answers.stream().allMatch(ans -> ans.getResponse() != null);

        if (!hasAny) {
            return QuestionnaireResponseStatus.PENDING;
        }
        return allAnswered ? QuestionnaireResponseStatus.COMPLETED : QuestionnaireResponseStatus.IN_PROGRESS;
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
            throw new BusinessException("Representante não identificado para o questionário");
        }
        return questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, representativeId)
                .orElseThrow(() -> new BusinessException("Registro de respostas não encontrado"));
    }

    private Long resolveRepresentativeId(Long projectId) {
        User authenticated = authService.getAuthenticatedUser();
        if (UserRoleEnum.ADMIN.equals(authenticated.getRole()) || projectRepository.existsByIdAndOwnerId(projectId, authenticated.getId())) {
            return null;
        }
        Representative representative = representativeRepository.findByUserIdAndProjectId(authenticated.getId(), projectId)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não possui representante vinculado ao projeto"));
        return representative.getId();
    }

    private void ensureRepresentativeBelongsToProject(Long representativeId, Project project) {
        Representative representative = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new BusinessException("Representante não encontrado"));
        if (!Objects.equals(representative.getProject().getId(), project.getId())) {
            throw new BusinessException("Representante não pertence ao projeto informado");
        }
    }

    private LinkDTO toLinkDTO(QuestionnaireResponse.LinkDocument doc) {
        if (doc == null) {
            return null;
        }
        LinkDTO dto = new LinkDTO();
        dto.setDescricao(doc.getDescricao());
        dto.setUrl(doc.getUrl());
        return dto;
    }

    private QuestionnaireResponse.LinkDocument toLinkDocument(LinkDTO dto) {
        if (dto == null) {
            return null;
        }
        QuestionnaireResponse.LinkDocument doc = new QuestionnaireResponse.LinkDocument();
        doc.setDescricao(dto.getDescricao());
        doc.setUrl(dto.getUrl());
        return doc;
    }

    private PageSlice resolveSlice(int page, int size, int totalElements) {
        if (size <= 0) {
            throw new BusinessException("Tamanho de página inválido");
        }
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (page < 0 || (totalPages > 0 && page >= totalPages)) {
            throw new BusinessException("Página solicitada está fora do intervalo disponível");
        }
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        return new PageSlice(fromIndex, toIndex, totalPages == 0 ? 1 : totalPages);
    }

    private record PageSlice(int fromIndex, int toIndex, int totalPages) {}
}
