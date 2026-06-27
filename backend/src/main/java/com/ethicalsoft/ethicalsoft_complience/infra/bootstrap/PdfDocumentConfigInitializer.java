package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.PdfDocumentConfigDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.PdfDocumentConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class PdfDocumentConfigInitializer {

    public static final String BOLETIM_KEY = "BOLETIM_NAO_CONFORMIDADE";
    public static final String CERTIFICADO_KEY = "CERTIFICADO_CONFORMIDADE";

    private final PdfDocumentConfigRepository repository;

    @PostConstruct
    public void seedConfigs() {
        try {
            configsToSeed().forEach(this::reconcileConfig);
        } catch (RuntimeException e) {
            log.warn("[pdf-config-init] Não foi possível inicializar as configurações de documentos PDF no MongoDB. " +
                    "A aplicação continuará normalmente. Erro: {}", e.getMessage());
        }
    }

    private List<PdfDocumentConfigDocument> configsToSeed() {
        return List.of(
                PdfDocumentConfigDocument.builder()
                        .key(BOLETIM_KEY)
                        .templateLink("documents/boletim-nao-conformidade.ftl")
                        .documentTitle("Boletim de Não Conformidade Ética")
                        .systemName("EthicalSoft Compliance")
                        .footerNote("Documento gerado automaticamente. Integra a trilha de auditoria do projeto " +
                                "e deve ser arquivado para consulta futura.")
                        .impactSummary("Os desvios identificados indicam pontos do processo em que as práticas " +
                                "éticas não atingiram o patamar mínimo esperado. A não correção compromete a " +
                                "qualidade ética do projeto e tende a acumular dívida ética e técnica, ampliando " +
                                "riscos de longo prazo e a assimetria de informação entre as partes interessadas.")
                        .defaultCorrectiveActions(List.of(
                                "Revisar com a equipe as perguntas listadas como não conformes e as respectivas justificativas.",
                                "Submeter novamente as respostas corrigidas dentro do prazo estabelecido.",
                                "Solicitar o recálculo dos resultados para verificar se a classificação mínima foi atingida.",
                                "Registrar as evidências de correção para fins de auditoria."))
                        .build(),
                PdfDocumentConfigDocument.builder()
                        .key(CERTIFICADO_KEY)
                        .templateLink("documents/certificado-conformidade.ftl")
                        .documentTitle("Certificado de Ciência Ética e Conformidade Declarada")
                        .systemName("EthicalSoft Compliance")
                        .validationUrl("Validação disponível mediante consulta ao administrador do projeto.")
                        .issuerLabel("Analista de Qualidade (ADMIN)")
                        .footerNote("Certificado gerado sob demanda a partir dos resultados consolidados do projeto. " +
                                "Não substitui auditoria externa nem certificação normativa independente.")
                        .build()
        );
    }

    private void reconcileConfig(PdfDocumentConfigDocument seed) {
        if (seed == null || seed.getKey() == null || seed.getKey().isBlank()) {
            return;
        }
        repository.findByKey(seed.getKey()).ifPresentOrElse(
                existing -> updateExistingConfig(existing, seed),
                () -> repository.save(seed));
    }

    private void updateExistingConfig(PdfDocumentConfigDocument existing, PdfDocumentConfigDocument seed) {
        boolean changed = applyMandatoryFields(existing, seed);
        changed |= applyOptionalFields(existing, seed);
        if (changed) {
            repository.save(existing);
            log.info("[pdf-config-init] Configuração '{}' reconciliada com os valores canônicos.", seed.getKey());
        }
    }

    private boolean applyMandatoryFields(PdfDocumentConfigDocument existing, PdfDocumentConfigDocument seed) {
        boolean changed = false;
        if (!java.util.Objects.equals(existing.getDocumentTitle(), seed.getDocumentTitle())) {
            existing.setDocumentTitle(seed.getDocumentTitle());
            changed = true;
        }
        if (!java.util.Objects.equals(existing.getTemplateLink(), seed.getTemplateLink())) {
            existing.setTemplateLink(seed.getTemplateLink());
            changed = true;
        }
        if (!java.util.Objects.equals(existing.getFooterNote(), seed.getFooterNote())) {
            existing.setFooterNote(seed.getFooterNote());
            changed = true;
        }
        return changed;
    }

    private boolean applyOptionalFields(PdfDocumentConfigDocument existing, PdfDocumentConfigDocument seed) {
        boolean changed = false;
        if (isBlank(existing.getSystemName()) && !isBlank(seed.getSystemName())) {
            existing.setSystemName(seed.getSystemName());
            changed = true;
        }
        if (isBlank(existing.getImpactSummary()) && !isBlank(seed.getImpactSummary())) {
            existing.setImpactSummary(seed.getImpactSummary());
            changed = true;
        }
        if (isEmpty(existing.getDefaultCorrectiveActions()) && !isEmpty(seed.getDefaultCorrectiveActions())) {
            existing.setDefaultCorrectiveActions(seed.getDefaultCorrectiveActions());
            changed = true;
        }
        if (isBlank(existing.getValidationUrl()) && !isBlank(seed.getValidationUrl())) {
            existing.setValidationUrl(seed.getValidationUrl());
            changed = true;
        }
        if (isBlank(existing.getIssuerLabel()) && !isBlank(seed.getIssuerLabel())) {
            existing.setIssuerLabel(seed.getIssuerLabel());
            changed = true;
        }
        return changed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(List<String> value) {
        return value == null || value.isEmpty();
    }
}
