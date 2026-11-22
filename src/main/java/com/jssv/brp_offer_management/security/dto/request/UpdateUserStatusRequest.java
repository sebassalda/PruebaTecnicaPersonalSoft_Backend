
package com.jssv.brp_offer_management.security.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

        @NotNull(message = "El estado del usuario es obligatorio")
        Boolean activo

) {
}
