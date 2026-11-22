package com.jssv.brp_offer_management.security.service;

import com.jssv.brp_offer_management.security.dto.request.LoginRequest;
import com.jssv.brp_offer_management.security.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
