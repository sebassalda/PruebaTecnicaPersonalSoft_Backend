package com.jssv.brp_offer_management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BrpResponse(
        Long id,
        String codigo,
        String estado,
        String tipoBrp,
        String matriculaInmobiliaria,
        String placa,
        String departamento,
        String municipio,
        String descripcion,
        BigDecimal precio,
        LocalDate fechaAsignacionPrecio,
        LocalDate fechaIngesta
) {
}
