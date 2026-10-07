package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Tarifas por Evento", description = "Endpoints para la gestión de tarifas por evento o intentos (M02)")
@SecurityRequirement(name = "Bearer Token")
public interface TarifaEventoApi {

    @Operation(summary = "Crear tarifa por evento", description = "Configura una tarifa por número de intentos para una atracción (ej: Toro Mecánico)")
    @ApiResponse(responseCode = "201", description = "Tarifa por evento creada exitosamente")
    ResponseEntity<TarifaEventoResponseDTO> crear(@Valid @RequestBody TarifaEventoRequestDTO request);

    @Operation(summary = "Obtener tarifas de evento de una atracción", description = "Lista todas las tarifas por evento configuradas para una atracción")
    @ApiResponse(responseCode = "200", description = "Lista de tarifas obtenida")
    ResponseEntity<List<TarifaEventoResponseDTO>> obtenerPorAtraccion(@PathVariable Long atraccionId);

    @Operation(summary = "Obtener tarifa por evento por ID", description = "Obtiene los detalles de una tarifa por evento específica")
    @ApiResponse(responseCode = "200", description = "Tarifa encontrada")
    ResponseEntity<TarifaEventoResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Actualizar tarifa por evento", description = "Actualiza el precio, intentos o estado de una tarifa existente")
    @ApiResponse(responseCode = "200", description = "Tarifa actualizada")
    ResponseEntity<TarifaEventoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody TarifaEventoRequestDTO request);

    @Operation(summary = "Eliminar tarifa por evento", description = "Elimina permanentemente una tarifa por evento")
    @ApiResponse(responseCode = "204", description = "Tarifa eliminada")
    ResponseEntity<Void> eliminar(@PathVariable Long id);
}
