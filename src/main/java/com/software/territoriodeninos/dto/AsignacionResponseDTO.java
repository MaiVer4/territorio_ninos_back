package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.util.EstadoAsignacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO de respuesta para asignación de operario")
public record AsignacionResponseDTO(
    Long id,
    Long usuarioId,
    String nombreOperario,
    String correoOperario,
    Long atraccionId,
    String nombreAtraccion,
    LocalDate fechaAsignacion,
    LocalDateTime horaInicio,
    LocalDateTime horaFin,
    EstadoAsignacion estado,
    String observaciones
) {
    @JsonProperty("operarioId")
    @Schema(description = "Alias frontend: ID del operario", example = "5")
    public Long operarioId() {
        return usuarioId;
    }

    @JsonProperty("operarioNombre")
    @Schema(description = "Alias frontend: Nombre del operario", example = "Carlos Operario")
    public String operarioNombre() {
        return nombreOperario;
    }

    @JsonProperty("operarioCorreo")
    @Schema(description = "Alias frontend: Correo del operario", example = "carlos@parque.com")
    public String operarioCorreo() {
        return correoOperario;
    }

    @JsonProperty("atraccionNombre")
    @Schema(description = "Alias frontend: Nombre de la atracción", example = "Montaña Rusa")
    public String atraccionNombre() {
        return nombreAtraccion;
    }

    @JsonProperty("activo")
    @Schema(description = "Alias frontend: Indica si la asignación está activa", example = "true")
    public boolean activo() {
        return estado == EstadoAsignacion.ACTIVA;
    }
}

