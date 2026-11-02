package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "DTO de respuesta y estado en tiempo real de una sesión de juego (HU-27.03)")
public record SesionJuegoResponseDTO(
    @Schema(description = "ID único de la sesión de juego", example = "105")
    Long sesionId,

    @Schema(description = "UUID del ticket asociado", example = "93957dbe-823c-4b57-b5e5-629857541d06")
    UUID ticketUuid,

    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "ID del menor", example = "15")
    Long menorId,

    @Schema(description = "Nombre completo del menor", example = "Sofia Restrepo")
    String menorNombre,

    @Schema(description = "Edad del menor", example = "5")
    Integer menorEdad,

    @Schema(description = "Nombre del acudiente responsable", example = "Valentina Restrepo")
    String acudienteNombre,

    @Schema(description = "Teléfono de contacto del acudiente", example = "3127894561")
    String acudienteTelefono,

    @Schema(description = "Documento del acudiente", example = "1094988776")
    String acudienteDocumento,

    @Schema(description = "Nombre del operario a cargo", example = "Carlos Operario")
    String operarioNombre,

    @Schema(description = "Correo del operario", example = "carlos@territoriodeninos.com")
    String operarioCorreo,

    @Schema(description = "Hora de inicio de la sesión", example = "2026-11-02T15:30:00")
    LocalDateTime horaInicio,

    @Schema(description = "Hora programada de finalización", example = "2026-11-02T15:45:00")
    LocalDateTime horaFin,

    @Schema(description = "Duración total contratada en segundos", example = "900")
    Integer duracionSegundos,

    @Schema(description = "Segundos restantes de juego en tiempo real", example = "340")
    Integer segundosRestantes,

    @Schema(description = "Segundos congelados durante la pausa", example = "450")
    Integer tiempoRestantePausa,

    @Schema(description = "Instante exacto en que se pausó la sesión", example = "2026-11-02T15:35:10")
    LocalDateTime fechaPausa,

    @Schema(description = "Estado de la sesión (EN_CURSO, PAUSADA, FINALIZADA)", example = "EN_CURSO")
    String estadoSesion,

    @Schema(description = "Segundos acumulados de permanencia en exceso", example = "0")
    Integer segundosExceso,

    @Schema(description = "Intentos totales para modalidad de eventos", example = "3")
    Integer intentosTotales,

    @Schema(description = "Intentos restantes disponibles", example = "2")
    Integer intentosRestantes,

    @Schema(description = "Indica si la alerta preventiva de <= 120s fue emitida", example = "false")
    boolean alertaPreExpiracion,

    @Schema(description = "Indica si el tiempo contratado ya expiró (<= 0 segundos)", example = "false")
    boolean tiempoExpirado
) {}
