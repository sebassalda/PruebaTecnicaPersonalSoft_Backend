package com.jssv.brp_offer_management.security.service;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserQueryService {

    Page<UserResponse> findAll(Pageable pageable);

    UserResponse findById(Long id);
}
