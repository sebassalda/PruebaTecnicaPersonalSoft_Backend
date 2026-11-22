package com.jssv.brp_offer_management.offer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferPaymentResponse(
        Long id,
        String tipoPago,
        BigDecimal valor,
        LocalDate fechaPago,
        String entidadFinanciera
) {
}
