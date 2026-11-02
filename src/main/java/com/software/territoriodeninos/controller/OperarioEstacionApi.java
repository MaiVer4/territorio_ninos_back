package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.EstacionTrabajoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Estación Operativa de Trabajo", description = "Endpoints para consulta de la estación física asignada al operario en su turno activo")
public interface OperarioEstacionApi {

    @Operation(
        summary = "Obtener estación de trabajo activa asignada al operario",
        description = "Valida y entrega los detalles del puesto de trabajo asignado al operario autenticado en su turno actual (HU-24.03)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estación activa encontrada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = EstacionTrabajoDTO.class))),
        @ApiResponse(responseCode = "404", description = "El operario no tiene una asignación activa en el parque",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<EstacionTrabajoDTO> obtenerEstacionActiva(
            @Parameter(hidden = true) Authentication authentication
    );
}
