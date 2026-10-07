package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import com.software.territoriodeninos.service.AtraccionService;
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
class AtraccionControllerTest {

    @Mock
    private AtraccionService atraccionService;

    @InjectMocks
    private AtraccionController atraccionController;

    @Test
    @DisplayName("POST /api/atracciones responde 201 Created")
    void testCrear_Created() {
        AtraccionRequestDTO request = new AtraccionRequestDTO("Inflable", "Para niños", new BigDecimal("50.00"));
        AtraccionResponseDTO responseDTO = new AtraccionResponseDTO(
                1L, "Inflable", "Para niños", new BigDecimal("50.00"), "ACTIVO", LocalDateTime.now()
        );

        when(atraccionService.crear(request)).thenReturn(responseDTO);

        ResponseEntity<AtraccionResponseDTO> response = atraccionController.crear(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Inflable", response.getBody().nombre());
    }

    @Test
    @DisplayName("GET /api/atracciones responde 200 OK")
    void testObtenerTodas_Ok() {
        AtraccionResponseDTO responseDTO = new AtraccionResponseDTO(
                1L, "Inflable", "Para niños", new BigDecimal("50.00"), "ACTIVO", LocalDateTime.now()
        );
        when(atraccionService.obtenerTodas()).thenReturn(List.of(responseDTO));

        ResponseEntity<List<AtraccionResponseDTO>> response = atraccionController.obtenerTodas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("GET /api/atracciones/{id} responde 200 OK")
    void testObtenerPorId_Ok() {
        AtraccionResponseDTO responseDTO = new AtraccionResponseDTO(
                1L, "Inflable", "Para niños", new BigDecimal("50.00"), "ACTIVO", LocalDateTime.now()
        );
        when(atraccionService.obtenerPorId(1L)).thenReturn(responseDTO);

        ResponseEntity<AtraccionResponseDTO> response = atraccionController.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
    }

    @Test
    @DisplayName("DELETE /api/atracciones/{id} responde 204 No Content")
    void testEliminar_NoContent() {
        doNothing().when(atraccionService).eliminar(1L);

        ResponseEntity<Void> response = atraccionController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(atraccionService, times(1)).eliminar(1L);
    }
}
