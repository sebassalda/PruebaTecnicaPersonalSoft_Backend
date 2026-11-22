package com.jssv.brp_offer_management.offer.attachment;

import com.jssv.brp_offer_management.offer.entity.OfferEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "adjunto")
@Getter @Setter
public class OfferAttachmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private OfferEntity oferta;
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;
    @Column(name = "ruta_local", nullable = false, length = 1024)
    private String rutaLocal;
    @Column(name = "fecha_carga", nullable = false)
    private OffsetDateTime fechaCarga;
}
