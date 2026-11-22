
package com.jssv.brp_offer_management.offer.service;

import com.jssv.brp_offer_management.offer.dto.request.OfferCreateRequest;

public interface OfferCommandService {

    Long createOffer(
            OfferCreateRequest request,
            Long radicadorId
    );
}
