package com.jssv.brp_offer_management.brp.repository;

import com.jssv.brp_offer_management.brp.entity.BrpEntity;

import com.jssv.brp_offer_management.brp.repository.BrpRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BrpRepositoryTest {

    @Autowired
    private BrpRepository brpRepository;

    @Test
    void shouldReturnInitialCatalogWithOneHundredBrps() {

        Page<BrpEntity> result = brpRepository.findLatestCatalog(
                PageRequest.of(0, 10)
        );

        assertEquals(100, result.getTotalElements());
        assertEquals(10, result.getContent().size());
        assertEquals(10, result.getTotalPages());
    }

    @Test
    void shouldReturnOnlyLatestIngestionForSameBrp() {

        BrpEntity original = brpRepository.findLatestCatalog(
                        PageRequest.of(0, 100)
                )
                .getContent()
                .stream()
                .filter(brp -> "BRP-0001".equals(brp.getCodigo()))
                .findFirst()
                .orElseThrow();

        BrpEntity newIngestion = new BrpEntity();

        newIngestion.setCodigo(original.getCodigo());
        newIngestion.setEstado("DISPONIBLE");
        newIngestion.setTipoBrp(original.getTipoBrp());
        newIngestion.setMatriculaInmobiliaria(
                original.getMatriculaInmobiliaria()
        );
        newIngestion.setPlaca(original.getPlaca());
        newIngestion.setDepartamento(original.getDepartamento());
        newIngestion.setMunicipio(original.getMunicipio());
        newIngestion.setDescripcion("Nueva ingesta de prueba");
        newIngestion.setPrecio(
                new BigDecimal("350000000.00")
        );
        newIngestion.setFechaAsignacionPrecio(
                LocalDate.of(2026, 11, 18)
        );
        newIngestion.setFechaIngesta(
                LocalDate.of(2026, 11, 19)
        );

        brpRepository.saveAndFlush(newIngestion);

        Page<BrpEntity> result = brpRepository.findLatestCatalog(
                PageRequest.of(0, 100)
        );

        assertEquals(100, result.getTotalElements());

        BrpEntity latest = result.getContent()
                .stream()
                .filter(brp -> "BRP-0001".equals(brp.getCodigo()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                LocalDate.of(2026, 11, 19),
                latest.getFechaIngesta()
        );

        assertEquals(
                new BigDecimal("350000000.00"),
                latest.getPrecio()
        );
    }

    @Test
    void shouldNotReturnHistoricalBrpById() {

        BrpEntity original = brpRepository.findLatestCatalog(
                        PageRequest.of(0, 100)
                )
                .getContent()
                .stream()
                .filter(brp -> "BRP-0001".equals(brp.getCodigo()))
                .findFirst()
                .orElseThrow();

        BrpEntity newIngestion = new BrpEntity();

        newIngestion.setCodigo(original.getCodigo());
        newIngestion.setEstado(original.getEstado());
        newIngestion.setTipoBrp(original.getTipoBrp());
        newIngestion.setMatriculaInmobiliaria(
                original.getMatriculaInmobiliaria()
        );
        newIngestion.setPlaca(original.getPlaca());
        newIngestion.setDepartamento(original.getDepartamento());
        newIngestion.setMunicipio(original.getMunicipio());
        newIngestion.setDescripcion("Versión actualizada");
        newIngestion.setPrecio(original.getPrecio());
        newIngestion.setFechaAsignacionPrecio(
                original.getFechaAsignacionPrecio()
        );
        newIngestion.setFechaIngesta(
                LocalDate.of(2026, 11, 19)
        );

        brpRepository.saveAndFlush(newIngestion);

        Optional<BrpEntity> historical =
                brpRepository.findLatestById(original.getId());

        Optional<BrpEntity> latest =
                brpRepository.findLatestById(newIngestion.getId());

        assertTrue(historical.isEmpty());
        assertTrue(latest.isPresent());
    }

    @Test
    void shouldReturnLatestIngestionDate() {

        Optional<LocalDate> result =
                brpRepository.findLatestIngestionDate();

        assertTrue(result.isPresent());

        assertEquals(
                LocalDate.of(2026, 11, 17),
                result.get()
        );
    }
}
