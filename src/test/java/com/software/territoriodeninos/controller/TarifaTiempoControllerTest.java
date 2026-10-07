package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.service.TarifaTiempoService;
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
class TarifaTiempoControllerTest {

    @Mock
    private TarifaTiempoService tarifaTiempoService;

    @InjectMocks
    private TarifaTiempoController tarifaTiempoController;

    @Test
    @DisplayName("POST /api/tarifas-tiempo responde 201 Created")
    void testCrear_Created() {
        TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, 15, new BigDecimal("5000.00"), true);
        TarifaTiempoResponseDTO responseDTO = new TarifaTiempoResponseDTO(1L, 1L, "Inflable", 15, new BigDecimal("5000.00"), true, LocalDateTime.now());

        when(tarifaTiempoService.crear(request)).thenReturn(responseDTO);

        ResponseEntity<TarifaTiempoResponseDTO> response = tarifaTiempoController.crear(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(15, response.getBody().duracionMinutos());
    }

    @Test
    @DisplayName("GET /api/tarifas-tiempo/atraccion/{id} responde 200 OK")
    void testObtenerPorAtraccion_Ok() {
        TarifaTiempoResponseDTO responseDTO = new TarifaTiempoResponseDTO(1L, 1L, "Inflable", 15, new BigDecimal("5000.00"), true, LocalDateTime.now());
        when(tarifaTiempoService.obtenerPorAtraccion(1L)).thenReturn(List.of(responseDTO));

        ResponseEntity<List<TarifaTiempoResponseDTO>> response = tarifaTiempoController.obtenerPorAtraccion(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("DELETE /api/tarifas-tiempo/{id} responde 204 No Content")
    void testEliminar_NoContent() {
        doNothing().when(tarifaTiempoService).eliminar(1L);

        ResponseEntity<Void> response = tarifaTiempoController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(tarifaTiempoService, times(1)).eliminar(1L);
    }
}
