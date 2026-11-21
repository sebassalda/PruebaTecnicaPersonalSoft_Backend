package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.request.LoginRequest;
import com.jssv.brp_offer_management.security.dto.response.AuthResponse;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.jwt.JwtService;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import com.jssv.brp_offer_management.security.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserModuleRepository userModuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        UserEntity user = userRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(this::invalidCredentials);

        if (!user.isActivo()) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        String role = user.getRol().getNombre();

        List<String> modules = userModuleRepository
                .findModuleCodesByUserId(user.getId());

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                role,
                modules
        );

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                role,
                modules,
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas"
        );
    }
}
