package com.jssv.brp_offer_management.controller;

import com.jssv.brp_offer_management.dto.response.BrpResponse;
import com.jssv.brp_offer_management.service.BrpService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/brps")
@RequiredArgsConstructor
public class BrpController {

    private final BrpService brpService;

    @GetMapping
    public ResponseEntity<Page<BrpResponse>> findAll(
            @PageableDefault(size = 10, sort = "codigo")
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                brpService.findAll(pageable)
        );
    }

    @GetMapping("/latest-ingestion")
    public ResponseEntity<Map<String, LocalDate>> getLatestIngestion() {
        return brpService.getLatestIngestionDate()
                .map(date -> ResponseEntity.ok(
                        Map.of("latestIngestionDate", date)
                ))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrpResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                brpService.findById(id)
        );
    }
}
