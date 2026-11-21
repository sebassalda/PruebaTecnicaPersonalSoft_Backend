
package com.jssv.brp_offer_management.offer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pago")
@Getter
@Setter
public class OfferPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferente_id", nullable = false)
    private OfferApplicantEntity oferente;

    @Column(name = "tipo_pago", nullable = false, length = 40)
    private String tipoPago;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "entidad_financiera", length = 150)
    private String entidadFinanciera;
}
