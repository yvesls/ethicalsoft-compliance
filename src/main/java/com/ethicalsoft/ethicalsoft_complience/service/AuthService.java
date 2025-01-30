package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    public Authentication login(AuthDTO authDTO, AuthenticationManager authenticationManager) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(authDTO.getUsername(), authDTO.getPassword());
        return authenticationManager.authenticate(usernamePassword);
    }

    public void register(RegisterUserDTO registerUserDTO) {
        if(!registerUserDTO.isAcceptedTerms()) {
            throw new RuntimeException("The terms were not accepted");
        }

        if(!ObjectUtils.isEmpty(userRepository.findByEmail(registerUserDTO.getEmail()))) {
            new RuntimeException("Email already exists");
        }
        var encryptedPassword = new BCryptPasswordEncoder().encode(registerUserDTO.getPassword());
        registerUserDTO.setPassword(encryptedPassword);

        var newUser = modelMapper.map( registerUserDTO, User.class );
        newUser.setRole(UserRoleEnum.ADMIN);

        this.userRepository.save(newUser);
    }
}
