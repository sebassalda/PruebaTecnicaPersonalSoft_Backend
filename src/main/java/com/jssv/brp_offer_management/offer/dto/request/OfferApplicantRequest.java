package com.jssv.brp_offer_management.offer.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record OfferApplicantRequest(

        @NotBlank
        @Pattern(regexp = "CC|NIT",
                message = "El tipo de identificación debe ser CC o NIT")
        String tipoIdentificacion,

        @NotBlank
        @Size(max = 50)
        String numeroIdentificacion,

        @NotBlank
        @Size(max = 200)
        String nombre,

        @NotNull
        @DecimalMin(value = "0.0001")
        @DecimalMax(value = "100.0000")
        @Digits(integer = 3, fraction = 4)
        BigDecimal porcentajeParticipacion,

        @Email
        @Size(max = 150)
        String email,

        @Size(max = 30)
        String telefono,

        @NotNull
        @Size(min = 1, max = 6,
                message = "Cada oferente debe registrar entre 1 y 6 pagos")
        List<@NotNull @Valid OfferPaymentRequest> pagos
) {
}
