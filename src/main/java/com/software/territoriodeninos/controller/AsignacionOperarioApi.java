package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Asignación de Operarios", description = "Endpoints para la gestión y asignación de operarios a atracciones (M02 / HU-10)")
@SecurityRequirement(name = "Bearer Token")
public interface AsignacionOperarioApi {

    @Operation(summary = "Asignar operario a atracción", description = "Vincula un operario activo a una atracción física. Falla con 409 si ya tiene asignación activa.")
    @ApiResponse(responseCode = "201", description = "Operario asignado exitosamente")
    ResponseEntity<AsignacionResponseDTO> asignar(@Valid @RequestBody AsignacionRequestDTO request);

    @Operation(summary = "Reasignar operario a otra atracción", description = "Cierra la asignación activa previa (REASIGNADA) y crea una nueva de manera atómica.")
    @ApiResponse(responseCode = "200", description = "Operario reasignado exitosamente")
    ResponseEntity<AsignacionResponseDTO> reasignar(@Valid @RequestBody ReasignacionRequestDTO request);

    @Operation(summary = "Finalizar turno de operario", description = "Finaliza la asignación activa actual de un operario registrando hora_fin.")
    @ApiResponse(responseCode = "200", description = "Asignación finalizada")
    ResponseEntity<AsignacionResponseDTO> finalizar(@PathVariable Long id);

    @Operation(summary = "Liberar o desasignar operario", description = "Libera la asignación activa de un operario delegando a la finalización de turno (DELETE /{id}).")
    @ApiResponse(responseCode = "200", description = "Asignación liberada exitosamente")
    ResponseEntity<AsignacionResponseDTO> liberar(@PathVariable Long id);

    @Operation(summary = "Obtener todas las asignaciones activas", description = "Retorna el listado de todas las asignaciones actualmente activas en el parque.")
    @ApiResponse(responseCode = "200", description = "Lista de asignaciones activas")
    ResponseEntity<List<AsignacionResponseDTO>> obtenerActivas();

    @Operation(summary = "Obtener asignación activa de un operario", description = "Permite a la aplicación móvil del operario consultar a qué atracción está asignado.")
    @ApiResponse(responseCode = "200", description = "Asignación activa encontrada")
    ResponseEntity<AsignacionResponseDTO> obtenerActivaPorOperario(@PathVariable Long usuarioId);

    @Operation(summary = "Obtener asignaciones activas de una atracción", description = "Consulta los operarios asignados a una atracción física.")
    @ApiResponse(responseCode = "200", description = "Lista de asignaciones activas por atracción")
    ResponseEntity<List<AsignacionResponseDTO>> obtenerActivasPorAtraccion(@PathVariable Long atraccionId);

    @Operation(summary = "Obtener historial de asignaciones de un operario", description = "Lista todas las asignaciones históricas de un operario.")
    @ApiResponse(responseCode = "200", description = "Historial de asignaciones obtenido")
    ResponseEntity<List<AsignacionResponseDTO>> obtenerHistorialPorOperario(@PathVariable Long usuarioId);
}
