package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationTemplateDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class NotificationTemplateInitializer {

    private static final String QUESTIONNAIRE_REMINDER = "QUESTIONNAIRE_REMINDER";

    private final NotificationTemplateRepository repository;

    @PostConstruct
    public void seedTemplates() {
        templatesToSeed().forEach(this::insertIfMissing);
    }

    private List<NotificationTemplateDocument> templatesToSeed() {
        return List.of(
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_REMINDER)
                        .whoCanSend(List.of("QUALITY_ANALYST", "PROJECT_MANAGER"))
                        .recipients(List.of("AGENT"))
                        .title("Questionário pendente: {questionnaireName}")
                        .body("Olá {recipientName}, existe um questionário pendente no projeto {projectName}: {questionnaireName} ({period}).")
                        .channels(List.of("INTERNAL", "EMAIL"))
                        .build()
        );
    }

    /**
     * Seed idempotente: só cria se não existir.
     * Obs: evitar sobrescrever para não apagar alterações manuais no Mongo em ambiente real.
     */
    private void insertIfMissing(NotificationTemplateDocument template) {
        if (template == null || template.getKey() == null || template.getKey().isBlank()) {
            return;
        }
        if (repository.findByKey(template.getKey()).isPresent()) {
            return;
        }
        repository.save(template);
    }
}
