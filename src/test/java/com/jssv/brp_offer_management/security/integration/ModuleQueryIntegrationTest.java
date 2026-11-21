
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModuleQueryIntegrationTest {

    private static final String MODULES_URL = "/api/modules";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldReturnModulesForAuthorizedUser() throws Exception {

        Long userId = createUser();
        assignModule(userId, "USER_MANAGEMENT");

        String token = generateToken(
                userId,
                List.of("USER_MANAGEMENT")
        );

        mockMvc.perform(
                        get(MODULES_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        org.springframework.http.MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].codigo").value("BRP_CATALOG"))
                .andExpect(jsonPath("$[1].codigo").value("OFFER_APPROVAL"))
                .andExpect(jsonPath("$[2].codigo").value("OFFER_MANAGEMENT"))
                .andExpect(jsonPath("$[3].codigo").value("OFFER_REGISTRATION"))
                .andExpect(jsonPath("$[4].codigo").value("OFFER_TRACKING"))
                .andExpect(jsonPath("$[5].codigo").value("USER_MANAGEMENT"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].nombre").isString());
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {

        mockMvc.perform(get(MODULES_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserWithoutPermission() throws Exception {

        Long userId = createUser();

        String token = generateToken(userId, List.of());

        mockMvc.perform(
                        get(MODULES_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInactiveUser() throws Exception {

        Long userId = createUser();
        assignModule(userId, "USER_MANAGEMENT");

        String token = generateToken(
                userId,
                List.of("USER_MANAGEMENT")
        );

        jdbcTemplate.update(
                "UPDATE usuario SET activo = FALSE WHERE id = ?",
                userId
        );

        mockMvc.perform(
                        get(MODULES_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnModulesOrderedByCode() throws Exception {

        Long userId = createUser();
        assignModule(userId, "USER_MANAGEMENT");

        String token = generateToken(
                userId,
                List.of("USER_MANAGEMENT")
        );

        String response = mockMvc.perform(
                        get(MODULES_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int catalog = response.indexOf("BRP_CATALOG");
        int approval = response.indexOf("OFFER_APPROVAL");
        int management = response.indexOf("OFFER_MANAGEMENT");
        int registration = response.indexOf("OFFER_REGISTRATION");
        int users = response.indexOf("USER_MANAGEMENT");

        assertTrue(catalog >= 0);
        assertTrue(catalog < approval);
        assertTrue(approval < management);
        assertTrue(management < registration);
        assertTrue(registration < users);
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
                "Usuario prueba modulos",
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
