package com.jssv.brp_offer_management.offer.entity;

import com.jssv.brp_offer_management.brp.entity.BrpEntity;
import com.jssv.brp_offer_management.offer.entity.OfferApplicantEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "oferta")
@Getter
@Setter
public class OfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brp_id", nullable = false)
    private BrpEntity brp;

    @Column(name = "valor_oferta", nullable = false,
            precision = 19, scale = 2)
    private BigDecimal valorOferta;

    @Column(name = "porcentaje_descuento",
            precision = 10, scale = 4)
    private BigDecimal porcentajeDescuento;

    @Column(nullable = false, length = 30)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "radicador_id", nullable = false)
    private UserEntity radicador;

    @Column(name = "fecha_radicacion", nullable = false)
    private OffsetDateTime fechaRadicacion;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(
            mappedBy = "oferta",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OfferApplicantEntity> oferentes =
            new ArrayList<>();

    public void addOferente(OfferApplicantEntity oferente) {
        oferentes.add(oferente);
        oferente.setOferta(this);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_id")
    private UserEntity analista;

    @Column(name = "fecha_actualizacion")
    private OffsetDateTime fechaActualizacion;

    @Column(name = "fecha_escalamiento")
    private OffsetDateTime fechaEscalamiento;

    @Column(name = "fecha_devolucion")
    private OffsetDateTime fechaDevolucion;

    @Column(name = "fecha_finalizacion")
    private OffsetDateTime fechaFinalizacion;
}
