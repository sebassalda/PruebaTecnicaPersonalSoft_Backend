package com.jssv.brp_offer_management.security.repository;

import com.jssv.brp_offer_management.security.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuleRepository
        extends JpaRepository<ModuleEntity, Long> {

    Optional<ModuleEntity> findByCodigo(String codigo);
}
