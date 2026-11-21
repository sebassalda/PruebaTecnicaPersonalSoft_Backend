package com.jssv.brp_offer_management.offer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "oferente")
@Getter
@Setter
public class OfferApplicantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private OfferEntity oferta;

    @Column(name = "tipo_identificacion",
            nullable = false, length = 30)
    private String tipoIdentificacion;

    @Column(name = "numero_identificacion",
            nullable = false, length = 50)
    private String numeroIdentificacion;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "porcentaje_participacion",
            nullable = false, precision = 7, scale = 4)
    private BigDecimal porcentajeParticipacion;

    @OneToMany(
            mappedBy = "oferente",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OfferPaymentEntity> pagos =
            new ArrayList<>();

    public void addPago(OfferPaymentEntity pago) {
        pagos.add(pago);
        pago.setOferente(this);
    }

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;
}
