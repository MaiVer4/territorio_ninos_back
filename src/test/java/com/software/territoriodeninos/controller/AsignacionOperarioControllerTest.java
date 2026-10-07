package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;
import com.software.territoriodeninos.service.AsignacionOperarioService;
import com.software.territoriodeninos.util.EstadoAsignacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsignacionOperarioControllerTest {

    @Mock
    private AsignacionOperarioService asignacionOperarioService;

    @InjectMocks
    private AsignacionOperarioController asignacionOperarioController;

    @Test
    @DisplayName("POST /api/asignaciones responde 201 Created")
    void testAsignar_Created() {
        AsignacionRequestDTO request = new AsignacionRequestDTO(5L, 1L, "Turno tarde");
        AsignacionResponseDTO responseDTO = new AsignacionResponseDTO(
                10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                LocalDate.now(), LocalDateTime.now(), null, EstadoAsignacion.ACTIVA, "Turno tarde"
        );

        when(asignacionOperarioService.asignar(request)).thenReturn(responseDTO);

        ResponseEntity<AsignacionResponseDTO> response = asignacionOperarioController.asignar(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Juan Perez", response.getBody().nombreOperario());
        assertEquals(EstadoAsignacion.ACTIVA, response.getBody().estado());
    }

    @Test
    @DisplayName("POST /api/asignaciones/reasignar responde 200 OK")
    void testReasignar_Ok() {
        ReasignacionRequestDTO request = new ReasignacionRequestDTO(5L, 2L, "Rotación");
        AsignacionResponseDTO responseDTO = new AsignacionResponseDTO(
                11L, 5L, "Juan Perez", "juan@test.com", 2L, "Toro Mecánico",
                LocalDate.now(), LocalDateTime.now(), null, EstadoAsignacion.ACTIVA, "Reasignado"
        );

        when(asignacionOperarioService.reasignar(request)).thenReturn(responseDTO);

        ResponseEntity<AsignacionResponseDTO> response = asignacionOperarioController.reasignar(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Toro Mecánico", response.getBody().nombreAtraccion());
    }

    @Test
    @DisplayName("PATCH /api/asignaciones/{id}/finalizar responde 200 OK")
    void testFinalizar_Ok() {
        AsignacionResponseDTO responseDTO = new AsignacionResponseDTO(
                10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), EstadoAsignacion.FINALIZADA, "Turno tarde"
        );

        when(asignacionOperarioService.finalizar(10L)).thenReturn(responseDTO);

        ResponseEntity<AsignacionResponseDTO> response = asignacionOperarioController.finalizar(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(EstadoAsignacion.FINALIZADA, response.getBody().estado());
    }

    @Test
    @DisplayName("GET /api/asignaciones/activas responde 200 OK")
    void testObtenerActivas_Ok() {
        AsignacionResponseDTO responseDTO = new AsignacionResponseDTO(
                10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                LocalDate.now(), LocalDateTime.now(), null, EstadoAsignacion.ACTIVA, "Turno tarde"
        );
        when(asignacionOperarioService.obtenerActivas()).thenReturn(List.of(responseDTO));

        ResponseEntity<List<AsignacionResponseDTO>> response = asignacionOperarioController.obtenerActivas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("GET /api/asignaciones/operario/{id}/activa responde 200 OK")
    void testObtenerActivaPorOperario_Ok() {
        AsignacionResponseDTO responseDTO = new AsignacionResponseDTO(
                10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                LocalDate.now(), LocalDateTime.now(), null, EstadoAsignacion.ACTIVA, "Turno tarde"
        );
        when(asignacionOperarioService.obtenerActivaPorOperario(5L)).thenReturn(responseDTO);

        ResponseEntity<AsignacionResponseDTO> response = asignacionOperarioController.obtenerActivaPorOperario(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody().usuarioId());
    }
}
