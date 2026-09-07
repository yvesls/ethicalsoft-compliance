package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.ConsolidatedAnswerDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.ConsolidatedAnswersFilter;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
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
    private final QuestionnaireRepositoryPort questionnaireRepository;
    private final RepresentativeRepositoryPort representativeRepository;
    private final LinkMapper linkMapper;

    public Page<ConsolidatedAnswerDTO> executeForQuestionnaire(Long projectId,
                                                                Integer questionnaireId,
                                                                ConsolidatedAnswersFilter filter,
                                                                Pageable pageable) {
        log.info("[consolidated-answers] questionário={} projeto={} filtros: rep={} q={} roleId={} role={} resp={} text={}",
                questionnaireId, projectId, filter.representativeId(), filter.questionId(), filter.roleId(),
                filter.roleName(), filter.response(), filter.questionText());

        List<QuestionnaireResponse> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId);

        return buildPage(projectId, responses, filter, pageable);
    }

    public Page<ConsolidatedAnswerDTO> executeForProject(Long projectId,
                                                          Integer questionnaireIdFilter,
                                                          ConsolidatedAnswersFilter filter,
                                                          Pageable pageable) {
        log.info("[consolidated-answers] projeto={} filtros: qnr={} rep={} q={} roleId={} role={} resp={} text={}",
                projectId, questionnaireIdFilter, filter.representativeId(), filter.questionId(), filter.roleId(),
                filter.roleName(), filter.response(), filter.questionText());

        List<QuestionnaireResponse> responses;
        if (questionnaireIdFilter != null) {
            responses = questionnaireResponseRepository.findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireIdFilter);
        } else {
            responses = questionnaireResponseRepository.findByProjectIdExcludingTemplates(projectId);
        }

        return buildPage(projectId, responses, filter, pageable);
    }

    private Page<ConsolidatedAnswerDTO> buildPage(Long projectId,
                                                   List<QuestionnaireResponse> responses,
                                                   ConsolidatedAnswersFilter filter,
                                                   Pageable pageable) {

        Long representativeIdFilter = filter.representativeId();
        Long questionIdFilter = filter.questionId();
        Long roleIdFilter = filter.roleId();
        String roleNameFilter = filter.roleName();
        Boolean responseFilter = filter.response();
        String questionTextFilter = filter.questionText();

        Map<Long, Representative> representativeMap = representativeRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(rep -> rep.getId(), Function.identity()));

        Map<Integer, String> questionnaireNameMap = questionnaireRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(q -> q.getId(), q ->
                        q.getIteration() != null && !q.getIteration().isBlank()
                                ? q.getIteration()
                                : q.getName()));

        Set<Long> representativesWithRole = null;

        if (roleIdFilter != null) {
            final Long finalRoleIdFilter = roleIdFilter;
            representativesWithRole = representativeMap.values().stream()
                    .filter(rep -> rep.getRoles() != null &&
                            rep.getRoles().stream()
                                    .anyMatch(role -> finalRoleIdFilter.equals(role.getId())))
                    .map(rep -> rep.getId())
                    .collect(Collectors.toSet());
            log.debug("[consolidated-answers] Representantes com roleId={}: {}", roleIdFilter, representativesWithRole);
        } else if (roleNameFilter != null && !roleNameFilter.isBlank()) {
            String normalizedFilter = roleNameFilter.trim().toLowerCase();
            representativesWithRole = representativeMap.values().stream()
                    .filter(rep -> rep.getRoles() != null &&
                            rep.getRoles().stream()
                                    .anyMatch(role -> role.getName().toLowerCase().contains(normalizedFilter)))
                    .map(rep -> rep.getId())
                    .collect(Collectors.toSet());
            log.debug("[consolidated-answers] Representantes com role '{}': {}", roleNameFilter, representativesWithRole);
        }
        final Set<Long> finalRepresentativesWithRole = representativesWithRole;

        List<ConsolidatedAnswerDTO> allRows = responses.stream()
                .filter(response -> response.getRepresentativeId() != null)
                .filter(response -> representativeIdFilter == null
                        || representativeIdFilter.equals(response.getRepresentativeId()))
                .filter(response -> finalRepresentativesWithRole == null
                        || finalRepresentativesWithRole.contains(response.getRepresentativeId()))
                .flatMap(response -> {
                    Representative rep = representativeMap.get(response.getRepresentativeId());
                    String repName = rep != null && rep.getUser() != null
                            ? rep.getUser().getFirstName() + " " + rep.getUser().getLastName()
                            : "Representante #" + response.getRepresentativeId();
                    List<String> roles = rep != null && rep.getRoles() != null
                            ? rep.getRoles().stream().map(role -> role.getName()).sorted().toList()
                            : Collections.emptyList();
                    String questionnaireName = questionnaireNameMap.getOrDefault(
                            response.getQuestionnaireId(), "#" + response.getQuestionnaireId());

                    List<QuestionnaireResponse.AnswerDocument> answers =
                            Optional.ofNullable(response.getAnswers()).orElse(Collections.emptyList());

                    return answers.stream()
                            .filter(ans -> ans.getResponse() != null)
                            .filter(ans -> questionIdFilter == null || questionIdFilter.equals(ans.getQuestionId()))
                            .filter(ans -> responseFilter == null || responseFilter.equals(ans.getResponse()))
                            .filter(ans -> {
                                if (questionTextFilter == null || questionTextFilter.isBlank()) {
                                    return true;
                                }
                                String text = ans.getQuestionText() != null ? ans.getQuestionText() : "";
                                return text.toLowerCase().contains(questionTextFilter.trim().toLowerCase());
                            })
                            .map(ans -> new ConsolidatedAnswerDTO(
                                    response.getRepresentativeId(),
                                    repName,
                                    roles,
                                    response.getQuestionnaireId(),
                                    questionnaireName,
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
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRows.size());

        List<ConsolidatedAnswerDTO> pageContent = start >= allRows.size()
                ? Collections.emptyList()
                : allRows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allRows.size());
    }
}
