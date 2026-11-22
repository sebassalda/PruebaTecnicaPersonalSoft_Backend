package com.jssv.brp_offer_management.offer.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record OfferCreateRequest(

        @NotNull
        @Positive
        Long brpId,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal valorOferta,

        @NotNull
        @Size(min = 1, max = 5,
                message = "La oferta debe tener entre 1 y 5 oferentes")
        List<@NotNull @Valid OfferApplicantRequest> oferentes
) {
}
