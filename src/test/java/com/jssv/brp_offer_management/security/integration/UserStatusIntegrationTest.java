package com.jssv.brp_offer_management.security.integration;

import com.jssv.brp_offer_management.security.jwt.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserStatusIntegrationTest {

    private static final String USERS_URL = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldDeactivateUser() throws Exception {
        Long adminId = createAdminUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        patch(USERS_URL + "/" + targetId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.activo").value(false));

        assertFalse(isActive(targetId));
    }

    @Test
    void shouldReactivateUser() throws Exception {
        Long adminId = createAdminUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        jdbcTemplate.update(
                "UPDATE usuario SET activo = FALSE WHERE id = ?",
                targetId
        );

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        patch(USERS_URL + "/" + targetId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":true}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        assertTrue(isActive(targetId));
    }

    @Test
    void shouldRejectSelfDeactivation() throws Exception {
        Long adminId = createAdminUser();
        assignModule(adminId, "USER_MANAGEMENT");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        patch(USERS_URL + "/" + adminId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}")
                )
                .andExpect(status().isConflict());

        assertTrue(isActive(adminId));
    }

    @Test
    void shouldReturn404ForMissingUser() throws Exception {
        Long adminId = createAdminUser();
        assignModule(adminId, "USER_MANAGEMENT");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        patch(USERS_URL + "/999999999/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectNullStatus() throws Exception {
        Long adminId = createAdminUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        patch(USERS_URL + "/" + targetId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":null}")
                )
                .andExpect(status().isBadRequest());

        assertTrue(isActive(targetId));
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {
        Long targetId = createUser();

        mockMvc.perform(
                        patch(USERS_URL + "/" + targetId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}")
                )
                .andExpect(status().isUnauthorized());

        assertTrue(isActive(targetId));
    }

    @Test
    void shouldRejectUserWithoutManagementPermission()
            throws Exception {

        Long operatorId = createUser();
        Long targetId = createUser();

        String token = generateToken(operatorId, List.of());

        mockMvc.perform(
                        patch(USERS_URL + "/" + targetId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}")
                )
                .andExpect(status().isForbidden());

        assertTrue(isActive(targetId));
    }

    @Test
    void shouldRejectExistingJwtAfterDeactivation()
            throws Exception {

        Long adminId = createAdminUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        assignModule(targetId, "BRP_CATALOG");

        String adminToken = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        String targetToken = generateToken(
                targetId, List.of("BRP_CATALOG")
        );

        // El usuario tiene acceso antes de desactivarse.
        mockMvc.perform(
                get("/api/brps")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + targetToken
                        )
        ).andExpect(status().isOk());

        // Otro usuario autorizado lo desactiva.
        mockMvc.perform(
                        patch(USERS_URL + "/" + targetId + "/status")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Se reutiliza exactamente el mismo JWT.
        mockMvc.perform(
                get("/api/brps")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + targetToken
                        )
        ).andExpect(status().isForbidden());

        assertFalse(isActive(targetId));
    }

    private Long createAdminUser() {
        Long id = createUser();
        jdbcTemplate.update(
                "UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre = 'ADMINISTRADOR') WHERE id = ?",
                id
        );
        return id;
    }

    private Long createUser() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO usuario
                    (nombre, email, password_hash, activo, rol_id)
                VALUES (
                    ?, ?, ?, TRUE,
                    (SELECT id FROM rol WHERE nombre = 'ANALISTA')
                )
                RETURNING id
                """,
                Long.class,
                "Usuario de integración",
                "status-" + UUID.randomUUID() + "@brp.local",
                "test-password-hash"
        );
    }

    private void assignModule(Long userId, String moduleCode) {
        jdbcTemplate.update(
                """
                INSERT INTO usuario_modulo (usuario_id, modulo_id)
                VALUES (
                    ?,
                    (SELECT id FROM modulo WHERE codigo = ?)
                )
                """,
                userId,
                moduleCode
        );
    }

    private String generateToken(
            Long userId,
            List<String> modules
    ) {
        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM usuario WHERE id = ?",
                String.class,
                userId
        );

        String role = jdbcTemplate.queryForObject(
                "SELECT r.nombre FROM usuario u JOIN rol r ON r.id = u.rol_id WHERE u.id = ?",
                String.class, userId
        );
        return jwtService.generateToken(
                userId,
                email,
                role,
                modules
        );
    }

    private boolean isActive(Long userId) {
        Boolean active = jdbcTemplate.queryForObject(
                "SELECT activo FROM usuario WHERE id = ?",
                Boolean.class,
                userId
        );

        return Boolean.TRUE.equals(active);
    }
}
