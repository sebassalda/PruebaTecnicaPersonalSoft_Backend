
package com.jssv.brp_offer_management.security.controller;

import com.jssv.brp_offer_management.security.dto.request.CreateUserRequest;
import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.service.UserCommandService;
import com.jssv.brp_offer_management.security.service.UserQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jssv.brp_offer_management.security.dto.request.UpdateUserStatusRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.jssv.brp_offer_management.security.dto.request.UpdateUserModulesRequest;
import com.jssv.brp_offer_management.security.service.UserModuleCommandService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final UserModuleCommandService userModuleCommandService;

    @GetMapping
    @PreAuthorize(
            "hasAuthority('ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'USER_MANAGEMENT')"
    )
    public Page<UserResponse> findAll(
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable
    ) {
        return userQueryService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'USER_MANAGEMENT')"
    )
    public UserResponse findById(@PathVariable Long id) {
        return userQueryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "hasAuthority('ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'USER_MANAGEMENT')"
    )
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userCommandService.create(request);
    }


    @PatchMapping("/{id}/status")
    @PreAuthorize(
            "hasAuthority('ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'USER_MANAGEMENT')"
    )
    public UserResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long authenticatedUserId =
                jwt.getClaim("userId");

        return userCommandService.updateStatus(
                id,
                request.activo(),
                authenticatedUserId
        );
    }


    @PutMapping("/{id}/modules")
    @PreAuthorize(
            "hasAuthority('ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'USER_MANAGEMENT')"
    )
    public UserResponse replaceModules(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserModulesRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long authenticatedUserId = jwt.getClaim("userId");

        return userModuleCommandService.replaceModules(
                id,
                request.modules(),
                authenticatedUserId
        );
    }

}
