package com.jssv.brp_offer_management.offer.service.impl;

import com.jssv.brp_offer_management.offer.entity.OfferEntity;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferTransitionServiceImplTest {
    @Mock OfferRepository offerRepository;
    @Mock UserRepository userRepository;
    @InjectMocks OfferTransitionServiceImpl service;
    OfferEntity offer;
    UserEntity analyst;

    @BeforeEach void setUp() {
        UserEntity creator = new UserEntity(); creator.setId(6L);
        RoleEntity analystRole = new RoleEntity(); analystRole.setNombre("ANALISTA");
        analyst = new UserEntity(); analyst.setId(7L); analyst.setActivo(true); analyst.setRol(analystRole);
        offer = new OfferEntity(); offer.setId(1L); offer.setEstado("RADICADO");
        offer.setRadicador(creator); offer.setVersion(0L);
    }
    private void found() { when(offerRepository.findById(1L)).thenReturn(Optional.of(offer)); }
    private void saved() { when(offerRepository.saveAndFlush(offer)).thenReturn(offer); }

    @Test void takeAssignsAnalyst() {
        found(); saved(); when(userRepository.findById(7L)).thenReturn(Optional.of(analyst));
        var result = service.transition(1L,"TAKE",7L,"ANALISTA");
        assertEquals("EN_PROCESO",result.estado()); assertEquals(7L,result.analistaId());
        assertNotNull(result.fechaActualizacion());
    }
    @Test void escalateAssignedAnalyst() {
        offer.setEstado("EN_PROCESO"); offer.setAnalista(analyst); found(); saved();
        var result = service.transition(1L,"ESCALATE",7L,"ANALISTA");
        assertEquals("ESCALADO",result.estado()); assertNotNull(result.fechaEscalamiento());
    }
    @Test void analystRejects() {
        offer.setEstado("EN_PROCESO"); offer.setAnalista(analyst); found(); saved();
        var result = service.transition(1L,"REJECT",7L,"ANALISTA");
        assertEquals("RECHAZADO",result.estado()); assertNotNull(result.fechaFinalizacion());
    }
    @Test void managerApproves() {
        offer.setEstado("ESCALADO"); offer.setAnalista(analyst); found(); saved();
        var result = service.transition(1L,"APPROVE",8L,"JEFE");
        assertEquals("APROBADO",result.estado()); assertNotNull(result.fechaFinalizacion());
    }
    @Test void managerRejects() {
        offer.setEstado("ESCALADO"); offer.setAnalista(analyst); found(); saved();
        var result = service.transition(1L,"REJECT",8L,"JEFE");
        assertEquals("RECHAZADO",result.estado()); assertNotNull(result.fechaFinalizacion());
    }
    @Test void managerReturnsToSameAnalyst() {
        offer.setEstado("ESCALADO"); offer.setAnalista(analyst); found(); saved();
        var result = service.transition(1L,"RETURN",8L,"JEFE");
        assertEquals("EN_PROCESO",result.estado()); assertEquals(7L,result.analistaId());
        assertNotNull(result.fechaDevolucion());
    }
    @Test void invalidStateIsConflict() {
        found();
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.transition(1L,"APPROVE",8L,"JEFE"));
        assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());
        verify(offerRepository,never()).saveAndFlush(any());
    }
    @Test void creatorCannotTakeOwnOffer() {
        found(); assertThrows(AccessDeniedException.class,
                () -> service.transition(1L,"TAKE",6L,"ANALISTA"));
    }
    @Test void anotherAnalystCannotEscalate() {
        offer.setEstado("EN_PROCESO"); offer.setAnalista(analyst); found();
        assertThrows(AccessDeniedException.class,
                () -> service.transition(1L,"ESCALATE",9L,"ANALISTA"));
    }
    @Test void missingOfferIsNotFound() {
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());
        var ex=assertThrows(ResponseStatusException.class,
                () -> service.transition(1L,"TAKE",7L,"ANALISTA"));
        assertEquals(HttpStatus.NOT_FOUND,ex.getStatusCode());
    }
}
