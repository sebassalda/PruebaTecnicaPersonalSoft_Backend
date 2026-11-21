
package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.ModuleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.entity.UserModuleEntity;
import com.jssv.brp_offer_management.security.repository.ModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import com.jssv.brp_offer_management.security.service.UserModuleCommandService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserModuleCommandServiceImpl
        implements UserModuleCommandService {

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final UserModuleRepository userModuleRepository;

    @Override
    @Transactional
    public UserResponse replaceModules(
            Long userId,
            List<String> modules,
            Long authenticatedUserId
    ) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        if (modules == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La lista de módulos es obligatoria"
            );
        }

        List<String> normalizedCodes = new ArrayList<>();
        Set<String> uniqueCodes = new HashSet<>();

        for (String code : modules) {

            if (code == null || code.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Los códigos de módulos no pueden estar vacíos"
                );
            }

            String normalized = code.trim()
                    .toUpperCase(Locale.ROOT);

            if (!uniqueCodes.add(normalized)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No se permiten módulos duplicados: " + normalized
                );
            }

            normalizedCodes.add(normalized);
        }

        if (userId.equals(authenticatedUserId)
                && !uniqueCodes.contains("USER_MANAGEMENT")) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puede revocar su propio permiso USER_MANAGEMENT"
            );
        }

        // Validar todos los módulos antes de modificar asignaciones.
        List<ModuleEntity> validModules = new ArrayList<>();

        for (String code : normalizedCodes) {

            ModuleEntity module = moduleRepository.findByCodigo(code)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Módulo inexistente: " + code
                    ));

            validModules.add(module);
        }

        // Eliminar asignaciones anteriores.
        userModuleRepository.deleteAllByUsuarioId(userId);

        // Crear las nuevas asignaciones.
        List<UserModuleEntity> assignments = new ArrayList<>();

        for (ModuleEntity module : validModules) {

            UserModuleEntity assignment = new UserModuleEntity();
            assignment.setUsuarioId(userId);
            assignment.setModuloId(module.getId());

            assignments.add(assignment);
        }

        userModuleRepository.saveAllAndFlush(assignments);

        List<String> updatedCodes = userModuleRepository
                .findModuleCodesByUserId(userId)
                .stream()
                .sorted()
                .toList();

        return new UserResponse(
                user.getId(),
                user.getNombre(),
                user.getEmail(),
                user.getRol().getNombre(),
                user.isActivo(),
                updatedCodes
        );
    }
}
