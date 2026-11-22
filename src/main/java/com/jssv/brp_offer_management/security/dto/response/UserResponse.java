package com.jssv.brp_offer_management.security.dto.response;

import java.util.List;

public record UserResponse(
        Long id,
        String nombre,
        String email,
        String role,
        boolean activo,
        List<String> modules
) {
}
