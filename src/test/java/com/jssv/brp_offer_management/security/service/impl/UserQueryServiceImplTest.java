package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.UserResponse;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserModuleRepository userModuleRepository;

    @InjectMocks
    private UserQueryServiceImpl service;

    private UserEntity user;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        user = mock(UserEntity.class);
        role = mock(RoleEntity.class);
    }

    @Test
    void shouldFindUserById() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);
        when(user.getNombre()).thenReturn("Administrador");
        when(user.getEmail()).thenReturn("admin@brp.local");
        when(user.getRol()).thenReturn(role);
        when(user.isActivo()).thenReturn(true);

        when(role.getNombre()).thenReturn("ADMINISTRADOR");

        when(userModuleRepository.findModuleCodesByUserId(1L))
                .thenReturn(List.of(
                        "BRP_CATALOG",
                        "USER_MANAGEMENT"
                ));

        UserResponse response = service.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("Administrador", response.nombre());
        assertEquals("admin@brp.local", response.email());
        assertEquals("ADMINISTRADOR", response.role());
        assertTrue(response.activo());
        assertEquals(2, response.modules().size());
        assertTrue(response.modules().contains("USER_MANAGEMENT"));
    }

    @Test
    void shouldReturn404WhenUserDoesNotExist() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(999L)
        );

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(userModuleRepository);
    }

    @Test
    void shouldReturnPaginatedUsers() {
        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(
                        List.of(user),
                        pageable,
                        1
                ));

        when(user.getId()).thenReturn(1L);
        when(user.getNombre()).thenReturn("Administrador");
        when(user.getEmail()).thenReturn("admin@brp.local");
        when(user.getRol()).thenReturn(role);
        when(user.isActivo()).thenReturn(true);

        when(role.getNombre()).thenReturn("ADMINISTRADOR");

        when(userModuleRepository.findModuleCodesByUserId(1L))
                .thenReturn(List.of("USER_MANAGEMENT"));

        Page<UserResponse> result = service.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().getFirst().id());

        verify(userRepository).findAll(pageable);
    }
}

