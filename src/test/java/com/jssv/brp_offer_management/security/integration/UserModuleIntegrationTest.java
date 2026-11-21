
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
class UserModuleIntegrationTest {

    private static final String USERS_URL = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldAssignModules() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(
                targetId,
                token,
                """
                {"modules":["BRP_CATALOG","OFFER_REGISTRATION"]}
                """
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.modules.length()").value(2))
                .andExpect(jsonPath("$.modules[0]").value("BRP_CATALOG"))
                .andExpect(jsonPath("$.modules[1]").value("OFFER_REGISTRATION"));

        assertEquals(
                List.of("BRP_CATALOG", "OFFER_REGISTRATION"),
                findModules(targetId)
        );
    }

    @Test
    void shouldReplaceExistingModules() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        assignModule(targetId, "BRP_CATALOG");
        assignModule(targetId, "OFFER_REGISTRATION");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(
                targetId,
                token,
                """
                {"modules":["OFFER_APPROVAL"]}
                """
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(1))
                .andExpect(jsonPath("$.modules[0]").value("OFFER_APPROVAL"));

        assertEquals(
                List.of("OFFER_APPROVAL"),
                findModules(targetId)
        );
    }

    @Test
    void shouldRevokeAllModules() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        assignModule(targetId, "BRP_CATALOG");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(targetId, token, """
                {"modules":[]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(0));

        assertTrue(findModules(targetId).isEmpty());
    }

    @Test
    void shouldRejectUnknownModuleWithoutChangingPermissions()
            throws Exception {

        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        assignModule(targetId, "BRP_CATALOG");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(
                targetId,
                token,
                """
                {"modules":["OFFER_APPROVAL","UNKNOWN_MODULE"]}
                """
        )
                .andExpect(status().isBadRequest());

        assertEquals(
                List.of("BRP_CATALOG"),
                findModules(targetId)
        );
    }

    @Test
    void shouldRejectDuplicateModules() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(
                targetId,
                token,
                """
                {"modules":["BRP_CATALOG","brp_catalog"]}
                """
        )
                .andExpect(status().isBadRequest());

        assertTrue(findModules(targetId).isEmpty());
    }

    @Test
    void shouldRejectNullModules() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(targetId, token, """
                {"modules":null}
                """)
                .andExpect(status().isBadRequest());

        assertTrue(findModules(targetId).isEmpty());
    }

    @Test
    void shouldReturn404ForMissingUser() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(
                999999999L,
                token,
                """
                {"modules":["BRP_CATALOG"]}
                """
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectSelfRevocation() throws Exception {
        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        String token = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        updateModules(
                adminId,
                token,
                """
                {"modules":["BRP_CATALOG"]}
                """
        )
                .andExpect(status().isConflict());

        assertEquals(
                List.of("USER_MANAGEMENT"),
                findModules(adminId)
        );
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {
        Long targetId = createUser();

        mockMvc.perform(
                        put(USERS_URL + "/" + targetId + "/modules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"modules":["BRP_CATALOG"]}
                                """)
                )
                .andExpect(status().isUnauthorized());

        assertTrue(findModules(targetId).isEmpty());
    }

    @Test
    void shouldRejectUserWithoutManagementPermission()
            throws Exception {

        Long operatorId = createUser();
        Long targetId = createUser();

        String token = generateToken(operatorId, List.of());

        updateModules(
                targetId,
                token,
                """
                {"modules":["BRP_CATALOG"]}
                """
        )
                .andExpect(status().isForbidden());

        assertTrue(findModules(targetId).isEmpty());
    }

    @Test
    void shouldRejectExistingJwtAfterModuleRevocation()
            throws Exception {

        Long adminId = createUser();
        assignModule(adminId, "USER_MANAGEMENT");

        Long targetId = createUser();
        assignModule(targetId, "BRP_CATALOG");

        String adminToken = generateToken(
                adminId, List.of("USER_MANAGEMENT")
        );

        String targetToken = generateToken(
                targetId, List.of("BRP_CATALOG")
        );

        // El usuario tiene acceso inicialmente.
        mockMvc.perform(
                get("/api/brps")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + targetToken
                        )
        ).andExpect(status().isOk());

        // El administrador revoca el módulo mediante el endpoint.
        updateModules(
                targetId,
                adminToken,
                """
                {"modules":[]}
                """
        )
                .andExpect(status().isOk());

        // Se reutiliza exactamente el mismo JWT.
        mockMvc.perform(
                get("/api/brps")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + targetToken
                        )
        ).andExpect(status().isForbidden());

        assertTrue(findModules(targetId).isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions
    updateModules(
            Long userId,
            String token,
            String json
    ) throws Exception {

        return mockMvc.perform(
                put(USERS_URL + "/" + userId + "/modules")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        );
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
                "modules-" + UUID.randomUUID() + "@brp.local",
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

    private List<String> findModules(Long userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT m.codigo
                FROM usuario_modulo um
                JOIN modulo m ON m.id = um.modulo_id
                WHERE um.usuario_id = ?
                ORDER BY m.codigo
                """,
                String.class,
                userId
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

        return jwtService.generateToken(
                userId,
                email,
                "ANALISTA",
                modules
        );
    }
}
