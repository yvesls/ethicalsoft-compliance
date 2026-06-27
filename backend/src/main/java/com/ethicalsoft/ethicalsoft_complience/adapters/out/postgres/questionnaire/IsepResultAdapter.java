package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.MemberComplianceResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.MemberStageComplianceResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireResultRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.DomainScores;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IseqCalculationResult;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IsepResultAdapter implements IsepResultCommandPort, IsepResultQueryPort {

    private final QuestionnaireResultRepository repository;

    @Override
    @Transactional
    public QuestionnaireResult save(IseqCalculationResult result) {
        log.info("[iseq-result-adapter] Persistindo resultado ISEQ questionário={}", result.questionnaireId());

        repository.findByQuestionnaireId(result.questionnaireId())
                .ifPresent(existing -> {
                    repository.delete(existing);
                    repository.flush();
                });

        QuestionnaireResult entity = buildEntity(result);
        QuestionnaireResult saved = repository.save(entity);

        log.info("[iseq-result-adapter] Resultado ISEQ persistido id={} ISEQ={}", saved.getId(),
                IsepMath.toPercent(saved.getIseq()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionnaireResult> findByQuestionnaireId(Integer questionnaireId) {
        return repository.findByQuestionnaireId(questionnaireId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireResult> findByProjectId(Long projectId) {
        return repository.findByProjectIdOrderedByDate(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByQuestionnaireId(Integer questionnaireId) {
        return repository.existsByQuestionnaireId(questionnaireId);
    }

    private QuestionnaireResult buildEntity(IseqCalculationResult result) {
        QuestionnaireResult entity = new QuestionnaireResult();
        entity.setProjectId(result.projectId());
        entity.setQuestionnaireId(result.questionnaireId());
        entity.setIseq(result.iseq());
        entity.setBand(result.iseqBand().name());
        entity.setTeamSimpleAverage(result.teamSimpleAverage());
        entity.setTeamStandardDeviation(result.teamStandardDeviation());
        entity.setCalculatedAt(LocalDateTime.now());

        DomainScores ds = result.domainScores();
        if (ds != null) {
            entity.setEthicsScore(ds.ethicsScore());
            entity.setProcessScore(ds.processScore());
            entity.setFairnessScore(ds.fairnessScore());
            entity.setEsgScore(ds.esgScore());
            entity.setEthicsDebtScore(ds.ethicsDebtScore());
            entity.setTechDebtScore(ds.techDebtScore());
        }

        List<MemberComplianceResult> memberResults = buildMemberResults(entity, result.memberComplianceIndex());
        entity.setMemberResults(memberResults);

        List<MemberStageComplianceResult> stageResults = buildStageResults(entity, result.memberStageComplianceIndex());
        entity.setStageResults(stageResults);

        return entity;
    }

    private List<MemberComplianceResult> buildMemberResults(QuestionnaireResult parent,
                                                             Map<Long, BigDecimal> memberIcp) {
        List<MemberComplianceResult> list = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : memberIcp.entrySet()) {
            MemberComplianceResult mcr = new MemberComplianceResult();
            mcr.setResult(parent);
            mcr.setRepresentativeId(entry.getKey());
            mcr.setIcp(entry.getValue());
            mcr.setBand(EthicalComplianceBand.classify(IsepMath.toPercent(entry.getValue())).name());
            list.add(mcr);
        }
        return list;
    }

    private List<MemberStageComplianceResult> buildStageResults(QuestionnaireResult parent,
                                                                  Map<Long, Map<Integer, BigDecimal>> memberStageIem) {
        List<MemberStageComplianceResult> list = new ArrayList<>();
        for (Map.Entry<Long, Map<Integer, BigDecimal>> memberEntry : memberStageIem.entrySet()) {
            Long repId = memberEntry.getKey();
            for (Map.Entry<Integer, BigDecimal> stageEntry : memberEntry.getValue().entrySet()) {
                MemberStageComplianceResult mscr = new MemberStageComplianceResult();
                mscr.setResult(parent);
                mscr.setRepresentativeId(repId);
                mscr.setStageId(stageEntry.getKey());
                mscr.setIem(stageEntry.getValue());
                list.add(mscr);
            }
        }
        return list;
    }
}

