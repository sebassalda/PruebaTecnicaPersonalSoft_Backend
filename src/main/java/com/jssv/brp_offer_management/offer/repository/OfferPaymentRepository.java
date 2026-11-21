package com.jssv.brp_offer_management.offer.repository;

import com.jssv.brp_offer_management.offer.entity.OfferPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferPaymentRepository
        extends JpaRepository<OfferPaymentEntity, Long> {
}
