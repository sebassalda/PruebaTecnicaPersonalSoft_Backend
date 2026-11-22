package com.jssv.brp_offer_management.offer.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record OfferApplicantResponse(
        Long id,
        String tipoIdentificacion,
        String numeroIdentificacion,
        String nombre,
        BigDecimal porcentajeParticipacion,
        String email,
        String telefono,
        List<OfferPaymentResponse> pagos
) {
}
