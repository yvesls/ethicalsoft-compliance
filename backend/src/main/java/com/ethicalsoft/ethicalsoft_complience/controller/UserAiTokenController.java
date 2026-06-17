package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.token.DeleteUserAiTokenUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.token.GetUserAiTokenStatusUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.token.UpdateUserAiTokenUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiTokenStatusDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.UpdateAiTokenRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/me/token")
@RequiredArgsConstructor
public class UserAiTokenController {

    private final GetUserAiTokenStatusUseCase getStatusUseCase;
    private final UpdateUserAiTokenUseCase updateUseCase;
    private final DeleteUserAiTokenUseCase deleteUseCase;

    @GetMapping("/status")
    public AiTokenStatusDTO getStatus(@AuthenticationPrincipal User currentUser) {
        requireAuthenticated(currentUser);
        return getStatusUseCase.execute(currentUser.getId());
    }

    @PutMapping
    public AiTokenStatusDTO update(@AuthenticationPrincipal User currentUser,
                                   @Valid @RequestBody UpdateAiTokenRequestDTO request) {
        requireAuthenticated(currentUser);
        return updateUseCase.execute(currentUser.getId(), request.token());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser) {
        requireAuthenticated(currentUser);
        deleteUseCase.execute(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    private void requireAuthenticated(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(
                    "É necessário estar autenticado para gerenciar seu token de IA.");
        }
    }
}
