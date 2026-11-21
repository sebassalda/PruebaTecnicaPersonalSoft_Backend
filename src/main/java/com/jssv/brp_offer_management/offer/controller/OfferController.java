
package com.jssv.brp_offer_management.offer.controller;

import com.jssv.brp_offer_management.offer.dto.request.OfferCreateRequest;
import com.jssv.brp_offer_management.offer.dto.response.OfferCreateResponse;
import com.jssv.brp_offer_management.offer.dto.response.OfferDetailResponse;
import com.jssv.brp_offer_management.offer.dto.response.OfferSummaryResponse;
import com.jssv.brp_offer_management.offer.service.OfferCommandService;
import com.jssv.brp_offer_management.offer.service.OfferQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferCommandService offerCommandService;
    private final OfferQueryService offerQueryService;

    @PostMapping
    @PreAuthorize("""
        hasAuthority('ROLE_RADICADOR')
        and @moduleSecurity.hasAccess(authentication, 'OFFER_REGISTRATION')
        """)
    public ResponseEntity<OfferCreateResponse> createOffer(
            @Valid @RequestBody OfferCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long radicadorId = jwt.getClaim("userId");

        Long offerId = offerCommandService.createOffer(
                request,
                radicadorId
        );

        OfferCreateResponse response = new OfferCreateResponse(
                offerId,
                "RADICADO",
                "Oferta registrada correctamente"
        );

        return ResponseEntity
                .created(URI.create("/api/offers/" + offerId))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("""
        hasAnyAuthority(
            'ROLE_RADICADOR',
            'ROLE_ANALISTA',
            'ROLE_JEFE',
            'ROLE_ADMINISTRADOR'
        )
        and @moduleSecurity.hasAccess(authentication, 'OFFER_TRACKING')
        """)
    public ResponseEntity<Page<OfferSummaryResponse>> findAll(
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = jwt.getClaim("userId");
        String role = jwt.getClaimAsString("role");

        Page<OfferSummaryResponse> response =
                offerQueryService.findAll(
                        pageable,
                        userId,
                        role
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
        hasAnyAuthority(
            'ROLE_RADICADOR',
            'ROLE_ANALISTA',
            'ROLE_JEFE',
            'ROLE_ADMINISTRADOR'
        )
        and @moduleSecurity.hasAccess(authentication, 'OFFER_TRACKING')
        """)
    public ResponseEntity<OfferDetailResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = jwt.getClaim("userId");
        String role = jwt.getClaimAsString("role");

        OfferDetailResponse response =
                offerQueryService.findById(
                        id,
                        userId,
                        role
                );

        return ResponseEntity.ok(response);
    }
}
