package com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserLanguagePreferenceDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserLanguagePreferenceRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserLanguageUseCase {

    private final UserLanguagePreferenceRepository repository;

    public String execute(Long userId, String languageCode) {
        if (userId == null) {
            throw new BusinessException("i18n.user_required", "Usuário não informado.");
        }
        SupportedLanguage language = SupportedLanguage.fromCode(languageCode)
                .orElseThrow(() -> new BusinessException("i18n.language_not_supported",
                        "Idioma não suportado: " + languageCode));

        UserLanguagePreferenceDocument preference = repository.findByUserId(userId)
                .orElseGet(() -> UserLanguagePreferenceDocument.builder()
                        .userId(userId)
                        .build());
        preference.setLanguage(language.code());
        preference.setUpdatedAt(LocalDateTime.now());
        repository.save(preference);

        log.info("[i18n] Preferência de idioma atualizada usuário={} idioma={}", userId, language.code());
        return language.code();
    }
}
