package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Auditoría de Operaciones e Incidentes", description = "Endpoints de trazabilidad cronológica inmutable de tickets y auditoría de paradas e incidentes")
public interface AuditoriaApi {

    @Operation(
            summary = "Consultar trazabilidad cronológica de tickets",
            description = "Recupera la línea temporal de estados transitados, operarios y marcas de tiempo por UUID o documento del acudiente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timeline de boleta(s) obtenido exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TimelineTicketDTO.class)))),
            @ApiResponse(responseCode = "404", description = "No se encontraron boletas para el criterio especificado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<List<TimelineTicketDTO>> consultarTimelineTicket(
            @Parameter(description = "UUID del ticket o número de documento del acudiente", example = "1020304050")
            @PathVariable String criterio
    );

    @Operation(
            summary = "Monitoreo y resumen de incidentes y paradas de atracciones",
            description = "Consulta interrupciones de servicio discriminando pausas operativas vs técnicas y calculando tiempos de inactividad"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de incidentes calculado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumenIncidentesAuditoriaDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<ResumenIncidentesAuditoriaDTO> consultarIncidentes(
            @Parameter(description = "Fecha inicial del rango", example = "2026-11-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,

            @Parameter(description = "Fecha final del rango", example = "2026-11-02T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,

            @Parameter(description = "ID de la atracción opcional para filtrar", example = "1")
            @RequestParam(required = false) Long atraccionId,

            @Parameter(description = "Tipo de pausa (OPERATIVA, TECNICA)", example = "TECNICA")
            @RequestParam(required = false) String tipoPausa
    );
}
