package com.jssv.brp_offer_management.offer.service;

import com.jssv.brp_offer_management.offer.dto.response.OfferTransitionResponse;

public interface OfferTransitionService {
    OfferTransitionResponse transition(Long offerId, String action, Long userId, String role);
}
