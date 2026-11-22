package com.jssv.brp_offer_management.brp.repository;

import com.jssv.brp_offer_management.brp.entity.BrpEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface BrpRepository extends JpaRepository<BrpEntity, Long> {

    @Query(
            value = """
            SELECT b
            FROM BrpEntity b
            WHERE NOT EXISTS (
                SELECT 1
                FROM BrpEntity newer
                WHERE newer.codigo = b.codigo
                  AND (newer.fechaIngesta > b.fechaIngesta OR (newer.fechaIngesta = b.fechaIngesta AND newer.id > b.id))
            )
            """,
            countQuery = """
            SELECT COUNT(b)
            FROM BrpEntity b
            WHERE NOT EXISTS (
                SELECT 1
                FROM BrpEntity newer
                WHERE newer.codigo = b.codigo
                  AND (newer.fechaIngesta > b.fechaIngesta OR (newer.fechaIngesta = b.fechaIngesta AND newer.id > b.id))
            )
            """
    )
    Page<BrpEntity> findLatestCatalog(Pageable pageable);

    @Query("""
        SELECT b
        FROM BrpEntity b
        WHERE b.id = :id
          AND NOT EXISTS (
              SELECT 1
              FROM BrpEntity newer
              WHERE newer.codigo = b.codigo
                AND (newer.fechaIngesta > b.fechaIngesta OR (newer.fechaIngesta = b.fechaIngesta AND newer.id > b.id))
          )
        """)
    Optional<BrpEntity> findLatestById(@Param("id") Long id);

    @Query(value = """
        SELECT b FROM BrpEntity b
        WHERE b.estado = 'DISPONIBLE'
          AND NOT EXISTS (
            SELECT 1 FROM BrpEntity newer
            WHERE newer.codigo = b.codigo
              AND (newer.fechaIngesta > b.fechaIngesta
                   OR (newer.fechaIngesta = b.fechaIngesta AND newer.id > b.id))
          )
        """, countQuery = """
        SELECT COUNT(b) FROM BrpEntity b
        WHERE b.estado = 'DISPONIBLE'
          AND NOT EXISTS (
            SELECT 1 FROM BrpEntity newer
            WHERE newer.codigo = b.codigo
              AND (newer.fechaIngesta > b.fechaIngesta
                   OR (newer.fechaIngesta = b.fechaIngesta AND newer.id > b.id))
          )
        """)
    Page<BrpEntity> findLatestAvailableCatalog(Pageable pageable);

    @Query("""
        SELECT b FROM BrpEntity b
        WHERE b.id = :id AND b.estado = 'DISPONIBLE'
          AND NOT EXISTS (
            SELECT 1 FROM BrpEntity newer
            WHERE newer.codigo = b.codigo
              AND (newer.fechaIngesta > b.fechaIngesta
                   OR (newer.fechaIngesta = b.fechaIngesta AND newer.id > b.id))
          )
        """)
    Optional<BrpEntity> findLatestAvailableById(@Param("id") Long id);

    @Query("""
        SELECT MAX(b.fechaIngesta)
        FROM BrpEntity b
        """)
    Optional<LocalDate> findLatestIngestionDate();
}
