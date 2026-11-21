package com.jssv.brp_offer_management.brp.controller;

import com.jssv.brp_offer_management.brp.dto.response.BrpResponse;
import com.jssv.brp_offer_management.brp.service.BrpService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/brps")
@RequiredArgsConstructor
public class BrpController {

    private final BrpService brpService;

    @GetMapping
    @PreAuthorize(
            "@moduleSecurity.hasAccess(authentication, 'BRP_CATALOG')"
    )
    public ResponseEntity<Page<BrpResponse>> findAll(
            @PageableDefault(size = 10, sort = "codigo")
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                brpService.findAll(pageable)
        );
    }

    @GetMapping("/latest-ingestion")
    @PreAuthorize(
            "@moduleSecurity.hasAccess(authentication, 'BRP_CATALOG')"
    )
    public ResponseEntity<Map<String, LocalDate>> getLatestIngestion() {
        return brpService.getLatestIngestionDate()
                .map(date -> ResponseEntity.ok(
                        Map.of("latestIngestionDate", date)
                ))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "@moduleSecurity.hasAccess(authentication, 'BRP_CATALOG')"
    )
    public ResponseEntity<BrpResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                brpService.findById(id)
        );
    }
}
