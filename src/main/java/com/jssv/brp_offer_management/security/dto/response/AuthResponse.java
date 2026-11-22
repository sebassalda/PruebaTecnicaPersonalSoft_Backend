package com.jssv.brp_offer_management.security.dto.response;

import java.util.List;

public record AuthResponse(
        Long userId,
        String email,
        String role,
        List<String> modules,
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
