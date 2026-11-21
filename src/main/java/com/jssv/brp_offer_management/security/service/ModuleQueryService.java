package com.jssv.brp_offer_management.security.service;

import com.jssv.brp_offer_management.security.dto.response.ModuleResponse;

import java.util.List;

public interface ModuleQueryService {

    List<ModuleResponse> findAll();
}
