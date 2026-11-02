package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ReimpresionRequestDTO;
import com.software.territoriodeninos.dto.TicketConsultaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tickets y Códigos QR", description = "Endpoints para generación de códigos QR y consulta de tickets emitidos")
public interface TicketApi {

    @Operation(
        summary = "Buscar tickets por código UUID o cédula del acudiente",
        description = "Ejecuta búsqueda indexada (< 500ms) de tickets por código UUID o por número de cédula del acudiente comprador"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Búsqueda ejecutada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = TicketConsultaDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Criterio de búsqueda nulo o vacío",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<List<TicketConsultaDTO>> buscarTickets(
            @Parameter(description = "Código UUID del ticket o número de cédula del acudiente", required = true)
            @RequestParam(name = "criterio", required = false) String criterio
    );

    @Operation(
        summary = "Consultar información detallada de un ticket por UUID",
        description = "Retorna el estado actual, minutos contratados, hora de emisión, menor, atracción y datos de la venta para un ticket específico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket encontrado y consultado exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TicketConsultaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Formato de UUID inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Ticket no encontrado con el UUID especificado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<TicketConsultaDTO> obtenerTicketPorUuid(
            @Parameter(description = "UUID único del ticket", required = true)
            @PathVariable UUID uuid
    );

    @Operation(
        summary = "Obtener imagen QR de un ticket en formato PNG",
        description = "Genera en memoria RAM una imagen QR de 200x200 píxeles con nivel de corrección 'M' (15% tolerancia) para el ticket indicado"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagen PNG del código QR generada exitosamente",
            content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
        @ApiResponse(responseCode = "400", description = "Formato de UUID inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Ticket no encontrado con el UUID proporcionado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<byte[]> obtenerQrTicket(
            @Parameter(description = "UUID único del ticket", required = true)
            @PathVariable UUID uuid
    );

    @Operation(
        summary = "Obtener payload estructurado ESC/POS para impresión térmica de un ticket",
        description = "Genera el texto estructurado con comandos ESC/POS (encabezados centrados, datos institucionales de ConfiguracionParque, datos del ticket, pie legal de hasta 2000 caracteres y comando de corte automático GS V 66 0) listo para enviar al spooler térmico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payload ESC/POS generado exitosamente",
            content = @Content(mediaType = "text/plain;charset=UTF-8")),
        @ApiResponse(responseCode = "400", description = "Formato de UUID inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Ticket no encontrado con el UUID proporcionado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<String> obtenerPrintPayload(
            @Parameter(description = "UUID único del ticket", required = true)
            @PathVariable UUID uuid
    );

    @Operation(
        summary = "Reimprimir ticket térmico con registro de auditoría inmutable",
        description = "Valida que el ticket no esté CONSUMIDO (HTTP 409) ni ANULADO, pertenezca a una venta del turno activo, registra auditoría inmutable en log_reimpresion y genera el payload térmico ESC/POS con la marca visible ** REIMPRESIÓN **"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket reimpreso exitosamente, retorna payload térmico ESC/POS",
            content = @Content(mediaType = "text/plain;charset=UTF-8")),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida o motivo en blanco",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Ticket no encontrado con el UUID proporcionado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "409", description = "Conflicto: Ticket consumido/anulado o turno no activo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<String> reimprimirTicket(
            @Parameter(description = "UUID único del ticket", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody ReimpresionRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    );
}
