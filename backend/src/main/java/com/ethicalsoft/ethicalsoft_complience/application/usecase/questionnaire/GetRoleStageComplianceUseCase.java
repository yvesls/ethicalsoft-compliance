package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireResultRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.RoleStageComplianceDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetRoleStageComplianceUseCase {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResultRepository questionnaireResultRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final RepresentativeRepository representativeRepository;
    private final StageRepository stageRepository;

    @Transactional(readOnly = true)
    public List<RoleStageComplianceDTO> execute(Long projectId, Integer questionnaireId) {
        log.info("[role-stage-compliance] Calculando heatmap/gráfico4 questionário={} projeto={}",
                questionnaireId, projectId);

        questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionário não encontrado: " + questionnaireId));

        questionnaireResultRepository.findByQuestionnaireId(questionnaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado ISEP ainda não disponível para o questionário: " + questionnaireId));

        List<QuestionnaireResponse> completedResponses = responseRepository
                .findByProjectIdAndQuestionnaireId(projectId, questionnaireId)
                .stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .toList();

        if (completedResponses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Representative> representatives = representativeRepository.findByProjectId(projectId);
        Map<Long, Set<Role>> rolesByRepId = representatives.stream()
                .collect(Collectors.toMap(
                        Representative::getId,
                        rep -> Optional.ofNullable(rep.getRoles()).orElse(Set.of())
                ));

        Map<Integer, Stage> stageMap = stageRepository.findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(Stage::getId, s -> s));

        Map<Long, String> roleNames = new LinkedHashMap<>();
        Map<Long, Map<Integer, List<BigDecimal>>> roleStageIemValues = new LinkedHashMap<>();

        for (QuestionnaireResponse response : completedResponses) {
            Long repId = response.getRepresentativeId();
            Set<Role> repRoles = rolesByRepId.getOrDefault(repId, Set.of());
            if (repRoles.isEmpty() || response.getAnswers() == null) continue;

            Map<Integer, List<QuestionnaireResponse.AnswerDocument>> byStage = groupAnswersByStage(response.getAnswers());

            for (Map.Entry<Integer, List<QuestionnaireResponse.AnswerDocument>> stageEntry : byStage.entrySet()) {
                Integer stageId = stageEntry.getKey();
                List<QuestionnaireResponse.AnswerDocument> stageAnswers = stageEntry.getValue();

                long simCount = stageAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getResponse())).count();
                BigDecimal iem = IsepMath.ratio(simCount, stageAnswers.size());

                for (Role role : repRoles) {
                    roleNames.put(role.getId(), role.getName());
                    roleStageIemValues
                            .computeIfAbsent(role.getId(), k -> new LinkedHashMap<>())
                            .computeIfAbsent(stageId, k -> new ArrayList<>())
                            .add(iem);
                }
            }
        }

        return roleStageIemValues.entrySet().stream()
                .map(roleEntry -> {
                    Long roleId = roleEntry.getKey();
                    String roleName = roleNames.getOrDefault(roleId, "Papel " + roleId);
                    Map<Integer, RoleStageComplianceDTO.StageIemSummary> iemByStage = new LinkedHashMap<>();

                    for (Map.Entry<Integer, List<BigDecimal>> stageEntry : roleEntry.getValue().entrySet()) {
                        Integer stageId = stageEntry.getKey();
                        List<BigDecimal> iemValues = stageEntry.getValue();
                        BigDecimal avgIem = IsepMath.simpleAverage(iemValues);
                        BigDecimal avgIemPercent = IsepMath.toPercent(avgIem);
                        String band = EthicalComplianceBand.classify(avgIemPercent).name();
                        String stageName = stageMap.containsKey(stageId)
                                ? stageMap.get(stageId).getName()
                                : "Etapa " + stageId;

                        iemByStage.put(stageId, new RoleStageComplianceDTO.StageIemSummary(
                                stageId, stageName, avgIemPercent, band, iemValues.size()
                        ));
                    }

                    return new RoleStageComplianceDTO(roleId, roleName, iemByStage);
                })
                .collect(Collectors.toList());
    }

    private Map<Integer, List<QuestionnaireResponse.AnswerDocument>> groupAnswersByStage(
            List<QuestionnaireResponse.AnswerDocument> answers) {
        Map<Integer, List<QuestionnaireResponse.AnswerDocument>> result = new LinkedHashMap<>();
        for (QuestionnaireResponse.AnswerDocument answer : answers) {
            List<Integer> stageIds = answer.getStageIds();
            if (stageIds == null || stageIds.isEmpty()) {
                result.computeIfAbsent(-1, k -> new ArrayList<>()).add(answer);
            } else {
                result.computeIfAbsent(stageIds.get(0), k -> new ArrayList<>()).add(answer);
            }
        }
        return result;
    }
}

