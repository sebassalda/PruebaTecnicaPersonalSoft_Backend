package com.jssv.brp_offer_management.security.controller;

import com.jssv.brp_offer_management.security.dto.response.ModuleResponse;
import com.jssv.brp_offer_management.security.service.ModuleQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleQueryService moduleQueryService;

    @GetMapping
    @PreAuthorize(
            "@moduleSecurity.hasAccess(authentication, 'USER_MANAGEMENT')"
    )
    public ResponseEntity<List<ModuleResponse>> findAll() {

        return ResponseEntity.ok(
                moduleQueryService.findAll()
        );
    }
}
