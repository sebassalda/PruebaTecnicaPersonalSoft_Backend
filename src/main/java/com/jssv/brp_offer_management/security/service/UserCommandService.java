package com.jssv.brp_offer_management.security.service;

import com.jssv.brp_offer_management.security.dto.request.CreateUserRequest;
import com.jssv.brp_offer_management.security.dto.response.UserResponse;

public interface UserCommandService {

    UserResponse create(CreateUserRequest request);

    UserResponse updateStatus(
            Long userId,
            boolean activo,
            Long authenticatedUserId
    );
}
