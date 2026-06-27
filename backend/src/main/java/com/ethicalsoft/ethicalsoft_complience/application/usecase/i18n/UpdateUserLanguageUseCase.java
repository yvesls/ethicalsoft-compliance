package com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserLanguagePreferenceDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserLanguagePreferenceRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UpdateUserLanguageUseCase {

    private final UserLanguagePreferenceRepository repository;

    public String execute(Long userId, String languageCode) {
        SupportedLanguage language = SupportedLanguage.fromCode(languageCode)
                .orElseThrow(() -> new BusinessException(
                        "Idioma não suportado: " + languageCode));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UserLanguagePreferenceDocument doc = repository.findByUserId(userId)
                .orElseGet(() -> UserLanguagePreferenceDocument.builder()
                        .userId(userId)
                        .build());
        doc.setLanguage(language.code());
        doc.setUpdatedAt(now);
        repository.save(doc);
        return language.code();
    }
}
