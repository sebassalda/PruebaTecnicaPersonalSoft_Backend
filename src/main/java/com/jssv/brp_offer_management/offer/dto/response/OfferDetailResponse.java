package com.jssv.brp_offer_management.offer.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OfferDetailResponse(
        Long id,
        Long brpId,
        String brpCodigo,
        BigDecimal valorOferta,
        BigDecimal porcentajeDescuento,
        String estado,
        Long radicadorId,
        Long analistaId,
        OffsetDateTime fechaRadicacion,
        OffsetDateTime fechaActualizacion,
        OffsetDateTime fechaEscalamiento,
        OffsetDateTime fechaDevolucion,
        OffsetDateTime fechaFinalizacion,
        List<OfferApplicantResponse> oferentes
) {
}
