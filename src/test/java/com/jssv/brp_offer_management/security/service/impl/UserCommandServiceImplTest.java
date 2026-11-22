package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.request.CreateUserRequest;
import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.RoleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCommandServiceImpl service;

    private CreateUserRequest request;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        request = new CreateUserRequest(
                "María González",
                "MARIA@EMPRESA.COM",
                "ClaveSegura#2026",
                "analista"
        );

        role = new RoleEntity();
        role.setId(3L);
        role.setNombre("ANALISTA");
    }

    @Test
    void shouldCreateActiveUserWithoutModules() {

        when(roleRepository.findByNombre("ANALISTA"))
                .thenReturn(Optional.of(role));

        when(passwordEncoder.encode("ClaveSegura#2026"))
                .thenReturn("bcrypt-hash");

        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(10L);
                    return user;
                });

        UserResponse response = service.create(request);

        assertEquals(10L, response.id());
        assertEquals("María González", response.nombre());
        assertEquals("maria@empresa.com", response.email());
        assertEquals("ANALISTA", response.role());
        assertTrue(response.activo());
        assertTrue(response.modules().isEmpty());

        verify(passwordEncoder).encode("ClaveSegura#2026");

        verify(userRepository).saveAndFlush(argThat(user ->
                user.getNombre().equals("María González")
                        && user.getEmail().equals("maria@empresa.com")
                        && user.getPasswordHash().equals("bcrypt-hash")
                        && user.isActivo()
                        && user.getRol() == role
        ));
    }

    @Test
    void shouldRejectUnknownRole() {

        when(roleRepository.findByNombre("ANALISTA"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectPasswordLongerThan72Bytes() {

        CreateUserRequest invalidRequest = new CreateUserRequest(
                "María González",
                "maria@empresa.com",
                "á".repeat(37),
                "ANALISTA"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(invalidRequest)
        );

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldReturnConflictWhenPersistenceFails() {

        when(roleRepository.findByNombre("ANALISTA"))
                .thenReturn(Optional.of(role));

        when(passwordEncoder.encode(anyString()))
                .thenReturn("bcrypt-hash");

        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(409, exception.getStatusCode().value());
    }
}
