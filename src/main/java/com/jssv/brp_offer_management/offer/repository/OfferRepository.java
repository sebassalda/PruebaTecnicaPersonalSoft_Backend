package com.jssv.brp_offer_management.offer.repository;

import com.jssv.brp_offer_management.offer.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository
        extends JpaRepository<OfferEntity, Long> {
}
