package com.jssv.brp_offer_management.security.repository;

import com.jssv.brp_offer_management.security.entity.UserModuleEntity;
import com.jssv.brp_offer_management.security.entity.UserModuleId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserModuleRepository
        extends JpaRepository<UserModuleEntity, UserModuleId> {

    @Query("""
            SELECT um.modulo.codigo
            FROM UserModuleEntity um
            WHERE um.usuarioId = :usuarioId
            """)
    List<String> findModuleCodesByUserId(
            @Param("usuarioId") Long usuarioId
    );

    boolean existsByUsuarioIdAndModuloId(
            Long usuarioId,
            Long moduloId
    );

    void deleteByUsuarioIdAndModuloId(
            Long usuarioId,
            Long moduloId
    );
}
