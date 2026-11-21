package com.jssv.brp_offer_management.brp.service;

import com.jssv.brp_offer_management.brp.dto.response.BrpResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface BrpService {

    Page<BrpResponse> findAll(Pageable pageable);

    Page<BrpResponse> findAll(Pageable pageable, boolean onlyAvailable);

    BrpResponse findById(Long id);

    BrpResponse findById(Long id, boolean onlyAvailable);

    Optional<LocalDate> getLatestIngestionDate();
}
