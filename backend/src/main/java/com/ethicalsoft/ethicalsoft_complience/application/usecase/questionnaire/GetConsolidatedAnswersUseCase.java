package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.ConsolidatedAnswerDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireResponseRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.RepresentativeRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.LinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetConsolidatedAnswersUseCase {

    private final QuestionnaireResponseRepositoryPort questionnaireResponseRepository;
    private final RepresentativeRepositoryPort representativeRepository;
    private final LinkMapper linkMapper;

    public Page<ConsolidatedAnswerDTO> executeForQuestionnaire(Long projectId,
                                                                Integer questionnaireId,
                                                                Long representativeId,
                                                                Long questionId,
                                                                Long roleId,
                                                                Boolean responseFilter,
                                                                String questionText,
                                                                Pageable pageable) {
        log.info("[consolidated-answers] questionário={} projeto={} filtros: rep={} q={} role={} resp={} text={}",
                questionnaireId, projectId, representativeId, questionId, roleId, responseFilter, questionText);

        List<QuestionnaireResponse> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireId(projectId, questionnaireId);

        return buildPage(projectId, responses, representativeId, questionId, roleId, responseFilter, questionText, pageable);
    }

    public Page<ConsolidatedAnswerDTO> executeForProject(Long projectId,
                                                          Integer questionnaireIdFilter,
                                                          Long representativeId,
                                                          Long questionId,
                                                          Long roleId,
                                                          Boolean responseFilter,
                                                          String questionText,
                                                          Pageable pageable) {
        log.info("[consolidated-answers] projeto={} filtros: qnr={} rep={} q={} role={} resp={} text={}",
                projectId, questionnaireIdFilter, representativeId, questionId, roleId, responseFilter, questionText);

        List<QuestionnaireResponse> responses;
        if (questionnaireIdFilter != null) {
            responses = questionnaireResponseRepository.findByProjectIdAndQuestionnaireId(projectId, questionnaireIdFilter);
        } else {
            responses = questionnaireResponseRepository.findByProjectId(projectId);
        }

        return buildPage(projectId, responses, representativeId, questionId, roleId, responseFilter, questionText, pageable);
    }

    private Page<ConsolidatedAnswerDTO> buildPage(Long projectId,
                                                   List<QuestionnaireResponse> responses,
                                                   Long representativeIdFilter,
                                                   Long questionIdFilter,
                                                   Long roleIdFilter,
                                                   Boolean responseFilter,
                                                   String questionTextFilter,
                                                   Pageable pageable) {

        Map<Long, Representative> representativeMap = representativeRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(Representative::getId, Function.identity()));

        List<ConsolidatedAnswerDTO> allRows = new ArrayList<>();

        for (QuestionnaireResponse response : responses) {
            if (representativeIdFilter != null && !representativeIdFilter.equals(response.getRepresentativeId())) {
                continue;
            }

            Representative rep = representativeMap.get(response.getRepresentativeId());
            String repName = rep != null && rep.getUser() != null
                    ? rep.getUser().getFirstName() + " " + rep.getUser().getLastName()
                    : "Representante #" + response.getRepresentativeId();
            List<String> roles = rep != null && rep.getRoles() != null
                    ? rep.getRoles().stream().map(Role::getName).sorted().toList()
                    : Collections.emptyList();

            List<QuestionnaireResponse.AnswerDocument> answers = Optional.ofNullable(response.getAnswers())
                    .orElse(Collections.emptyList());

            for (QuestionnaireResponse.AnswerDocument ans : answers) {
                if (questionIdFilter != null && !questionIdFilter.equals(ans.getQuestionId())) {
                    continue;
                }

                if (roleIdFilter != null) {
                    boolean matchesRole = ans.getRoleIds() != null && ans.getRoleIds().contains(roleIdFilter);
                    if (!matchesRole) continue;
                }

                if (responseFilter != null && !responseFilter.equals(ans.getResponse())) {
                    continue;
                }

                if (questionTextFilter != null && !questionTextFilter.isBlank()) {
                    String text = ans.getQuestionText() != null ? ans.getQuestionText() : "";
                    if (!text.toLowerCase().contains(questionTextFilter.toLowerCase())) {
                        continue;
                    }
                }

                allRows.add(new ConsolidatedAnswerDTO(
                        response.getRepresentativeId(),
                        repName,
                        roles,
                        response.getQuestionnaireId(),
                        response.getStatus(),
                        response.getSubmissionDate(),
                        ans.getQuestionId(),
                        ans.getQuestionText(),
                        ans.getStageIds(),
                        ans.getResponse(),
                        linkMapper.toDto(ans.getJustification()),
                        linkMapper.toDto(ans.getEvidence()),
                        Optional.ofNullable(ans.getAttachments())
                                .map(list -> list.stream().map(linkMapper::toDto).toList())
                                .orElse(Collections.emptyList())
                ));
            }
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRows.size());

        List<ConsolidatedAnswerDTO> pageContent = start >= allRows.size()
                ? Collections.emptyList()
                : allRows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allRows.size());
    }
}

