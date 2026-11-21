package com.jssv.brp_offer_management.offer.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferPaymentRequest(

        @NotBlank
        @Pattern(regexp = "EFECTIVO|CREDITO",
                message = "El tipo de pago debe ser EFECTIVO o CREDITO")
        String tipoPago,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal valor,

        @NotNull
        LocalDate fechaPago,

        @Size(max = 150)
        String entidadFinanciera
) {
}
