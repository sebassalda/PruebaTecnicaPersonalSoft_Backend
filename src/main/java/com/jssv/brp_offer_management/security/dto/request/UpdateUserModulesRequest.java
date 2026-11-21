
package com.jssv.brp_offer_management.security.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserModulesRequest(

        @NotNull(message = "La lista de módulos es obligatoria")
        List<@NotNull(message = "El código del módulo no puede ser nulo")
                String> modules

) {
}
