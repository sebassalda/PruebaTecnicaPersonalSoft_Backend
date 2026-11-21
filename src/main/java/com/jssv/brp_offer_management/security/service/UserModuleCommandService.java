
package com.jssv.brp_offer_management.security.service;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;

import java.util.List;

public interface UserModuleCommandService {

    UserResponse replaceModules(
            Long userId,
            List<String> modules,
            Long authenticatedUserId
    );
}
