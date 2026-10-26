package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Turnos de Caja", description = "Endpoints para apertura, cierre, consulta activa e historial de turnos de caja de taquilla")
public interface TurnoCajaApi {

    @Operation(summary = "Abrir turno de caja", description = "Inicia un nuevo turno de caja para el cajero autenticado con un monto base inicial")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Turno abierto exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TurnoCajaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o regla de negocio no cumplida",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "El usuario ya cuenta con un turno de caja abierto",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<TurnoCajaResponseDTO> abrirTurno(
            @Valid @RequestBody AperturaCajaDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "Obtener turno activo", description = "Consulta el turno de caja actualmente abierto del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno activo encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TurnoCajaResponseDTO.class))),
        @ApiResponse(responseCode = "204", description = "No hay turno activo para el usuario"),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<TurnoCajaResponseDTO> obtenerTurnoActivo(
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "Cerrar turno de caja (PUT)", description = "Cierra el turno de caja activo calculando diferencias y registrando novedades de arqueo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno cerrado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TurnoCajaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o regla de negocio no cumplida",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "No se encontró turno de caja abierto para cerrar",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<TurnoCajaResponseDTO> cerrarTurno(
            @Valid @RequestBody CierreCajaDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "Cerrar turno de caja (POST)", description = "Alias POST para el cierre del turno de caja activo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno cerrado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TurnoCajaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o regla de negocio no cumplida",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "No se encontró turno de caja abierto para cerrar",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<TurnoCajaResponseDTO> cerrarTurnoPost(
            @Valid @RequestBody CierreCajaDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "Consultar historial de turnos", description = "Obtiene el historial de turnos de caja ordenados descendentemente, opcionalmente filtrados por fecha (YYYY-MM-DD)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TurnoCajaResponseDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Formato de fecha inválido",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<List<TurnoCajaResponseDTO>> obtenerHistorialTurnos(
            @Parameter(description = "Fecha de consulta en formato YYYY-MM-DD", example = "2026-10-25")
            @RequestParam(required = false) String fecha
    );
}
