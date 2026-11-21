
package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.ModuleEntity;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.entity.UserModuleEntity;
import com.jssv.brp_offer_management.security.repository.ModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserModuleCommandServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private UserModuleRepository userModuleRepository;

    @InjectMocks
    private UserModuleCommandServiceImpl service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setNombre("ANALISTA");

        user = new UserEntity();
        user.setId(10L);
        user.setNombre("Usuario de prueba");
        user.setEmail("usuario@brp.local");
        user.setActivo(true);
        user.setRol(role);
    }

    @Test
    void shouldAssignModules() {

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(moduleRepository.findByCodigo("BRP_CATALOG"))
                .thenReturn(Optional.of(
                        createModule(1L, "BRP_CATALOG")
                ));

        when(moduleRepository.findByCodigo("OFFER_REGISTRATION"))
                .thenReturn(Optional.of(
                        createModule(2L, "OFFER_REGISTRATION")
                ));

        when(userModuleRepository.findModuleCodesByUserId(10L))
                .thenReturn(List.of(
                        "BRP_CATALOG",
                        "OFFER_REGISTRATION"
                ));

        UserResponse response = service.replaceModules(
                10L,
                List.of("BRP_CATALOG", "OFFER_REGISTRATION"),
                1L
        );

        assertEquals(2, response.modules().size());

        verify(userModuleRepository).deleteAllByUsuarioId(10L);
        verify(userModuleRepository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldRevokeAllModules() {

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(userModuleRepository.findModuleCodesByUserId(10L))
                .thenReturn(List.of());

        UserResponse response = service.replaceModules(
                10L,
                List.of(),
                1L
        );

        assertTrue(response.modules().isEmpty());

        verify(userModuleRepository).deleteAllByUsuarioId(10L);
        verify(userModuleRepository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldRejectDuplicateModules() {

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.replaceModules(
                        10L,
                        List.of("BRP_CATALOG", "brp_catalog"),
                        1L
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(userModuleRepository, never())
                .deleteAllByUsuarioId(anyLong());
    }

    @Test
    void shouldRejectUnknownModule() {

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(moduleRepository.findByCodigo("UNKNOWN_MODULE"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.replaceModules(
                        10L,
                        List.of("UNKNOWN_MODULE"),
                        1L
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(userModuleRepository, never())
                .deleteAllByUsuarioId(anyLong());
    }

    @Test
    void shouldRejectSelfRevocation() {

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.replaceModules(
                        10L,
                        List.of("BRP_CATALOG"),
                        10L
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(userModuleRepository, never())
                .deleteAllByUsuarioId(anyLong());
    }

    @Test
    void shouldReturn404ForMissingUser() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.replaceModules(
                        999L,
                        List.of("BRP_CATALOG"),
                        1L
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    private ModuleEntity createModule(
            Long id,
            String code
    ) {

        ModuleEntity module = new ModuleEntity();
        module.setId(id);
        module.setCodigo(code);
        module.setNombre(code);

        return module;
    }
}
