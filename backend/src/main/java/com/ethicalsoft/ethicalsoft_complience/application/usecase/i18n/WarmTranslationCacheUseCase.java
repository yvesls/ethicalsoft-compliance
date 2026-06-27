package com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateQuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarmTranslationCacheUseCase {

    private final ProjectTemplateRepository templateRepository;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final TranslateDynamicTextUseCase translateUseCase;

    public record WarmCacheResult(
            int uniqueTexts,
            int languagesProcessed,
            int totalCalls,
            int translated,
            int skipped,
            long elapsedMillis,
            List<String> languages
    ) {}

    public WarmCacheResult execute(List<String> requestedLanguages) {
        return execute(requestedLanguages, null);
    }

    public WarmCacheResult execute(List<String> requestedLanguages, Long userId) {
        long startTime = System.currentTimeMillis();

        List<SupportedLanguage> targets = resolveTargets(requestedLanguages);
        Set<String> texts = collectAllTexts();

        log.info("[i18n-warm] Aquecimento iniciado: {} textos únicos × {} idiomas (userId={})",
                texts.size(), targets.size(), userId);

        int translated = 0;
        int skipped = 0;
        int processed = 0;

        for (String text : texts) {
            for (SupportedLanguage target : targets) {
                try {
                    String result = translateUseCase.execute(text, target.code(), userId);
                    if (result == null || result.equals(text)) {
                        skipped++;
                    } else {
                        translated++;
                    }
                } catch (RuntimeException e) {
                    log.warn("[i18n-warm] Falha ao processar texto ({}): {}",
                            target.code(), e.getMessage());
                    skipped++;
                }
            }
            processed++;
            if (processed % 50 == 0) {
                log.info("[i18n-warm] Progresso: {}/{} textos processados", processed, texts.size());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[i18n-warm] Aquecimento concluído em {} ms: {} traduções efetivas, {} ignoradas",
                elapsed, translated, skipped);

        return new WarmCacheResult(
                texts.size(),
                targets.size(),
                texts.size() * targets.size(),
                translated,
                skipped,
                elapsed,
                targets.stream().map(SupportedLanguage::code).toList()
        );
    }

    private List<SupportedLanguage> resolveTargets(List<String> requestedLanguages) {
        if (requestedLanguages == null || requestedLanguages.isEmpty()) {
            return Arrays.stream(SupportedLanguage.values())
                    .filter(language -> language != SupportedLanguage.DEFAULT)
                    .toList();
        }
        List<SupportedLanguage> resolved = new ArrayList<>();
        for (String code : requestedLanguages) {
            SupportedLanguage.fromCode(code).ifPresent(language -> {
                if (language != SupportedLanguage.DEFAULT && !resolved.contains(language)) {
                    resolved.add(language);
                }
            });
        }
        return resolved;
    }

    private Set<String> collectAllTexts() {
        Set<String> texts = new LinkedHashSet<>();
        templateRepository.findAll().forEach(template -> collectTemplateTexts(template, texts));
        questionMetadataRepository.findAll().forEach(metadata -> addText(texts, metadata.getTheme()));
        return texts;
    }

    private void collectTemplateTexts(ProjectTemplate template, Set<String> texts) {
        addText(texts, template.getName());
        addText(texts, template.getDescription());
        if (template.getStages() != null) {
            template.getStages().forEach(stage -> addText(texts, stage.getName()));
        }
        if (template.getIterations() != null) {
            template.getIterations().forEach(iteration -> addText(texts, iteration.getName()));
        }
        if (template.getQuestionnaires() != null) {
            template.getQuestionnaires().forEach(questionnaire -> collectQuestionnaireTexts(questionnaire, texts));
        }
    }

    private void collectQuestionnaireTexts(TemplateQuestionnaireDTO questionnaire, Set<String> texts) {
        addText(texts, questionnaire.getName());
        addText(texts, questionnaire.getStageName());
        if (questionnaire.getStageNames() != null) {
            questionnaire.getStageNames().forEach(name -> addText(texts, name));
        }
        if (questionnaire.getQuestions() != null) {
            questionnaire.getQuestions().forEach(question -> {
                addText(texts, question.getValue());
                addText(texts, question.getStageName());
            });
        }
    }

    private void addText(Set<String> texts, String value) {
        if (value != null && !value.isBlank()) {
            texts.add(value.trim());
        }
    }
}
