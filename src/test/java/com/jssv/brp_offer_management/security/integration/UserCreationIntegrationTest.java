package com.jssv.brp_offer_management.security.integration;

import tools.jackson.databind.ObjectMapper;
import com.jssv.brp_offer_management.security.jwt.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserCreationIntegrationTest {

    private static final String URL = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateUserWithEncodedPasswordAndNoModules()
            throws Exception {

        String token = authorizedToken();
        String email = uniqueEmail();
        String password = "ClaveSegura#2026";

        String body = createBody(
                "María González",
                email,
                password,
                "ANALISTA"
        );

        String response = mockMvc.perform(
                        post(URL)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("María González"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ANALISTA"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.modules").isEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long createdId = objectMapper.readTree(response)
                .get("id")
                .longValue();

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM usuario WHERE id = ?",
                String.class,
                createdId
        );

        assertNotNull(storedHash);
        assertNotEquals(password, storedHash);
        assertTrue(passwordEncoder.matches(password, storedHash));

        Integer moduleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario_modulo WHERE usuario_id = ?",
                Integer.class,
                createdId
        );

        assertEquals(0, moduleCount);
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        String token = authorizedToken();
        String email = uniqueEmail();

        jdbcTemplate.update(
                """
                INSERT INTO usuario
                    (nombre, email, password_hash, activo, rol_id)
                VALUES (
                    ?, ?, ?, TRUE,
                    (SELECT id FROM rol WHERE nombre = 'ANALISTA')
                )
                """,
                "Usuario existente",
                email,
                "hash-test"
        );

        mockMvc.perform(
                post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                "Usuario duplicado",
                                email.toUpperCase(),
                                "ClaveSegura#2026",
                                "ANALISTA"
                        ))
        ).andExpect(status().isConflict());
    }

    @Test
    void shouldRejectUnknownRole() throws Exception {

        String token = authorizedToken();

        mockMvc.perform(
                post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                "Usuario nuevo",
                                uniqueEmail(),
                                "ClaveSegura#2026",
                                "ROL_INEXISTENTE"
                        ))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {

        String token = authorizedToken();

        mockMvc.perform(
                        post(URL)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody(
                                        "",
                                        "correo-invalido",
                                        "123",
                                        ""
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {

        mockMvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                "Usuario nuevo",
                                uniqueEmail(),
                                "ClaveSegura#2026",
                                "ANALISTA"
                        ))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserWithoutManagementPermission()
            throws Exception {

        Long userId = createTestUser();

        String token = jwtService.generateToken(
                userId,
                getEmail(userId),
                "ANALISTA",
                List.of()
        );

        mockMvc.perform(
                post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(
                                "Usuario nuevo",
                                uniqueEmail(),
                                "ClaveSegura#2026",
                                "ANALISTA"
                        ))
        ).andExpect(status().isForbidden());
    }

    private String authorizedToken() {

        Long userId = createTestUser();
        jdbcTemplate.update("UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre = 'ADMINISTRADOR') WHERE id = ?", userId);

        jdbcTemplate.update(
                """
                INSERT INTO usuario_modulo
                    (usuario_id, modulo_id)
                VALUES (
                    ?,
                    (SELECT id FROM modulo
                     WHERE codigo = 'USER_MANAGEMENT')
                )
                """,
                userId
        );

        return jwtService.generateToken(
                userId,
                getEmail(userId),
                "ADMINISTRADOR",
                List.of("USER_MANAGEMENT")
        );
    }

    private Long createTestUser() {

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
                uniqueEmail(),
                "hash-test"
        );
    }

    private String getEmail(Long userId) {

        return jdbcTemplate.queryForObject(
                "SELECT email FROM usuario WHERE id = ?",
                String.class,
                userId
        );
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@brp.local";
    }

    private String createBody(
            String nombre,
            String email,
            String password,
            String role
    ) throws Exception {

        return objectMapper.writeValueAsString(
                Map.of(
                        "nombre", nombre,
                        "email", email,
                        "password", password,
                        "role", role
                )
        );
    }
}
