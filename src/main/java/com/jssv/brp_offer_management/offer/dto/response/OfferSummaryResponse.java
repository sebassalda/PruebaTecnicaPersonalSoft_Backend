package com.jssv.brp_offer_management.offer.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OfferSummaryResponse(
        Long id,
        Long brpId,
        String brpCodigo,
        BigDecimal valorOferta,
        BigDecimal porcentajeDescuento,
        String estado,
        Long radicadorId,
        Long analistaId,
        OffsetDateTime fechaRadicacion
) {
}
