package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.QuestionAnalysis;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.QuestionDataType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class QuestionContextAnalyzer {

    private static final Map<QuestionDataType, List<String>> TYPE_KEYWORDS = Map.of(
            QuestionDataType.ANSWER_SUMMARY, List.of(
                    "resposta", "respostas", "questão", "questões", "pergunta", "perguntas",
                    "sim", "não", "respondeu", "responderam", "proporção", "taxa",
                    "quantas", "quantos", "total de respostas", "classificação",
                    "conformidade das questões", "resultado das perguntas"
            ),
            QuestionDataType.ROLE_STAGE, List.of(
                    "papel", "papeis", "papéis", "função", "funções", "cargo", "cargos",
                    "etapa", "etapas", "fase", "fases", "heatmap", "mapa de calor",
                    "cruzamento", "iem", "conformidade por papel", "conformidade por etapa",
                    "papel e etapa", "role", "stage"
            ),
            QuestionDataType.JUSTIFICATIONS, List.of(
                    "justificativa", "justificativas", "justificou", "justificaram",
                    "por que", "porquê", "motivo", "motivos", "razão", "razões",
                    "explicação", "explicações", "explicou", "disseram", "falaram",
                    "comentaram", "texto", "observação", "observações",
                    "anotação", "anotações", "nota escrita"
            ),
            QuestionDataType.WORD_CLOUD, List.of(
                    "termos", "palavras", "frequente", "frequentes", "nuvem",
                    "word cloud", "palavras-chave", "mais citado", "mais mencionado",
                    "recorrente", "recorrentes", "vocabulário"
            )
    );

    private static final Map<String, List<String>> DOMAIN_KEYWORDS = Map.of(
            "ETHICS", List.of(
                    "ética", "ético", "eticamente", "responsabilidade", "privacidade",
                    "transparência", "impacto social", "dívida ética", "ética digital"
            ),
            "SECURITY", List.of(
                    "segurança", "seguro", "proteção", "vulnerabilidade", "acesso",
                    "dados sensíveis", "controle de acesso", "criptografia"
            ),
            "PROCESS", List.of(
                    "processo", "processual", "documentação", "rastreabilidade",
                    "governança", "gestão", "procedimento", "dívida técnica"
            ),
            "FAIRNESS", List.of(
                    "equidade", "viés", "bias", "inclusão", "acessibilidade",
                    "algoritmo", "discriminação", "fairness"
            ),
            "ESG", List.of(
                    "esg", "ambiental", "sustentabilidade", "governança corporativa",
                    "social", "impacto ambiental"
            ),
            "QUALITY", List.of(
                    "qualidade", "teste", "testes", "código", "padrão de código",
                    "critério de aceite", "tech debt"
            )
    );

    public QuestionAnalysis analyze(String question) {
        if (question == null || question.isBlank()) {
            return QuestionAnalysis.minimal();
        }

        String lower = question.toLowerCase();
        Set<QuestionDataType> dataTypes = EnumSet.noneOf(QuestionDataType.class);

        for (Map.Entry<QuestionDataType, List<String>> entry : TYPE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    dataTypes.add(entry.getKey());
                    break;
                }
            }
        }

        String domainFilter = null;
        outer:
        for (Map.Entry<String, List<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    domainFilter = entry.getKey();
                    break outer;
                }
            }
        }

        if (domainFilter != null && !dataTypes.contains(QuestionDataType.JUSTIFICATIONS)) {
            dataTypes.add(QuestionDataType.JUSTIFICATIONS);
        }

        if (dataTypes.isEmpty()) {
            log.debug("[qa-analyzer] Nenhum tipo de dado específico detectado — contexto mínimo");
            return QuestionAnalysis.minimal();
        }

        log.debug("[qa-analyzer] Tipos detectados={}, domínio={}", dataTypes, domainFilter);
        return new QuestionAnalysis(dataTypes, domainFilter);
    }
}
