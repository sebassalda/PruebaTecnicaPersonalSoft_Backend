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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserQueryIntegrationTest {

    private static final String USERS_URL = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(USERS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WithoutUserManagementPermission()
            throws Exception {

        Long userId = createUser();
        String token = generateToken(userId, List.of());

        mockMvc.perform(
                get(USERS_URL)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WithUserManagementPermission()
            throws Exception {

        Long userId = createUser();
        assignModule(userId, "USER_MANAGEMENT");

        String token = generateToken(
                userId,
                List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        get(USERS_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void shouldReturnUserDetailsWithoutPassword()
            throws Exception {

        Long userId = createUser();
        assignModule(userId, "USER_MANAGEMENT");

        String token = generateToken(
                userId,
                List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        get(USERS_URL + "/" + userId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.role").value("ANALISTA"))
                .andExpect(jsonPath("$.modules",
                        hasItem("USER_MANAGEMENT")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn404ForMissingUser() throws Exception {
        Long userId = createUser();
        assignModule(userId, "USER_MANAGEMENT");

        String token = generateToken(
                userId,
                List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                get(USERS_URL + "/999999999")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isNotFound());
    }

    private Long createUser() {

        String email = "user-query-"
                + UUID.randomUUID()
                + "@brp.local";

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO usuario
                    (nombre, email, password_hash, activo, rol_id)
                VALUES (
                    ?,
                    ?,
                    ?,
                    TRUE,
                    (SELECT id FROM rol WHERE nombre = ?)
                )
                RETURNING id
                """,
                Long.class,
                "Usuario de integración",
                email,
                "test-password-hash",
                "ANALISTA"
        );
    }

    private void assignModule(Long userId, String moduleCode) {
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

        return jwtService.generateToken(
                userId,
                email,
                "ANALISTA",
                modules
        );
    }
}
