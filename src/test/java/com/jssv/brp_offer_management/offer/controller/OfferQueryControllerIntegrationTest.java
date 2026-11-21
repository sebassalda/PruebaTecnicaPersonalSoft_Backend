
package com.jssv.brp_offer_management.offer.controller;

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
class OfferQueryControllerIntegrationTest {

    private static final String ENDPOINT = "/api/offers";
    private static final String MODULE = "OFFER_TRACKING";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WithoutTrackingModule()
            throws Exception {

        Long userId = createUser("RADICADOR");

        mockMvc.perform(
                get(ENDPOINT)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(userId, "RADICADOR")
                        )
        ).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOnlyOwnOffersForRadicador()
            throws Exception {

        Long ownerId = createUser("RADICADOR");
        Long otherId = createUser("RADICADOR");

        assignTracking(ownerId);

        Long ownOfferId = createOffer(ownerId);
        Long otherOfferId = createOffer(otherId);

        String response = mockMvc.perform(
                        get(ENDPOINT)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token(ownerId, "RADICADOR")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownOfferId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(response.contains(
                "\"id\":" + otherOfferId + ","
        ));
    }

    @Test
    void shouldReturn404ForAnotherRadicadorOffer()
            throws Exception {

        Long ownerId = createUser("RADICADOR");
        Long otherId = createUser("RADICADOR");

        assignTracking(otherId);

        Long offerId = createOffer(ownerId);

        var result = mockMvc.perform(
                get(ENDPOINT + "/" + offerId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(otherId, "RADICADOR")
                        )
        ).andReturn();

        Exception exception = result.getResolvedException();

        if (exception != null) {
            System.err.println("=== EXCEPCION ORIGINAL ===");
            exception.printStackTrace(System.err);
        }

        assertEquals(
                404,
                result.getResponse().getStatus(),
                "Un RADICADOR no debe consultar ofertas ajenas"
        );
    }



    private Long createUser(String role) {

        String email = "offer-query-"
                + UUID.randomUUID()
                + "@brp.local";

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO usuario
                    (nombre, email, password_hash, activo, rol_id)
                VALUES (
                    ?, ?, ?, TRUE,
                    (SELECT id FROM rol WHERE nombre = ?)
                )
                RETURNING id
                """,
                Long.class,
                "Usuario de prueba",
                email,
                "not-used-in-test",
                role
        );
    }

    private void assignTracking(Long userId) {

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

    private Long createOffer(Long radicadorId) {

        Long brpId = jdbcTemplate.queryForObject(
                "SELECT id FROM brp ORDER BY id LIMIT 1",
                Long.class
        );

        assertNotNull(brpId);

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO oferta (
                    brp_id,
                    valor_oferta,
                    porcentaje_descuento,
                    estado,
                    radicador_id,
                    fecha_radicacion,
                    version
                )
                VALUES (
                    ?, 180000000.00, 10.0000,
                    'RADICADO', ?, CURRENT_TIMESTAMP, 0
                )
                RETURNING id
                """,
                Long.class,
                brpId,
                radicadorId
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
}
