package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.service.AtraccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaControllerTest - Pruebas de integración Web con MockMvc para /api/ventas")
class VentaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AtraccionService atraccionService;

    @InjectMocks
    private VentaController ventaController;

    private CatalogoAtraccionVentaDTO mockAtraccionCatalogo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ventaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TarifaTiempoResponseDTO tarifaTiempo = new TarifaTiempoResponseDTO(
                10L, 1L, "Piscina de Pelotas", 15, new BigDecimal("12000.00"), true, LocalDateTime.now()
        );

        TarifaEventoResponseDTO tarifaEvento = new TarifaEventoResponseDTO(
                20L, 1L, "Piscina de Pelotas", 3, new BigDecimal("8000.00"), true, LocalDateTime.now()
        );

        mockAtraccionCatalogo = new CatalogoAtraccionVentaDTO(
                1L,
                "Piscina de Pelotas",
                "Área con piscina y tobogán",
                "TEMPORIZADA",
                "ACTIVO",
                new BigDecimal("1000.00"),
                15,
                5,
                3,
                20,
                5,
                15,
                true,
                List.of(tarifaTiempo),
                List.of(tarifaEvento)
        );
    }

    @Test
    @DisplayName("GET /api/ventas/catalogo - Retorna 200 OK con header Cache-Control y catálogo completo")
    void testObtenerCatalogo_Exitoso() throws Exception {
        when(atraccionService.obtenerCatalogoVenta()).thenReturn(List.of(mockAtraccionCatalogo));

        mockMvc.perform(get("/api/ventas/catalogo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=5")))
                .andExpect(header().string("Cache-Control", containsString("must-revalidate")))
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$[0].tipo", is("TEMPORIZADA")))
                .andExpect(jsonPath("$[0].estado", is("ACTIVO")))
                .andExpect(jsonPath("$[0].capacidadMaxima", is(20)))
                .andExpect(jsonPath("$[0].ocupacionActual", is(5)))
                .andExpect(jsonPath("$[0].aforoDisponible", is(15)))
                .andExpect(jsonPath("$[0].tieneCupo", is(true)))
                .andExpect(jsonPath("$[0].ninosActivos", is(5)))
                .andExpect(jsonPath("$[0].tieneOperarioAsignado", is(true)))
                .andExpect(jsonPath("$[0].tarifasTiempo", hasSize(1)))
                .andExpect(jsonPath("$[0].tarifasTiempo[0].duracionMinutos", is(15)))
                .andExpect(jsonPath("$[0].tarifasEvento", hasSize(1)))
                .andExpect(jsonPath("$[0].tarifasEvento[0].intentos", is(3)));

        verify(atraccionService).obtenerCatalogoVenta();
    }

    @Test
    @DisplayName("GET /api/ventas/catalogo - Retorna lista vacía cuando no hay atracciones activas")
    void testObtenerCatalogo_Vacio() throws Exception {
        when(atraccionService.obtenerCatalogoVenta()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ventas/catalogo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=5")))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(atraccionService).obtenerCatalogoVenta();
    }
}
