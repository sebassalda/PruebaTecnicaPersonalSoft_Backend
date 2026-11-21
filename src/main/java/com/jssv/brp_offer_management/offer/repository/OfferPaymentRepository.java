
package com.jssv.brp_offer_management.offer.repository;

import com.jssv.brp_offer_management.offer.entity.OfferPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferPaymentRepository
        extends JpaRepository<OfferPaymentEntity, Long> {

    @Query("""
            SELECT p
            FROM OfferPaymentEntity p
            WHERE p.oferente.oferta.id = :offerId
            ORDER BY p.id
            """)
    List<OfferPaymentEntity> findAllByOfferId(
            @Param("offerId") Long offerId
    );
}
