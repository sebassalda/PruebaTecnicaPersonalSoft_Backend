package com.jssv.brp_offer_management.offer.repository;

import com.jssv.brp_offer_management.offer.entity.OfferEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfferRepository
        extends JpaRepository<OfferEntity, Long> {


    @EntityGraph(attributePaths = {
            "brp",
            "radicador",
            "analista",
            "oferentes"
    })
    Optional<OfferEntity> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "brp",
            "radicador",
            "analista",
            "oferentes"
    })
    Optional<OfferEntity> findWithDetailsByIdAndRadicador_Id(
            Long id,
            Long radicadorId
    );

    @Query(
            value = """
            SELECT o
            FROM OfferEntity o
            JOIN FETCH o.brp
            JOIN FETCH o.radicador
            LEFT JOIN FETCH o.analista
            """,
            countQuery = """
            SELECT COUNT(o)
            FROM OfferEntity o
            """
    )
    Page<OfferEntity> findAllWithReferences(
            Pageable pageable
    );

    @Query(
            value = """
            SELECT o
            FROM OfferEntity o
            JOIN FETCH o.brp
            JOIN FETCH o.radicador
            LEFT JOIN FETCH o.analista
            WHERE o.radicador.id = :radicadorId
            """,
            countQuery = """
            SELECT COUNT(o)
            FROM OfferEntity o
            WHERE o.radicador.id = :radicadorId
            """
    )
    Page<OfferEntity> findAllByRadicadorId(
            @Param("radicadorId") Long radicadorId,
            Pageable pageable
    );


}
