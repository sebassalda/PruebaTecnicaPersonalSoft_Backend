package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.request.CreateUserRequest;
import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.RoleRepository;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import com.jssv.brp_offer_management.security.service.UserCommandService;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserModuleRepository userModuleRepository;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {

        String nombre = request.nombre().trim();

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String roleName = request.role()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (nombre.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio"
            );
        }

        if (email.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo es obligatorio"
            );
        }

        int passwordBytes = request.password()
                .getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña no puede superar 72 bytes UTF-8"
            );
        }

        RoleEntity role = roleRepository.findByNombre(roleName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El rol especificado no existe"
                ));

        UserEntity user = new UserEntity();

        user.setNombre(nombre);
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setActivo(true);
        user.setRol(role);

        try {
            UserEntity saved = userRepository.saveAndFlush(user);

            return new UserResponse(
                    saved.getId(),
                    saved.getNombre(),
                    saved.getEmail(),
                    saved.getRol().getNombre(),
                    saved.isActivo(),
                    List.of()
            );

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo crear el usuario: posible correo duplicado",
                    ex
            );
        }
    }


    @Override
    @Transactional
    public UserResponse updateStatus(
            Long userId,
            boolean activo,
            Long authenticatedUserId
    ) {

        if (userId.equals(authenticatedUserId) && !activo) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puede desactivar su propio usuario"
            );
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        if (!activo && user.isActivo()
                && "ADMINISTRADOR".equals(user.getRol().getNombre())
                && userRepository.countByActivoTrueAndRol_Nombre("ADMINISTRADOR") <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede desactivar al último administrador activo"
            );
        }

        user.setActivo(activo);

        UserEntity updated = userRepository.saveAndFlush(user);

        return new UserResponse(
                updated.getId(),
                updated.getNombre(),
                updated.getEmail(),
                updated.getRol().getNombre(),
                updated.isActivo(),
                List.copyOf(
                        userModuleRepository.findModuleCodesByUserId(
                                updated.getId()
                        )
                )
        );
    }

}
