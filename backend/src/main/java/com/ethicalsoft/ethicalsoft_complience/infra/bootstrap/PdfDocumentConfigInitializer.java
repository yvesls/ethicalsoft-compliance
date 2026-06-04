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
            configsToSeed().forEach(this::insertIfMissing);
        } catch (Exception e) {
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

    private void insertIfMissing(PdfDocumentConfigDocument config) {
        if (config == null || config.getKey() == null || config.getKey().isBlank()) {
            return;
        }
        if (repository.findByKey(config.getKey()).isPresent()) {
            return;
        }
        repository.save(config);
    }
}
