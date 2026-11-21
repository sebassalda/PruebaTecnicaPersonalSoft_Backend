package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import com.jssv.brp_offer_management.security.service.ModuleAuthorizationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("moduleAuthorizationService")
@RequiredArgsConstructor
public class ModuleAuthorizationServiceImpl
        implements ModuleAuthorizationService {

    private final UserRepository userRepository;
    private final UserModuleRepository userModuleRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, String moduleCode) {

        if (userId == null
                || moduleCode == null
                || moduleCode.isBlank()) {
            return false;
        }

        UserEntity user = userRepository.findById(userId)
                .orElse(null);

        if (user == null || !user.isActivo()) {
            return false;
        }

        return userModuleRepository
                .findModuleCodesByUserId(userId)
                .contains(moduleCode);
    }
}
