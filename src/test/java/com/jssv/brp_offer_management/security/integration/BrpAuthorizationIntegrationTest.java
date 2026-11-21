package com.jssv.brp_offer_management.security.integration;

import com.jssv.brp_offer_management.security.jwt.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrpAuthorizationIntegrationTest {

    private static final String BRP_ENDPOINT =
            "/api/brps";

    private static final String BRP_MODULE =
            "BRP_CATALOG";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(BRP_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowUserWithBrpModule() throws Exception {
        Long userId = createTestUser(true);

        assignBrpModule(userId);

        String token = generateToken(userId);

        mockMvc.perform(
                get(BRP_ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isOk());
    }

    @Test
    void shouldDenyUserWithoutBrpModule() throws Exception {
        Long userId = createTestUser(true);

        String token = generateToken(userId);

        mockMvc.perform(
                get(BRP_ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessAfterPermissionRevocation()
            throws Exception {

        Long userId = createTestUser(true);

        assignBrpModule(userId);

        String token = generateToken(userId);

        mockMvc.perform(
                get(BRP_ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isOk());

        revokeBrpModule(userId);

        // Reutilizamos exactamente el mismo JWT.
        mockMvc.perform(
                get(BRP_ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyInactiveUser() throws Exception {
        Long userId = createTestUser(true);

        assignBrpModule(userId);

        String token = generateToken(userId);

        jdbcTemplate.update(
                "UPDATE usuario SET activo = FALSE WHERE id = ?",
                userId
        );

        mockMvc.perform(
                get(BRP_ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    private Long createTestUser(boolean active) {

        String email = "integration-"
                + UUID.randomUUID()
                + "@brp.local";

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO usuario
                    (nombre, email, password_hash, activo, rol_id)
                VALUES
                    (
                        ?,
                        ?,
                        ?,
                        ?,
                        (SELECT id FROM rol WHERE nombre = ?)
                    )
                RETURNING id
                """,
                Long.class,
                "Usuario de integración",
                email,
                "not-used-in-this-test",
                active,
                "ANALISTA"
        );
    }

    private void assignBrpModule(Long userId) {
        jdbcTemplate.update(
                """
                INSERT INTO usuario_modulo
                    (usuario_id, modulo_id)
                VALUES (
                    ?,
                    (SELECT id FROM modulo WHERE codigo = ?)
                )
                """,
                userId,
                BRP_MODULE
        );
    }

    private void revokeBrpModule(Long userId) {
        jdbcTemplate.update(
                """
                DELETE FROM usuario_modulo
                WHERE usuario_id = ?
                  AND modulo_id = (
                      SELECT id FROM modulo WHERE codigo = ?
                  )
                """,
                userId,
                BRP_MODULE
        );
    }

    private String generateToken(Long userId) {

        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM usuario WHERE id = ?",
                String.class,
                userId
        );

        return jwtService.generateToken(
                userId,
                email,
                "ANALISTA",
                List.of(BRP_MODULE)
        );
    }
}
