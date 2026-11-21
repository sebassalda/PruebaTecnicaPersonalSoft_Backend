package com.jssv.brp_offer_management.brp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "brp")
@Getter
@Setter
public class BrpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(name = "tipo_brp", nullable = false, length = 30)
    private String tipoBrp;

    @Column(name = "matricula_inmobiliaria", length = 100)
    private String matriculaInmobiliaria;

    @Column(length = 20)
    private String placa;

    @Column(length = 100)
    private String departamento;

    @Column(length = 100)
    private String municipio;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(precision = 19, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_asignacion_precio")
    private LocalDate fechaAsignacionPrecio;

    @Column(name = "fecha_ingesta", nullable = false)
    private LocalDate fechaIngesta;
}
