
package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleAuthorizationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserModuleRepository userModuleRepository;

    @InjectMocks
    private ModuleAuthorizationServiceImpl authorizationService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setActivo(true);
    }

    @Test
    void shouldAllowAccessWhenUserHasModule() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userModuleRepository.findModuleCodesByUserId(1L))
                .thenReturn(List.of("BRP_CATALOG", "USER_MANAGEMENT"));

        boolean result = authorizationService.hasAccess(
                1L, "BRP_CATALOG"
        );

        assertTrue(result);
    }

    @Test
    void shouldDenyAccessWhenModuleIsNotAssigned() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userModuleRepository.findModuleCodesByUserId(1L))
                .thenReturn(List.of("USER_MANAGEMENT"));

        boolean result = authorizationService.hasAccess(
                1L, "BRP_CATALOG"
        );

        assertFalse(result);
    }

    @Test
    void shouldDenyAccessWhenUserIsInactive() {
        user.setActivo(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        boolean result = authorizationService.hasAccess(
                1L, "BRP_CATALOG"
        );

        assertFalse(result);

        verifyNoInteractions(userModuleRepository);
    }

    @Test
    void shouldDenyAccessWhenUserDoesNotExist() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        boolean result = authorizationService.hasAccess(
                999L, "BRP_CATALOG"
        );

        assertFalse(result);
    }

    @Test
    void shouldDenyAccessWhenUserIdIsNull() {
        assertFalse(
                authorizationService.hasAccess(null, "BRP_CATALOG")
        );

        verifyNoInteractions(userRepository, userModuleRepository);
    }

    @Test
    void shouldDenyAccessWhenModuleCodeIsBlank() {
        assertFalse(
                authorizationService.hasAccess(1L, " ")
        );

        verifyNoInteractions(userRepository, userModuleRepository);
    }

    @Test
    void shouldReflectPermissionRevocation() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userModuleRepository.findModuleCodesByUserId(1L))
                .thenReturn(
                        List.of("BRP_CATALOG"),
                        List.of()
                );

        assertTrue(
                authorizationService.hasAccess(1L, "BRP_CATALOG")
        );

        assertFalse(
                authorizationService.hasAccess(1L, "BRP_CATALOG")
        );

        verify(userModuleRepository, times(2))
                .findModuleCodesByUserId(1L);
    }
}
