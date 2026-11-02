package com.software.territoriodeninos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "Tickets y Códigos QR", description = "Endpoints para generación de códigos QR y consulta de tickets emitidos")
public interface TicketApi {

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
}
