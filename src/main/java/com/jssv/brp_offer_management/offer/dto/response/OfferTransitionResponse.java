package com.jssv.brp_offer_management.offer.dto.response;

import java.time.OffsetDateTime;

public record OfferTransitionResponse(
        Long id, String estado, Long analistaId, Long version,
        OffsetDateTime fechaActualizacion, OffsetDateTime fechaEscalamiento,
        OffsetDateTime fechaDevolucion, OffsetDateTime fechaFinalizacion
) {}
