package com.jssv.brp_offer_management.service.impl;

import com.jssv.brp_offer_management.dto.response.BrpResponse;
import com.jssv.brp_offer_management.entity.BrpEntity;
import com.jssv.brp_offer_management.repository.BrpRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrpServiceImplTest {

    @Mock
    private BrpRepository brpRepository;

    @InjectMocks
    private BrpServiceImpl brpService;

    private BrpEntity brp;

    @BeforeEach
    void setUp() {
        brp = new BrpEntity();

        brp.setId(1L);
        brp.setCodigo("BRP-0001");
        brp.setEstado("DISPONIBLE");
        brp.setTipoBrp("APARTAMENTO");
        brp.setMatriculaInmobiliaria("001-554854");
        brp.setDepartamento("Antioquia");
        brp.setMunicipio("Medellín");
        brp.setDescripcion("Apartamento de prueba");
        brp.setPrecio(new BigDecimal("320000000.00"));
        brp.setFechaAsignacionPrecio(
                LocalDate.of(2026, 9, 20)
        );
        brp.setFechaIngesta(
                LocalDate.of(2026, 11, 10)
        );
    }

    @Test
    void shouldReturnPaginatedBrpCatalog() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<BrpEntity> entityPage = new PageImpl<>(
                List.of(brp),
                pageable,
                1
        );

        when(brpRepository.findLatestCatalog(pageable))
                .thenReturn(entityPage);

        Page<BrpResponse> result =
                brpService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        BrpResponse response = result.getContent().getFirst();

        assertEquals("BRP-0001", response.codigo());
        assertEquals("DISPONIBLE", response.estado());
        assertEquals("APARTAMENTO", response.tipoBrp());
        assertEquals(
                new BigDecimal("320000000.00"),
                response.precio()
        );

        verify(brpRepository).findLatestCatalog(pageable);
    }

    @Test
    void shouldReturnBrpWhenIdExists() {

        when(brpRepository.findLatestById(1L))
                .thenReturn(Optional.of(brp));

        BrpResponse response = brpService.findById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("BRP-0001", response.codigo());
        assertEquals("Medellín", response.municipio());

        verify(brpRepository).findLatestById(1L);
    }

    @Test
    void shouldThrowNotFoundWhenBrpDoesNotExist() {

        when(brpRepository.findLatestById(9999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> brpService.findById(9999L)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(brpRepository).findLatestById(9999L);
    }

    @Test
    void shouldReturnLatestIngestionDate() {

        LocalDate expectedDate =
                LocalDate.of(2026, 11, 17);

        when(brpRepository.findLatestIngestionDate())
                .thenReturn(Optional.of(expectedDate));

        Optional<LocalDate> result =
                brpService.getLatestIngestionDate();

        assertTrue(result.isPresent());
        assertEquals(expectedDate, result.get());

        verify(brpRepository).findLatestIngestionDate();
    }

    @Test
    void shouldReturnEmptyWhenNoIngestionExists() {

        when(brpRepository.findLatestIngestionDate())
                .thenReturn(Optional.empty());

        Optional<LocalDate> result =
                brpService.getLatestIngestionDate();

        assertTrue(result.isEmpty());

        verify(brpRepository).findLatestIngestionDate();
    }
}
