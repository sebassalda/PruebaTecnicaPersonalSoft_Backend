
package com.jssv.brp_offer_management.offer.service;

import com.jssv.brp_offer_management.offer.dto.response.OfferDetailResponse;
import com.jssv.brp_offer_management.offer.dto.response.OfferSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OfferQueryService {

    Page<OfferSummaryResponse> findAll(
            Pageable pageable,
            Long authenticatedUserId,
            String authenticatedRole
    );

    OfferDetailResponse findById(
            Long offerId,
            Long authenticatedUserId,
            String authenticatedRole
    );
}
