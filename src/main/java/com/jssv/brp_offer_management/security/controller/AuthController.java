package com.jssv.brp_offer_management.security.controller;

import com.jssv.brp_offer_management.security.dto.request.LoginRequest;
import com.jssv.brp_offer_management.security.dto.response.AuthResponse;
import com.jssv.brp_offer_management.security.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}
