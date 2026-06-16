package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.AcceptTermsDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.AuthCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcceptTermsUseCase {

    private final AuthCommandPort authCommandPort;

    public void execute(AcceptTermsDTO acceptTermsDTO) {
        authCommandPort.acceptTerms(acceptTermsDTO);
    }
}
