package com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserLanguagePreferenceRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserLanguageUseCase {

    private final UserLanguagePreferenceRepository repository;

    public String execute(Long userId) {
        if (userId == null) {
            return SupportedLanguage.DEFAULT.code();
        }
        return repository.findByUserId(userId)
                .map(pref -> SupportedLanguage.fromCodeOrDefault(pref.getLanguage()).code())
                .orElse(SupportedLanguage.DEFAULT.code());
    }
}
