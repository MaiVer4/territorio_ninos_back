package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.service.TarifaEventoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarifaEventoControllerTest {

    @Mock
    private TarifaEventoService tarifaEventoService;

    @InjectMocks
    private TarifaEventoController tarifaEventoController;

    @Test
    @DisplayName("POST /api/tarifas-evento responde 201 Created")
    void testCrear_Created() {
        TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 1, new BigDecimal("6000.00"), true);
        TarifaEventoResponseDTO responseDTO = new TarifaEventoResponseDTO(1L, 2L, "Toro Mecánico", 1, new BigDecimal("6000.00"), true, LocalDateTime.now());

        when(tarifaEventoService.crear(request)).thenReturn(responseDTO);

        ResponseEntity<TarifaEventoResponseDTO> response = tarifaEventoController.crear(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().intentos());
    }

    @Test
    @DisplayName("GET /api/tarifas-evento/atraccion/{id} responde 200 OK")
    void testObtenerPorAtraccion_Ok() {
        TarifaEventoResponseDTO responseDTO = new TarifaEventoResponseDTO(1L, 2L, "Toro Mecánico", 1, new BigDecimal("6000.00"), true, LocalDateTime.now());
        when(tarifaEventoService.obtenerPorAtraccion(2L)).thenReturn(List.of(responseDTO));

        ResponseEntity<List<TarifaEventoResponseDTO>> response = tarifaEventoController.obtenerPorAtraccion(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("DELETE /api/tarifas-evento/{id} responde 204 No Content")
    void testEliminar_NoContent() {
        doNothing().when(tarifaEventoService).eliminar(1L);

        ResponseEntity<Void> response = tarifaEventoController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(tarifaEventoService, times(1)).eliminar(1L);
    }
}
