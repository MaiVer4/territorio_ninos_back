package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "DTO de respuesta tras validación de ticket en puesto de acceso (HU-26.02)")
public record ValidacionTicketResponseDTO(
    @Schema(description = "Indica si el ticket fue validado exitosamente", example = "true")
    boolean valido,

    @Schema(description = "Mensaje informativo o descriptivo del resultado", example = "Ticket válido para ingreso")
    String mensaje,

    @Schema(description = "UUID único del ticket", example = "93957dbe-823c-4b57-b5e5-629857541d06")
    UUID uuidTicket,

    @Schema(description = "Estado actual del ticket en el sistema", example = "PAGADO")
    String estadoActual,

    @Schema(description = "ID de la atracción autorizada", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción autorizada", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "Tipo de atracción: TEMPORIZADA o EVENTO", example = "TEMPORIZADA")
    String tipoAtraccion,

    @Schema(description = "ID del menor", example = "10")
    Long menorId,

    @Schema(description = "Nombre completo del menor", example = "Sofia Restrepo")
    String menorNombre,

    @Schema(description = "Edad calculada del menor en años", example = "5")
    Integer menorEdad,

    @Schema(description = "Nombre completo del acudiente responsable", example = "Valentina Restrepo")
    String acudienteNombre,

    @Schema(description = "Documento de identidad del acudiente", example = "1094988776")
    String acudienteDocumento,

    @Schema(description = "Teléfono de contacto del acudiente", example = "3127894561")
    String acudienteTelefono,

    @Schema(description = "Modalidad de tarifa contratada: TIEMPO o EVENTO", example = "TIEMPO")
    String tipoTarifa,

    @Schema(description = "Minutos autorizados contratados para atracciones temporizadas", example = "30")
    Integer minutosAutorizados,

    @Schema(description = "Intentos o rondas autorizadas para atracciones por evento", example = "3")
    Integer intentosAutorizados,

    @Schema(description = "Duración base de la atracción en minutos", example = "15")
    Integer duracionMinutosBase,

    @Schema(description = "Tolerancia o gracia en minutos", example = "5")
    Integer toleranciaMinutos,

    @Schema(description = "Límite de caídas para eventos", example = "3")
    Integer caidasLimite,

    @Schema(description = "Fecha y hora de emisión del ticket", example = "2026-11-02T14:30:00")
    LocalDateTime fechaEmision
) {}
