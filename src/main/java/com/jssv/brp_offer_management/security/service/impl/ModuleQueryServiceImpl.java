
package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.ModuleResponse;
import com.jssv.brp_offer_management.security.entity.ModuleEntity;
import com.jssv.brp_offer_management.security.repository.ModuleRepository;
import com.jssv.brp_offer_management.security.service.ModuleQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleQueryServiceImpl implements ModuleQueryService {

    private final ModuleRepository moduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> findAll() {

        return moduleRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "codigo")
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ModuleResponse toResponse(ModuleEntity module) {

        return new ModuleResponse(
                module.getId(),
                module.getCodigo(),
                module.getNombre()
        );
    }
}
