package com.jssv.brp_offer_management.security.config;

import com.jssv.brp_offer_management.security.service.ModuleAuthorizationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("moduleSecurity")
@RequiredArgsConstructor
public class ModuleSecurity {

    private final ModuleAuthorizationService authorizationService;

    public boolean hasAccess(
            Authentication authentication,
            String moduleCode
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        Number userId = jwt.getClaim("userId");

        if (userId == null) {
            return false;
        }

        return authorizationService.hasAccess(
                userId.longValue(),
                moduleCode
        );
    }
}
