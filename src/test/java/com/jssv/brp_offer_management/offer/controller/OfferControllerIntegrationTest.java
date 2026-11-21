package com.jssv.brp_offer_management.offer.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OfferControllerIntegrationTest {

    private static final String ENDPOINT = "/api/offers";
    private static final String MODULE = "OFFER_REGISTRATION";
    private static final String VALID_REQUEST = """
        {
          "brpId": 1,
          "valorOferta": 180000000.00,
          "oferentes": [
            {
              "tipoIdentificacion": "CC",
              "numeroIdentificacion": "123456789",
              "nombre": "Oferente de prueba",
              "porcentajeParticipacion": 100.0000,
              "email": "oferente@example.com",
              "telefono": "3001234567",
              "pagos": [
                {
                  "tipoPago": "EFECTIVO",
                  "valor": 180000000.00,
                  "fechaPago": "2026-11-21",
                  "entidadFinanciera": null
                }
              ]
            }
          ]
        }
        """;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(
                post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForWrongRole() throws Exception {
        Long userId = createUser("ANALISTA", true);
        assignModule(userId);

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(userId, "ANALISTA")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
        )
                .andDo(print()).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WithoutRegistrationModule()
            throws Exception {

        Long userId = createUser("RADICADOR", true);

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(userId, "RADICADOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
        ).andDo(print()).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRevokedPermissionWithExistingJwt()
            throws Exception {

        Long userId = createUser("RADICADOR", true);
        assignModule(userId);

        String jwt = token(userId, "RADICADOR");

        revokeModule(userId);

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwt
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
        ).andDo(print()).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForInactiveUser() throws Exception {
        Long userId = createUser("RADICADOR", true);
        assignModule(userId);

        String jwt = token(userId, "RADICADOR");

        jdbcTemplate.update(
                "UPDATE usuario SET activo = FALSE WHERE id = ?",
                userId
        );

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwt
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
        ).andDo(print()).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        Long userId = createUser("RADICADOR", true);
        assignModule(userId);

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(userId, "RADICADOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brpId": null,
                                  "valorOferta": -100,
                                  "oferentes": []
                                }
                                """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateOfferAndPersistRelationships()
            throws Exception {

        Long userId = createUser("RADICADOR", true);
        assignModule(userId);

        Long brpId = prepareAvailableBrp();

        String request = """
                {
                  "brpId": %d,
                  "valorOferta": 180000000.00,
                  "oferentes": [
                    {
                      "tipoIdentificacion": "CC",
                      "numeroIdentificacion": "123456789",
                      "nombre": "Oferente de integración",
                      "porcentajeParticipacion": 100.0000,
                      "email": "oferente@example.com",
                      "telefono": "3001234567",
                      "pagos": [
                        {
                          "tipoPago": "EFECTIVO",
                          "valor": 180000000.00,
                          "fechaPago": "2026-11-21",
                          "entidadFinanciera": null
                        }
                      ]
                    }
                  ]
                }
                """.formatted(brpId);

        String response = mockMvc.perform(
                        post(ENDPOINT)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token(userId, "RADICADOR")
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RADICADO"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        long offerId = json.get("id").asLong();

        assertTrue(offerId > 0);

        Long savedRadicadorId = jdbcTemplate.queryForObject(
                "SELECT radicador_id FROM oferta WHERE id = ?",
                Long.class,
                offerId
        );

        assertEquals(userId, savedRadicadorId);

        BigDecimal discount = jdbcTemplate.queryForObject(
                "SELECT porcentaje_descuento FROM oferta WHERE id = ?",
                BigDecimal.class,
                offerId
        );

        assertEquals(
                0,
                new BigDecimal("10.0000").compareTo(discount)
        );

        Integer applicantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oferente WHERE oferta_id = ?",
                Integer.class,
                offerId
        );

        Integer paymentCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pago p
                JOIN oferente o ON o.id = p.oferente_id
                WHERE o.oferta_id = ?
                """,
                Integer.class,
                offerId
        );

        assertEquals(1, applicantCount);
        assertEquals(1, paymentCount);
    }

    private Long prepareAvailableBrp() {
        Long brpId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM brp
                ORDER BY fecha_ingesta DESC, id DESC
                LIMIT 1
                """,
                Long.class
        );

        assertNotNull(brpId);

        jdbcTemplate.update(
                """
                UPDATE brp
                SET estado = 'DISPONIBLE',
                    precio = 200000000.00
                WHERE id = ?
                """,
                brpId
        );

        return brpId;
    }

    private Long createUser(String role, boolean active) {
        String email = "offer-test-"
                + UUID.randomUUID()
                + "@brp.local";

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO usuario
                    (nombre, email, password_hash, activo, rol_id)
                VALUES (
                    ?, ?, ?, ?,
                    (SELECT id FROM rol WHERE nombre = ?)
                )
                RETURNING id
                """,
                Long.class,
                "Usuario de integración",
                email,
                "not-used-in-test",
                active,
                role
        );
    }

    private void assignModule(Long userId) {
        jdbcTemplate.update(
                """
                INSERT INTO usuario_modulo (usuario_id, modulo_id)
                VALUES (
                    ?,
                    (SELECT id FROM modulo WHERE codigo = ?)
                )
                """,
                userId,
                MODULE
        );
    }

    private void revokeModule(Long userId) {
        jdbcTemplate.update(
                """
                DELETE FROM usuario_modulo
                WHERE usuario_id = ?
                  AND modulo_id = (
                      SELECT id FROM modulo WHERE codigo = ?
                  )
                """,
                userId,
                MODULE
        );
    }

    private String token(Long userId, String role) {
        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM usuario WHERE id = ?",
                String.class,
                userId
        );

        return jwtService.generateToken(
                userId,
                email,
                role,
                List.of(MODULE)
        );
    }


    @Test
    void shouldReturn404WhenBrpDoesNotExist() throws Exception {

        Long userId = createUser("RADICADOR", true);
        assignModule(userId);

        String request = VALID_REQUEST.replace(
                "\"brpId\": 1",
                "\"brpId\": 999999999"
        );

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(userId, "RADICADOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(status().isNotFound());
    }


    @Test
    void shouldReturn409WhenBrpIsNotAvailable() throws Exception {

        Long userId = createUser("RADICADOR", true);
        assignModule(userId);

        Long brpId = prepareAvailableBrp();

        jdbcTemplate.update(
                """
                UPDATE brp
                SET estado = 'NO_DISPONIBLE'
                WHERE id = ?
                """,
                brpId
        );

        String request = VALID_REQUEST.replace(
                "\"brpId\": 1",
                "\"brpId\": " + brpId
        );

        mockMvc.perform(
                post(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(userId, "RADICADOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(status().isConflict());
    }

}
