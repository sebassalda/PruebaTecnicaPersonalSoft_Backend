package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.RoleRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStatusServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserModuleRepository userModuleRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCommandServiceImpl service;

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
    void shouldDeactivateUser() {

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        when(userModuleRepository.findModuleCodesByUserId(10L))
                .thenReturn(List.of("BRP_CATALOG"));

        UserResponse response = service.updateStatus(
                10L,
                false,
                1L
        );

        assertFalse(response.activo());
        assertFalse(user.isActivo());
        assertEquals(10L, response.id());
        assertEquals(List.of("BRP_CATALOG"), response.modules());

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldReactivateUser() {

        user.setActivo(false);

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        when(userModuleRepository.findModuleCodesByUserId(10L))
                .thenReturn(List.of());

        UserResponse response = service.updateStatus(
                10L,
                true,
                1L
        );

        assertTrue(response.activo());
        assertTrue(user.isActivo());

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldRejectSelfDeactivation() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateStatus(10L, false, 10L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldReturn404ForMissingUser() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateStatus(999L, false, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository, never()).save(any());
    }
}
