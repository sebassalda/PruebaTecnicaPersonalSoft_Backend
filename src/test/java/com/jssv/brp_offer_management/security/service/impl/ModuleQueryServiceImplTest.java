package com.jssv.brp_offer_management.security.service.impl;

import com.jssv.brp_offer_management.security.dto.response.ModuleResponse;
import com.jssv.brp_offer_management.security.entity.ModuleEntity;
import com.jssv.brp_offer_management.security.repository.ModuleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleQueryServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @InjectMocks
    private ModuleQueryServiceImpl service;

    @Test
    void shouldReturnAllModules() {

        ModuleEntity catalog = createModule(
                1L,
                "BRP_CATALOG",
                "Catalogo BRP"
        );

        ModuleEntity management = createModule(
                2L,
                "USER_MANAGEMENT",
                "Administracion de usuarios"
        );

        when(moduleRepository.findAll(
                Sort.by(Sort.Direction.ASC, "codigo")
        )).thenReturn(List.of(catalog, management));

        List<ModuleResponse> result = service.findAll();

        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).id());
        assertEquals("BRP_CATALOG", result.get(0).codigo());
        assertEquals("Catalogo BRP", result.get(0).nombre());

        assertEquals(2L, result.get(1).id());
        assertEquals("USER_MANAGEMENT", result.get(1).codigo());

        verify(moduleRepository).findAll(
                Sort.by(Sort.Direction.ASC, "codigo")
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoModulesExist() {

        when(moduleRepository.findAll(
                Sort.by(Sort.Direction.ASC, "codigo")
        )).thenReturn(List.of());

        List<ModuleResponse> result = service.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPreserveRepositoryOrdering() {

        ModuleEntity first = createModule(
                1L,
                "BRP_CATALOG",
                "Catalogo BRP"
        );

        ModuleEntity second = createModule(
                2L,
                "OFFER_APPROVAL",
                "Aprobacion de ofertas"
        );

        ModuleEntity third = createModule(
                3L,
                "USER_MANAGEMENT",
                "Administracion de usuarios"
        );

        when(moduleRepository.findAll(
                Sort.by(Sort.Direction.ASC, "codigo")
        )).thenReturn(List.of(first, second, third));

        List<ModuleResponse> result = service.findAll();

        assertEquals(
                List.of(
                        "BRP_CATALOG",
                        "OFFER_APPROVAL",
                        "USER_MANAGEMENT"
                ),
                result.stream()
                        .map(ModuleResponse::codigo)
                        .toList()
        );
    }

    private ModuleEntity createModule(
            Long id,
            String code,
            String name
    ) {

        ModuleEntity module = new ModuleEntity();

        module.setId(id);
        module.setCodigo(code);
        module.setNombre(name);

        return module;
    }
}
