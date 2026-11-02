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
}
