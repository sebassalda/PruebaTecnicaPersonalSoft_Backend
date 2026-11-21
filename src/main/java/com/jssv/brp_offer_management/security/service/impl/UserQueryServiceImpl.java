package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import com.jssv.brp_offer_management.security.service.UserQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final UserModuleRepository userModuleRepository;

    @Override
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public UserResponse findById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        return toResponse(user);
    }

    private UserResponse toResponse(UserEntity user) {

        List<String> modules =
                userModuleRepository.findModuleCodesByUserId(
                        user.getId()
                );

        return new UserResponse(
                user.getId(),
                user.getNombre(),
                user.getEmail(),
                user.getRol().getNombre(),
                user.isActivo(),
                List.copyOf(modules)
        );
    }
}
