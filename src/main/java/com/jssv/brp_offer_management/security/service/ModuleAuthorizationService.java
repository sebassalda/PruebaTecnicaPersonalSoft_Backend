package com.jssv.brp_offer_management.security.service;

public interface ModuleAuthorizationService {

    boolean hasAccess(Long userId, String moduleCode);
}
