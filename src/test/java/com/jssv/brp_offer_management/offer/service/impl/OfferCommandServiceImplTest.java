package com.jssv.brp_offer_management.offer.service.impl;

import com.jssv.brp_offer_management.brp.entity.BrpEntity;
import com.jssv.brp_offer_management.brp.repository.BrpRepository;
import com.jssv.brp_offer_management.offer.dto.request.*;
import com.jssv.brp_offer_management.offer.entity.OfferEntity;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferCommandServiceImplTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private BrpRepository brpRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OfferCommandServiceImpl service;

    private static final Long BRP_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final Long OFFER_ID = 30L;

    @Test
    void shouldCreateOfferSuccessfully() {
        mockAvailableBrp("200000000.00");
        mockRadicador();

        when(offerRepository.saveAndFlush(any(OfferEntity.class)))
                .thenAnswer(invocation -> {
                    OfferEntity offer = invocation.getArgument(0);
                    offer.setId(OFFER_ID);
                    return offer;
                });

        Long result = service.createOffer(
                validRequest("180000000.00"),
                USER_ID
        );

        assertEquals(OFFER_ID, result);

        var captor = org.mockito.ArgumentCaptor
                .forClass(OfferEntity.class);

        verify(offerRepository).saveAndFlush(captor.capture());

        OfferEntity saved = captor.getValue();

        assertEquals("RADICADO", saved.getEstado());

        assertEquals(
                0,
                new BigDecimal("180000000.00")
                        .compareTo(saved.getValorOferta())
        );

        assertEquals(
                0,
                new BigDecimal("10.0000")
                        .compareTo(saved.getPorcentajeDescuento())
        );

        assertEquals(1, saved.getOferentes().size());
        assertEquals(1, saved.getOferentes().get(0).getPagos().size());

        assertSame(saved, saved.getOferentes().get(0).getOferta());

        assertSame(
                saved.getOferentes().get(0),
                saved.getOferentes().get(0).getPagos().get(0).getOferente()
        );

        assertNotNull(saved.getFechaRadicacion());
        assertNotNull(saved.getFechaActualizacion());
    }

    @Test
    void shouldReturn404WhenBrpDoesNotExist() {
        when(brpRepository.findLatestById(BRP_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createOffer(
                        validRequest("180000000.00"),
                        USER_ID
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(offerRepository);
    }

    @Test
    void shouldReturn409WhenBrpIsNotAvailable() {
        BrpEntity brp = new BrpEntity();
        brp.setEstado("NO_DISPONIBLE");

        when(brpRepository.findLatestById(BRP_ID))
                .thenReturn(Optional.of(brp));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createOffer(
                        validRequest("180000000.00"),
                        USER_ID
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(offerRepository);
    }

    @Test
    void shouldReturn409WhenBrpPriceIsNull() {
        mockAvailableBrp(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createOffer(
                        validRequest("180000000.00"),
                        USER_ID
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(offerRepository);
    }

    @Test
    void shouldReturn404WhenRadicadorDoesNotExist() {
        mockAvailableBrp("200000000.00");

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createOffer(
                        validRequest("180000000.00"),
                        USER_ID
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(offerRepository);
    }

    @Test
    void shouldRejectParticipationDifferentFrom100() {
        mockAvailableBrp("200000000.00");
        mockRadicador();

        OfferApplicantRequest applicant = applicant(
                "80.0000",
                "144000000.00"
        );

        OfferCreateRequest request = new OfferCreateRequest(
                BRP_ID,
                new BigDecimal("180000000.00"),
                List.of(applicant)
        );

        assertBadRequest(request);
    }

    @Test
    void shouldRejectPaymentsThatDoNotMatchParticipation() {
        mockAvailableBrp("200000000.00");
        mockRadicador();

        OfferCreateRequest request = new OfferCreateRequest(
                BRP_ID,
                new BigDecimal("180000000.00"),
                List.of(applicant("100.0000", "170000000.00"))
        );

        assertBadRequest(request);
    }

    @Test
    void shouldRejectMoreThanFiveApplicants() {
        mockAvailableBrp("200000000.00");
        mockRadicador();

        OfferApplicantRequest applicant = applicant(
                "20.0000",
                "36000000.00"
        );

        OfferCreateRequest request = new OfferCreateRequest(
                BRP_ID,
                new BigDecimal("180000000.00"),
                List.of(
                        applicant, applicant, applicant,
                        applicant, applicant, applicant
                )
        );

        assertBadRequest(request);
    }

    @Test
    void shouldRejectMoreThanSixPayments() {
        mockAvailableBrp("200000000.00");
        mockRadicador();

        OfferPaymentRequest payment = payment("100.00");

        OfferApplicantRequest applicant = new OfferApplicantRequest(
                "CC",
                "123456789",
                "Oferente de prueba",
                new BigDecimal("100.0000"),
                "test@example.com",
                "3001234567",
                List.of(
                        payment, payment, payment, payment,
                        payment, payment, payment
                )
        );

        OfferCreateRequest request = new OfferCreateRequest(
                BRP_ID,
                new BigDecimal("180000000.00"),
                List.of(applicant)
        );

        assertBadRequest(request);
    }

    @Test
    void shouldCalculateNegativeDiscountWhenOfferExceedsPrice() {
        mockAvailableBrp("200000000.00");
        mockRadicador();

        when(offerRepository.saveAndFlush(any(OfferEntity.class)))
                .thenAnswer(invocation -> {
                    OfferEntity offer = invocation.getArgument(0);
                    offer.setId(OFFER_ID);
                    return offer;
                });

        service.createOffer(
                validRequest("220000000.00"),
                USER_ID
        );

        var captor = org.mockito.ArgumentCaptor
                .forClass(OfferEntity.class);

        verify(offerRepository).saveAndFlush(captor.capture());

        assertEquals(
                0,
                new BigDecimal("-10.0000").compareTo(
                        captor.getValue().getPorcentajeDescuento()
                )
        );
    }

    private void mockAvailableBrp(String price) {
        BrpEntity brp = new BrpEntity();
        brp.setEstado("DISPONIBLE");

        if (price != null) {
            brp.setPrecio(new BigDecimal(price));
        }

        when(brpRepository.findLatestById(BRP_ID))
                .thenReturn(Optional.of(brp));
    }

    private void mockRadicador() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
    }

    private OfferCreateRequest validRequest(String amount) {
        return new OfferCreateRequest(
                BRP_ID,
                new BigDecimal(amount),
                List.of(applicant("100.0000", amount))
        );
    }

    private OfferApplicantRequest applicant(
            String percentage,
            String paymentAmount
    ) {
        return new OfferApplicantRequest(
                "CC",
                "123456789",
                "Oferente de prueba",
                new BigDecimal(percentage),
                "test@example.com",
                "3001234567",
                List.of(payment(paymentAmount))
        );
    }

    private OfferPaymentRequest payment(String amount) {
        return new OfferPaymentRequest(
                "EFECTIVO",
                new BigDecimal(amount),
                LocalDate.of(2026, 11, 21),
                null
        );
    }

    private void assertBadRequest(OfferCreateRequest request) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createOffer(request, USER_ID)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(offerRepository);
    }
}
