package com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserLanguagePreferenceDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserLanguagePreferenceRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserLanguageUseCase {

    private final UserLanguagePreferenceRepository repository;

    public String execute(Long userId) {
        return repository.findByUserId(userId)
                .map(UserLanguagePreferenceDocument::getLanguage)
                .map(SupportedLanguage::fromCodeOrDefault)
                .orElse(SupportedLanguage.DEFAULT)
                .code();
    }
}
