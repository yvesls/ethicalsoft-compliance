package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserResolutionPolicy {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResolutionResult resolveOrCreateUser(RepresentativeDTO dto) {
        if (StringUtils.hasText(dto.getEmail())) {
            return userRepository.findByEmail(dto.getEmail())
                    .map(user -> new UserResolutionResult(user, Optional.empty()))
                    .orElseGet(() -> createUserFromDto(dto));
        }
        throw new IllegalArgumentException("Representante precisa informar userId ou email.");
    }

    private UserResolutionResult createUserFromDto(RepresentativeDTO dto) {
        if (!StringUtils.hasText(dto.getEmail()) || !StringUtils.hasText(dto.getFirstName()) || !StringUtils.hasText(dto.getLastName())) {
            throw new IllegalArgumentException("Dados insuficientes para criar um usuário.");
        }
        String tempPassword = generateTempPassword();

        User user = new User();
        user.setRole(UserRoleEnum.USER);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setFirstAccess(true);
        user.setAcceptedTerms(false);

        user = userRepository.save(user);

        return new UserResolutionResult(user, Optional.of(tempPassword));
    }

    private String generateTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public record UserResolutionResult(User user, Optional<String> temporaryPassword) {}
}

