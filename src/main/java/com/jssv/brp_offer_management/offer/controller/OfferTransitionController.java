package com.jssv.brp_offer_management.offer.controller;

import com.jssv.brp_offer_management.offer.dto.response.OfferTransitionResponse;
import com.jssv.brp_offer_management.offer.service.OfferTransitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferTransitionController {
    private final OfferTransitionService service;

    @PostMapping("/{id}/take")
    @PreAuthorize("hasAuthority('ROLE_ANALISTA') and @moduleSecurity.hasAccess(authentication, 'OFFER_MANAGEMENT')")
    public ResponseEntity<OfferTransitionResponse> take(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return run(id, "TAKE", jwt);
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('ROLE_ANALISTA') and @moduleSecurity.hasAccess(authentication, 'OFFER_MANAGEMENT')")
    public ResponseEntity<OfferTransitionResponse> escalate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return run(id, "ESCALATE", jwt);
    }

    @PostMapping("/{id}/analysis/reject")
    @PreAuthorize("hasAuthority('ROLE_ANALISTA') and @moduleSecurity.hasAccess(authentication, 'OFFER_MANAGEMENT')")
    public ResponseEntity<OfferTransitionResponse> rejectAnalysis(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return run(id, "REJECT", jwt);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_JEFE') and @moduleSecurity.hasAccess(authentication, 'OFFER_APPROVAL')")
    public ResponseEntity<OfferTransitionResponse> approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return run(id, "APPROVE", jwt);
    }

    @PostMapping("/{id}/approval/reject")
    @PreAuthorize("hasAuthority('ROLE_JEFE') and @moduleSecurity.hasAccess(authentication, 'OFFER_APPROVAL')")
    public ResponseEntity<OfferTransitionResponse> rejectApproval(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return run(id, "REJECT", jwt);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('ROLE_JEFE') and @moduleSecurity.hasAccess(authentication, 'OFFER_APPROVAL')")
    public ResponseEntity<OfferTransitionResponse> returnToAnalyst(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return run(id, "RETURN", jwt);
    }

    private ResponseEntity<OfferTransitionResponse> run(Long id, String action, Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        return ResponseEntity.ok(service.transition(id, action,
                userId == null ? null : userId.longValue(), jwt.getClaimAsString("role")));
    }
}
