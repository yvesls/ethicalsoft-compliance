package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n.GetUserLanguageUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n.TranslateDynamicTextUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n.UpdateUserLanguageUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n.WarmTranslationCacheUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n.LanguagePreferenceDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n.SupportedLanguageDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n.TranslateRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n.TranslateResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n.WarmCacheRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/i18n")
@RequiredArgsConstructor
public class I18nController {

    private final GetUserLanguageUseCase getUserLanguageUseCase;
    private final UpdateUserLanguageUseCase updateUserLanguageUseCase;
    private final TranslateDynamicTextUseCase translateDynamicTextUseCase;
    private final WarmTranslationCacheUseCase warmTranslationCacheUseCase;

    @GetMapping("/languages")
    public List<SupportedLanguageDTO> listSupportedLanguages() {
        return Arrays.stream(SupportedLanguage.values())
                .map(language -> new SupportedLanguageDTO(
                        language.code(),
                        language.label(),
                        language == SupportedLanguage.DEFAULT))
                .toList();
    }

    @GetMapping("/me/language")
    public LanguagePreferenceDTO getMyLanguage(@AuthenticationPrincipal User currentUser) {
        requireAuthenticated(currentUser);
        return new LanguagePreferenceDTO(getUserLanguageUseCase.execute(currentUser.getId()));
    }

    @PatchMapping("/me/language")
    public LanguagePreferenceDTO updateMyLanguage(@AuthenticationPrincipal User currentUser,
                                                  @Valid @RequestBody LanguagePreferenceDTO request) {
        requireAuthenticated(currentUser);
        String updated = updateUserLanguageUseCase.execute(currentUser.getId(), request.language());
        return new LanguagePreferenceDTO(updated);
    }

    @PostMapping("/translate")
    public TranslateResponseDTO translate(@Valid @RequestBody TranslateRequestDTO request) {
        String translated = translateDynamicTextUseCase.execute(request.text(), request.language());
        return new TranslateResponseDTO(request.text(), translated, request.language());
    }

    @PostMapping("/admin/warm-cache")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WarmTranslationCacheUseCase.WarmCacheResult warmCache(
            @RequestBody(required = false) WarmCacheRequestDTO request) {
        return warmTranslationCacheUseCase.execute(
                request != null ? request.languages() : null);
    }

    private void requireAuthenticated(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("i18n.unauthenticated",
                    "É necessário estar autenticado para acessar as preferências de idioma.");
        }
    }
}
