package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Tarifas por Tiempo", description = "Endpoints para la gestión de tarifas por bloques de tiempo (M02)")
@SecurityRequirement(name = "Bearer Token")
public interface TarifaTiempoApi {

    @Operation(summary = "Crear tarifa por tiempo", description = "Configura una tarifa para una atracción por bloque de tiempo (ej: 15, 30, 60 min)")
    @ApiResponse(responseCode = "201", description = "Tarifa creada exitosamente")
    ResponseEntity<TarifaTiempoResponseDTO> crear(@Valid @RequestBody TarifaTiempoRequestDTO request);

    @Operation(summary = "Obtener tarifas de una atracción", description = "Lista todas las tarifas por tiempo configuradas para una atracción")
    @ApiResponse(responseCode = "200", description = "Lista de tarifas obtenida")
    ResponseEntity<List<TarifaTiempoResponseDTO>> obtenerPorAtraccion(@PathVariable Long atraccionId);

    @Operation(summary = "Obtener tarifa por ID", description = "Obtiene los detalles de una tarifa por tiempo específica")
    @ApiResponse(responseCode = "200", description = "Tarifa encontrada")
    ResponseEntity<TarifaTiempoResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Actualizar tarifa por tiempo", description = "Actualiza el precio, duración o estado de una tarifa existente")
    @ApiResponse(responseCode = "200", description = "Tarifa actualizada")
    ResponseEntity<TarifaTiempoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody TarifaTiempoRequestDTO request);

    @Operation(summary = "Eliminar tarifa por tiempo", description = "Elimina permanentemente una tarifa por tiempo")
    @ApiResponse(responseCode = "204", description = "Tarifa eliminada")
    ResponseEntity<Void> eliminar(@PathVariable Long id);
}
