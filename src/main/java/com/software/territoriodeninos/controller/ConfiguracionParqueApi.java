package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Configuración del Parque", description = "Endpoints para la parametrización institucional y operativa del parque")
@SecurityRequirement(name = "Bearer Token")
public interface ConfiguracionParqueApi {

    @Operation(summary = "Obtener configuración general del parque")
    @ApiResponse(responseCode = "200", description = "Configuración obtenida")
    ResponseEntity<ConfiguracionParqueDTO> obtener();

    @Operation(summary = "Actualizar configuración general del parque (Solo Administrador)")
    @ApiResponse(responseCode = "200", description = "Configuración actualizada")
    ResponseEntity<ConfiguracionParqueDTO> actualizar(@Valid @RequestBody ConfiguracionParqueDTO dto);
}
