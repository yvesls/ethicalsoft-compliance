package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.QuestionDomainEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** (EXCLUIR DEPOIS)
 * Inicializador de metadados de perguntas para cálculo de sub-índices de governança.
 * Mapeia perguntas existentes a domínios (ETHICS, PROCESS, QUALITY, ESG, FAIRNESS, SECURITY),
 * temas e criticidade. Utilizado pelo DomainScoreCalculator durante o cálculo de ISEP.
 *
 * Para cada pergunta cadastrada, insere um QuestionMetadataDocument se não existir.
 * Os mapeamentos aqui são exemplos baseados nas perguntas comuns de maturidade ética
 * do ISEP e devem ser ajustados conforme as perguntas reais do sistema.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class QuestionMetadataInitializer {

    private final QuestionMetadataRepository repository;

    @PostConstruct
    public void seedMetadata() {
        List<QuestionMetadataDocument> seeds = defaultMetadata();
        int inserted = 0;
        for (QuestionMetadataDocument doc : seeds) {
            if (!repository.existsByQuestionId(doc.getQuestionId())) {
                repository.save(doc);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("[question-metadata-init] {} metadados de perguntas inseridos.", inserted);
        }
    }

    /** (EXCLUIR DEPOIS)
     * Mapeamento padrão baseado nas perguntas típicas de maturidade ética.
     * IDs devem ser ajustados conforme o banco de dados real.
     *
     * Categorias:
     * - ETHICS: privacidade, transparência, responsabilidade, impacto social
     * - PROCESS: documentação, rastreabilidade, governança de decisões
     * - QUALITY: testes, critérios de aceite, padrões de código
     * - SECURITY: proteção de dados, vulnerabilidades, controle de acesso
     * - ESG: sustentabilidade, impacto ambiental
     * - FAIRNESS: equidade, viés, acessibilidade, inclusão
     */
    private List<QuestionMetadataDocument> defaultMetadata() {
        return List.of(
                meta(1L, QuestionDomainEnum.ETHICS, "RESPONSIBILITY", true),
                meta(2L, QuestionDomainEnum.ETHICS, "TRANSPARENCY", true),
                meta(3L, QuestionDomainEnum.ETHICS, "PRIVACY", true),
                meta(4L, QuestionDomainEnum.ETHICS, "SOCIAL_IMPACT", false),
                meta(5L, QuestionDomainEnum.ETHICS, "ACCOUNTABILITY", true),

                meta(6L, QuestionDomainEnum.PROCESS, "DOCUMENTATION", true),
                meta(7L, QuestionDomainEnum.PROCESS, "TRACEABILITY", true),
                meta(8L, QuestionDomainEnum.PROCESS, "GOVERNANCE", false),
                meta(9L, QuestionDomainEnum.PROCESS, "STAKEHOLDER_ENGAGEMENT", false),
                meta(10L, QuestionDomainEnum.PROCESS, "DECISION_GOVERNANCE", true),

                meta(11L, QuestionDomainEnum.QUALITY, "TESTING", true),
                meta(12L, QuestionDomainEnum.QUALITY, "ACCEPTANCE_CRITERIA", true),
                meta(13L, QuestionDomainEnum.QUALITY, "CODE_STANDARDS", false),
                meta(14L, QuestionDomainEnum.QUALITY, "REVIEW", false),

                meta(15L, QuestionDomainEnum.SECURITY, "DATA_PROTECTION", true),
                meta(16L, QuestionDomainEnum.SECURITY, "ACCESS_CONTROL", true),
                meta(17L, QuestionDomainEnum.SECURITY, "VULNERABILITY", false),

                meta(18L, QuestionDomainEnum.ESG, "SUSTAINABILITY", false),
                meta(19L, QuestionDomainEnum.ESG, "ENVIRONMENTAL_IMPACT", false),

                meta(20L, QuestionDomainEnum.FAIRNESS, "ACCESSIBILITY", true),
                meta(21L, QuestionDomainEnum.FAIRNESS, "BIAS", true),
                meta(22L, QuestionDomainEnum.FAIRNESS, "INCLUSION", false),

                meta(23L, QuestionDomainEnum.ETHICS, "INFORMED_CONSENT", true),
                meta(24L, QuestionDomainEnum.PROCESS, "PROJECT_CHARTER", false),
                meta(25L, QuestionDomainEnum.PROCESS, "STAKEHOLDER_IDENTIFICATION", false)
        );
    }

    private QuestionMetadataDocument meta(Long questionId, QuestionDomainEnum domain,
                                          String theme, boolean critical) {
        return QuestionMetadataDocument.builder()
                .questionId(questionId)
                .domain(domain)
                .theme(theme)
                .weight(BigDecimal.ONE)
                .critical(critical)
                .build();
    }
}

