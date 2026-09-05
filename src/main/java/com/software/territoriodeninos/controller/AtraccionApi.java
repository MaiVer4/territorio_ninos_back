package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Atracciones", description = "Endpoints para la gestión de atracciones")
@SecurityRequirement(name = "Bearer Token")
public interface AtraccionApi {

    @Operation(summary = "Crear nueva atracción")
    @ApiResponse(responseCode = "201", description = "Atracción creada")
    ResponseEntity<AtraccionResponseDTO> crear(@Valid @RequestBody AtraccionRequestDTO request);

    @Operation(summary = "Obtener todas las atracciones")
    @ApiResponse(responseCode = "200", description = "Lista de atracciones obtenida")
    ResponseEntity<List<AtraccionResponseDTO>> obtenerTodas();

    @Operation(summary = "Obtener atracciones activas")
    @ApiResponse(responseCode = "200", description = "Lista de atracciones activas obtenida")
    ResponseEntity<List<AtraccionResponseDTO>> obtenerActivas();

    @Operation(summary = "Obtener atracción por ID")
    @ApiResponse(responseCode = "200", description = "Atracción encontrada")
    ResponseEntity<AtraccionResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Actualizar atracción")
    @ApiResponse(responseCode = "200", description = "Atracción actualizada")
    ResponseEntity<AtraccionResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody AtraccionRequestDTO request);

    @Operation(summary = "Cambiar estado de atracción")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    ResponseEntity<AtraccionResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam String estado);

    @Operation(summary = "Desactivar atracción")
    @ApiResponse(responseCode = "204", description = "Atracción desactivada")
    ResponseEntity<Void> eliminar(@PathVariable Long id);
}
