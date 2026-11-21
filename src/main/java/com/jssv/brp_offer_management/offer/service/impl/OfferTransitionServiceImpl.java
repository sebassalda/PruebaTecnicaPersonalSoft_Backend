package com.jssv.brp_offer_management.offer.service.impl;

import com.jssv.brp_offer_management.offer.dto.response.OfferTransitionResponse;
import com.jssv.brp_offer_management.offer.entity.OfferEntity;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import com.jssv.brp_offer_management.offer.service.OfferTransitionService;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class OfferTransitionServiceImpl implements OfferTransitionService {
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OfferTransitionResponse transition(Long offerId, String action, Long userId, String role) {
        if (userId == null || userId <= 0 || role == null) {
            throw new AccessDeniedException("Usuario no autorizado");
        }
        if (offerId == null || offerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido");
        }
        String normalized = action == null ? "" : action.toUpperCase(java.util.Locale.ROOT);
        boolean analystAction = switch (normalized) {
            case "TAKE", "ESCALATE" -> true;
            case "REJECT" -> "ANALISTA".equals(role);
            default -> false;
        };
        boolean managerAction = switch (normalized) {
            case "APPROVE", "RETURN" -> true;
            case "REJECT" -> "JEFE".equals(role);
            default -> false;
        };
        if (!analystAction && !managerAction) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acción no reconocida para el rol");
        }
        if ((analystAction && !"ANALISTA".equals(role)) ||
                (managerAction && !"JEFE".equals(role))) {
            throw new AccessDeniedException("El rol no puede realizar esta transición");
        }
        OfferEntity offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (analystAction) {
            if (offer.getRadicador().getId().equals(userId)) {
                throw new AccessDeniedException("El radicador no puede analizar su oferta");
            }
            if ("TAKE".equals(normalized)) {
                requireState(offer, "RADICADO");
                UserEntity analyst = userRepository.findById(userId)
                        .orElseThrow(() -> new AccessDeniedException("Analista no encontrado"));
                if (!analyst.isActivo() || analyst.getRol() == null ||
                        !"ANALISTA".equals(analyst.getRol().getNombre())) {
                    throw new AccessDeniedException("Usuario sin rol de analista activo");
                }
                offer.setAnalista(analyst);
                offer.setEstado("EN_PROCESO");
            } else {
                requireState(offer, "EN_PROCESO");
                requireAssignedAnalyst(offer, userId);
                if ("ESCALATE".equals(normalized)) {
                    offer.setEstado("ESCALADO");
                    offer.setFechaEscalamiento(now);
                } else {
                    offer.setEstado("RECHAZADO");
                    offer.setFechaFinalizacion(now);
                }
            }
        } else {
            requireState(offer, "ESCALADO");
            if (offer.getAnalista() != null && offer.getAnalista().getId().equals(userId)) {
                throw new AccessDeniedException("El analista no puede aprobar su propia oferta");
            }
            switch (normalized) {
                case "APPROVE" -> {
                    offer.setEstado("APROBADO");
                    offer.setFechaFinalizacion(now);
                }
                case "REJECT" -> {
                    offer.setEstado("RECHAZADO");
                    offer.setFechaFinalizacion(now);
                }
                case "RETURN" -> {
                    if (offer.getAnalista() == null) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "La oferta no tiene analista asignado");
                    }
                    offer.setEstado("EN_PROCESO");
                    offer.setFechaDevolucion(now);
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acción desconocida");
            }
        }
        offer.setFechaActualizacion(now);
        // saveAndFlush fuerza la comprobación @Version dentro de esta transacción.
        OfferEntity updated = offerRepository.saveAndFlush(offer);
        return new OfferTransitionResponse(updated.getId(), updated.getEstado(),
                updated.getAnalista() == null ? null : updated.getAnalista().getId(),
                updated.getVersion(), updated.getFechaActualizacion(),
                updated.getFechaEscalamiento(), updated.getFechaDevolucion(),
                updated.getFechaFinalizacion());
    }

    private void requireState(OfferEntity offer, String expected) {
        if (!expected.equals(offer.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición inválida desde " + offer.getEstado() + "; se esperaba " + expected);
        }
    }

    private void requireAssignedAnalyst(OfferEntity offer, Long userId) {
        if (offer.getAnalista() == null || !userId.equals(offer.getAnalista().getId())) {
            throw new AccessDeniedException("Solo el analista asignado puede gestionar esta oferta");
        }
    }
}
