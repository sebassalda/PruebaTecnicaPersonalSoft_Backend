package com.jssv.brp_offer_management.brp.service.impl;

import com.jssv.brp_offer_management.brp.dto.response.BrpResponse;
import com.jssv.brp_offer_management.brp.entity.BrpEntity;
import com.jssv.brp_offer_management.brp.repository.BrpRepository;
import com.jssv.brp_offer_management.brp.service.BrpService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrpServiceImpl implements BrpService {

    private final BrpRepository brpRepository;

    @Override
    public Page<BrpResponse> findAll(Pageable pageable) {
        return brpRepository.findLatestCatalog(pageable)
                .map(this::toResponse);
    }

    @Override
    public BrpResponse findById(Long id) {
        BrpEntity brp = brpRepository.findLatestById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "BRP no encontrado"
                ));

        return toResponse(brp);
    }

    @Override
    public Optional<LocalDate> getLatestIngestionDate() {
        return brpRepository.findLatestIngestionDate();
    }

    private BrpResponse toResponse(BrpEntity brp) {
        return new BrpResponse(
                brp.getId(),
                brp.getCodigo(),
                brp.getEstado(),
                brp.getTipoBrp(),
                brp.getMatriculaInmobiliaria(),
                brp.getPlaca(),
                brp.getDepartamento(),
                brp.getMunicipio(),
                brp.getDescripcion(),
                brp.getPrecio(),
                brp.getFechaAsignacionPrecio(),
                brp.getFechaIngesta()
        );
    }
}
