package com.jssv.brp_offer_management.security.repository;

import com.jssv.brp_offer_management.security.entity.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserModuleRepository userModuleRepository;

    @Test
    void shouldFindAllConfiguredRoles() {
        assertEquals(4, roleRepository.count());

        assertTrue(roleRepository
                .findByNombre("ADMINISTRADOR")
                .isPresent());

        assertTrue(roleRepository
                .findByNombre("RADICADOR")
                .isPresent());

        assertTrue(roleRepository
                .findByNombre("ANALISTA")
                .isPresent());

        assertTrue(roleRepository
                .findByNombre("JEFE")
                .isPresent());
    }

    @Test
    void shouldFindAllConfiguredModules() {
        assertEquals(6, moduleRepository.count());

        assertTrue(moduleRepository
                .findByCodigo("BRP_CATALOG")
                .isPresent());

        assertTrue(moduleRepository
                .findByCodigo("OFFER_REGISTRATION")
                .isPresent());

        assertTrue(moduleRepository
                .findByCodigo("OFFER_MANAGEMENT")
                .isPresent());

        assertTrue(moduleRepository
                .findByCodigo("OFFER_APPROVAL")
                .isPresent());

        assertTrue(moduleRepository
                .findByCodigo("USER_MANAGEMENT")
                .isPresent());

        assertTrue(moduleRepository
                .findByCodigo("OFFER_TRACKING")
                .isPresent());
    }

    @Test
    void shouldFindUserByEmailIgnoringCase() {

        UserEntity user = createTestUser();

        userRepository.saveAndFlush(user);

        UserEntity found = userRepository
                .findByEmailIgnoreCase("TEST.SECURITY@EXAMPLE.COM")
                .orElseThrow();

        assertEquals(user.getId(), found.getId());
        assertEquals("ADMINISTRADOR", found.getRol().getNombre());
    }

    @Test
    void shouldDetectExistingEmailIgnoringCase() {

        userRepository.saveAndFlush(createTestUser());

        assertTrue(userRepository.existsByEmailIgnoreCase(
                "TEST.SECURITY@EXAMPLE.COM"
        ));

        assertFalse(userRepository.existsByEmailIgnoreCase(
                "unknown@example.com"
        ));
    }

    @Test
    void shouldFindAssignedModulesByUserId() {

        UserEntity user = userRepository.saveAndFlush(
                createTestUser()
        );

        ModuleEntity catalog = moduleRepository
                .findByCodigo("BRP_CATALOG")
                .orElseThrow();

        ModuleEntity management = moduleRepository
                .findByCodigo("OFFER_MANAGEMENT")
                .orElseThrow();

        assignModule(user.getId(), catalog.getId());
        assignModule(user.getId(), management.getId());

        List<String> modules = userModuleRepository
                .findModuleCodesByUserId(user.getId());

        assertEquals(2, modules.size());
        assertTrue(modules.contains("BRP_CATALOG"));
        assertTrue(modules.contains("OFFER_MANAGEMENT"));
    }

    @Test
    void shouldDetectExistingUserModuleAssignment() {

        UserEntity user = userRepository.saveAndFlush(
                createTestUser()
        );

        ModuleEntity module = moduleRepository
                .findByCodigo("BRP_CATALOG")
                .orElseThrow();

        assignModule(user.getId(), module.getId());

        assertTrue(userModuleRepository
                .existsByUsuarioIdAndModuloId(
                        user.getId(),
                        module.getId()
                ));

        assertFalse(userModuleRepository
                .existsByUsuarioIdAndModuloId(
                        user.getId(),
                        9999L
                ));
    }

    @Test
    void shouldDeleteUserModuleAssignment() {

        UserEntity user = userRepository.saveAndFlush(
                createTestUser()
        );

        ModuleEntity module = moduleRepository
                .findByCodigo("BRP_CATALOG")
                .orElseThrow();

        assignModule(user.getId(), module.getId());

        userModuleRepository.deleteByUsuarioIdAndModuloId(
                user.getId(),
                module.getId()
        );

        userModuleRepository.flush();

        assertFalse(userModuleRepository
                .existsByUsuarioIdAndModuloId(
                        user.getId(),
                        module.getId()
                ));
    }

    private UserEntity createTestUser() {

        RoleEntity role = roleRepository
                .findByNombre("ADMINISTRADOR")
                .orElseThrow();

        UserEntity user = new UserEntity();

        user.setNombre("Usuario de prueba");
        user.setEmail("test.security@example.com");
        user.setPasswordHash("test-hash-not-for-authentication");
        user.setActivo(true);
        user.setRol(role);

        return user;
    }

    private void assignModule(Long userId, Long moduleId) {

        UserModuleEntity assignment = new UserModuleEntity();

        assignment.setUsuarioId(userId);
        assignment.setModuloId(moduleId);

        userModuleRepository.saveAndFlush(assignment);
    }
}
