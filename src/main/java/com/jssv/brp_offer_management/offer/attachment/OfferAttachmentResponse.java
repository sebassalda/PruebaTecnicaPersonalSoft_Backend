package com.jssv.brp_offer_management.offer.attachment;

import java.time.OffsetDateTime;

public record OfferAttachmentResponse(Long id, String nombreArchivo, OffsetDateTime fechaCarga) {
    public static OfferAttachmentResponse from(OfferAttachmentEntity entity) {
        return new OfferAttachmentResponse(entity.getId(), entity.getNombreArchivo(), entity.getFechaCarga());
    }
}
