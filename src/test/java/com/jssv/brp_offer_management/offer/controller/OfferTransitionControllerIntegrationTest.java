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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OfferTransitionControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwtService;

    @Test
    void shouldRejectAnonymousRequest() throws Exception {
        mvc.perform(post("/api/offers/1/take"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAnalystWithoutModule() throws Exception {
        Long analyst = user("ANALISTA");
        Long owner = user("RADICADOR");
        Long offer = offer(owner);
        call(offer, "take", analyst, "ANALISTA", List.of())
                .andExpect(status().isForbidden());
        assertEquals("RADICADO", state(offer));
    }

    @Test
    void shouldRejectRadicadorTakingOffer() throws Exception {
        Long owner = user("RADICADOR");
        Long offer = offer(owner);
        call(offer, "take", owner, "RADICADOR", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isForbidden());
        assertEquals("RADICADO", state(offer));
    }

    @Test
    void shouldTakeEscalateAndApproveWithCorrectRoles() throws Exception {
        Long owner = user("RADICADOR");
        Long analyst = user("ANALISTA");
        Long chief = user("JEFE");
        Long offer = offer(owner);
        assign(analyst, "OFFER_MANAGEMENT");
        assign(chief, "OFFER_APPROVAL");

        call(offer, "take", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
        call(offer, "escalate", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ESCALADO"));
        call(offer, "approve", chief, "JEFE", List.of("OFFER_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        assertEquals("APROBADO", state(offer));
        assertEquals(analyst, jdbc.queryForObject(
                "SELECT analista_id FROM oferta WHERE id = ?", Long.class, offer));
        assertNotNull(jdbc.queryForObject(
                "SELECT fecha_finalizacion FROM oferta WHERE id = ?", Object.class, offer));
    }

    @Test
    void shouldReturnToSameAnalystAndAllowReescalation() throws Exception {
        Long owner = user("RADICADOR");
        Long analyst = user("ANALISTA");
        Long chief = user("JEFE");
        Long offer = offer(owner);
        assign(analyst, "OFFER_MANAGEMENT");
        assign(chief, "OFFER_APPROVAL");
        call(offer, "take", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isOk());
        call(offer, "escalate", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isOk());
        call(offer, "return", chief, "JEFE", List.of("OFFER_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
        assertEquals(analyst, jdbc.queryForObject(
                "SELECT analista_id FROM oferta WHERE id = ?", Long.class, offer));
        assertNotNull(jdbc.queryForObject(
                "SELECT fecha_devolucion FROM oferta WHERE id = ?", Object.class, offer));
        call(offer, "escalate", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isOk());
        assertEquals("ESCALADO", state(offer));
    }

    @Test
    void shouldReturn409ForInvalidTransition() throws Exception {
        Long owner = user("RADICADOR");
        Long analyst = user("ANALISTA");
        Long offer = offer(owner);
        assign(analyst, "OFFER_MANAGEMENT");
        call(offer, "escalate", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isConflict());
        assertEquals("RADICADO", state(offer));
    }

    @Test
    void shouldRejectOtherAnalystManagingAssignedOffer() throws Exception {
        Long owner = user("RADICADOR");
        Long analyst = user("ANALISTA");
        Long otherAnalyst = user("ANALISTA");
        Long offer = offer(owner);
        assign(analyst, "OFFER_MANAGEMENT");
        assign(otherAnalyst, "OFFER_MANAGEMENT");
        call(offer, "take", analyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isOk());
        call(offer, "escalate", otherAnalyst, "ANALISTA", List.of("OFFER_MANAGEMENT"))
                .andExpect(status().isForbidden());
        assertEquals("EN_PROCESO", state(offer));
    }

    private org.springframework.test.web.servlet.ResultActions call(
            Long offer, String action, Long id, String role, List<String> modules) throws Exception {
        String email = jdbc.queryForObject(
                "SELECT email FROM usuario WHERE id = ?", String.class, id);
        String token = jwtService.generateToken(id, email, role, modules);
        return mvc.perform(post("/api/offers/" + offer + "/" + action)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private Long user(String role) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (nombre, email, password_hash, activo, rol_id)
                VALUES (?, ?, ?, TRUE, (SELECT id FROM rol WHERE nombre = ?))
                RETURNING id
                """, Long.class, "Test flow", "flow-" + UUID.randomUUID() + "@brp.local", "test-only", role);
    }

    private void assign(Long user, String module) {
        jdbc.update("""
                INSERT INTO usuario_modulo (usuario_id, modulo_id)
                VALUES (?, (SELECT id FROM modulo WHERE codigo = ?))
                """, user, module);
    }

    private Long offer(Long owner) {
        Long brp = jdbc.queryForObject("SELECT id FROM brp ORDER BY id LIMIT 1", Long.class);
        assertNotNull(brp);
        return jdbc.queryForObject("""
                INSERT INTO oferta (brp_id, valor_oferta, porcentaje_descuento,
                                    estado, radicador_id, fecha_radicacion, version)
                VALUES (?, 180000000.00, 10.0000, 'RADICADO', ?, CURRENT_TIMESTAMP, 0)
                RETURNING id
                """, Long.class, brp, owner);
    }

    private String state(Long offer) {
        return jdbc.queryForObject("SELECT estado FROM oferta WHERE id = ?", String.class, offer);
    }
}
