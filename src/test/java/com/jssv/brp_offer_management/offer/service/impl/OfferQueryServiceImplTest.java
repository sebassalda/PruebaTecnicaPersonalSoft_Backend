
package com.jssv.brp_offer_management.offer.service.impl;

import com.jssv.brp_offer_management.brp.entity.BrpEntity;
import com.jssv.brp_offer_management.offer.dto.response.*;
import com.jssv.brp_offer_management.offer.entity.*;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import com.jssv.brp_offer_management.offer.repository.OfferPaymentRepository;
import com.jssv.brp_offer_management.security.entity.UserEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferQueryServiceImplTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private OfferPaymentRepository offerPaymentRepository;

    @InjectMocks
    private OfferQueryServiceImpl service;

    private OfferEntity offer;

    @BeforeEach
    void setUp() {

        BrpEntity brp = new BrpEntity();
        brp.setId(1L);
        brp.setCodigo("BRP-001");

        UserEntity radicador = new UserEntity();
        radicador.setId(6L);

        offer = new OfferEntity();
        offer.setId(1L);
        offer.setBrp(brp);
        offer.setRadicador(radicador);
        offer.setValorOferta(
                new BigDecimal("180000000.00")
        );
        offer.setPorcentajeDescuento(
                new BigDecimal("43.7500")
        );
        offer.setEstado("RADICADO");
        offer.setFechaRadicacion(
                OffsetDateTime.of(
                        2026, 11, 21,
                        8, 40, 0, 0,
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void shouldReturnPaginatedOffers() {

        PageRequest pageable = PageRequest.of(0, 10);

        Page<OfferEntity> page = new PageImpl<>(
                List.of(offer),
                pageable,
                1
        );

        when(offerRepository.findAllWithReferences(pageable))
                .thenReturn(page);

        Page<OfferSummaryResponse> result =
                service.findAll(pageable, 2L, "ANALISTA");;

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalPages());

        OfferSummaryResponse response =
                result.getContent().get(0);

        assertEquals(1L, response.id());
        assertEquals(1L, response.brpId());
        assertEquals("BRP-001", response.brpCodigo());
        assertEquals("RADICADO", response.estado());
        assertEquals(6L, response.radicadorId());
        assertNull(response.analistaId());

        assertEquals(
                0,
                new BigDecimal("180000000.00")
                        .compareTo(response.valorOferta())
        );

        verify(offerRepository).findAllWithReferences(pageable);
    }

    @Test
    void shouldReturnOfferDetail() {

        when(offerRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(offer));

        OfferDetailResponse response =
                service.findById(1L, 2L, "ANALISTA");;

        assertEquals(1L, response.id());
        assertEquals("BRP-001", response.brpCodigo());
        assertEquals("RADICADO", response.estado());
        assertEquals(6L, response.radicadorId());
        assertNull(response.analistaId());

        assertEquals(
                0,
                new BigDecimal("43.7500")
                        .compareTo(response.porcentajeDescuento())
        );

        assertTrue(response.oferentes().isEmpty());

        verify(offerRepository)
                .findWithDetailsById(1L);
    }

    @Test
    void shouldReturnApplicantsAndPayments() {

        OfferApplicantEntity applicant =
                new OfferApplicantEntity();

        applicant.setId(10L);
        applicant.setTipoIdentificacion("CC");
        applicant.setNumeroIdentificacion("123456789");
        applicant.setNombre("Oferente de prueba Postman");
        applicant.setPorcentajeParticipacion(
                new BigDecimal("100.0000")
        );
        applicant.setEmail("oferente@example.com");
        applicant.setTelefono("3001234567");

        OfferPaymentEntity payment =
                new OfferPaymentEntity();

        payment.setId(20L);
        payment.setTipoPago("EFECTIVO");
        payment.setValor(
                new BigDecimal("180000000.00")
        );
        payment.setFechaPago(
                LocalDate.of(2026, 11, 21)
        );

        applicant.addPago(payment);
        offer.addOferente(applicant);

        when(offerRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(offer));

        when(offerPaymentRepository.findAllByOfferId(1L))
                .thenReturn(List.of(payment));

        OfferDetailResponse response =
                service.findById(1L, 2L, "ANALISTA");;

        assertEquals(1, response.oferentes().size());

        OfferApplicantResponse applicantResponse =
                response.oferentes().get(0);

        assertEquals(10L, applicantResponse.id());
        assertEquals(
                "Oferente de prueba Postman",
                applicantResponse.nombre()
        );

        assertEquals(
                0,
                new BigDecimal("100.0000")
                        .compareTo(
                                applicantResponse
                                        .porcentajeParticipacion()
                        )
        );

        assertEquals(1, applicantResponse.pagos().size());

        OfferPaymentResponse paymentResponse =
                applicantResponse.pagos().get(0);

        assertEquals(20L, paymentResponse.id());
        assertEquals("EFECTIVO", paymentResponse.tipoPago());

        assertEquals(
                0,
                new BigDecimal("180000000.00")
                        .compareTo(paymentResponse.valor())
        );

        assertEquals(
                LocalDate.of(2026, 11, 21),
                paymentResponse.fechaPago()
        );
    }

    @Test
    void shouldReturn404WhenOfferDoesNotExist() {

        when(offerRepository.findWithDetailsById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(999L, 2L, "ANALISTA")
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(offerRepository)
                .findWithDetailsById(999L);

        verifyNoInteractions(offerPaymentRepository);
    }


    @Test
    void shouldReturnOnlyOwnOffersForRadicador() {

        PageRequest pageable = PageRequest.of(0, 10);

        Page<OfferEntity> page = new PageImpl<>(
                List.of(offer),
                pageable,
                1
        );

        when(offerRepository.findAllByRadicadorId(6L, pageable))
                .thenReturn(page);

        Page<OfferSummaryResponse> result =
                service.findAll(pageable, 6L, "RADICADOR");

        assertEquals(1, result.getTotalElements());
        assertEquals(6L, result.getContent().get(0).radicadorId());

        verify(offerRepository)
                .findAllByRadicadorId(6L, pageable);

        verify(offerRepository, never())
                .findAllWithReferences(any(Pageable.class));
    }

    @Test
    void shouldReturnOwnOfferDetailForRadicador() {

        when(offerRepository.findWithDetailsByIdAndRadicador_Id(1L, 6L))
                .thenReturn(Optional.of(offer));

        OfferDetailResponse response =
                service.findById(1L, 6L, "RADICADOR");

        assertEquals(1L, response.id());
        assertEquals(6L, response.radicadorId());

        verify(offerRepository)
                .findWithDetailsByIdAndRadicador_Id(1L, 6L);

        verify(offerRepository, never())
                .findWithDetailsById(anyLong());
    }

    @Test
    void shouldReturn404ForAnotherRadicadorOffer() {

        when(offerRepository.findWithDetailsByIdAndRadicador_Id(1L, 7L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(1L, 7L, "RADICADOR")
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(offerRepository, never())
                .findWithDetailsById(anyLong());

        verifyNoInteractions(offerPaymentRepository);
    }

    @Test
    void shouldRejectUnknownRole() {

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> service.findAll(
                        PageRequest.of(0, 10),
                        8L,
                        "UNKNOWN"
                )
        );

        verifyNoInteractions(offerRepository);
    }

}
