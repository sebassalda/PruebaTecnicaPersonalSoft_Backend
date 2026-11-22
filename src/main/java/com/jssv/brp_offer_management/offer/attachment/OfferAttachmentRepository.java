package com.jssv.brp_offer_management.offer.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OfferAttachmentRepository extends JpaRepository<OfferAttachmentEntity, Long> {
    List<OfferAttachmentEntity> findAllByOferta_IdOrderByFechaCargaDesc(Long ofertaId);
    Optional<OfferAttachmentEntity> findByIdAndOferta_Id(Long id, Long ofertaId);
}
