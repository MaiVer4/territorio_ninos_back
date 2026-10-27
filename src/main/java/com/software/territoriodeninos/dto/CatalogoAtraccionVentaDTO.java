package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO del catálogo de ventas con datos de la atracción, tarifarios vigentes y aforo en tiempo real")
public record CatalogoAtraccionVentaDTO(
    @Schema(description = "ID único de la atracción", example = "1")
    Long id,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String nombre,

    @Schema(description = "Descripción de la atracción", example = "Zona infantil con piscina de pelotas y tobogán")
    String descripcion,

    @Schema(description = "Tipo de atracción (TEMPORIZADA, EVENTOS_CAIDAS)", example = "TEMPORIZADA")
    String tipo,

    @Schema(description = "Estado operativo actual", example = "ACTIVO")
    String estado,

    @Schema(description = "Tarifa base por minuto o fracción adicional", example = "1000.00")
    BigDecimal tarifaMinuto,

    @Schema(description = "Duración base en minutos", example = "15")
    Integer duracionMinutosBase,

    @Schema(description = "Minutos de tolerancia o gracia", example = "5")
    Integer toleranciaMinutos,

    @Schema(description = "Límite de caídas para atracciones por eventos", example = "3")
    Integer caidasLimite,

    @Schema(description = "Capacidad máxima total de la atracción", example = "20")
    Integer capacidadMaxima,

    @Schema(description = "Cantidad de menores actualmente dentro de la atracción", example = "5")
    Integer ocupacionActual,

    @Schema(description = "Cupos disponibles en tiempo real (capacidadMaxima - ocupacionActual)", example = "15")
    Integer aforoDisponible,

    @Schema(description = "Indica si actualmente tiene un operario asignado activo", example = "true")
    Boolean tieneOperarioAsignado,

    @Schema(description = "Lista de tarifas por tiempo vigentes ordenadas por duración ascendente")
    List<TarifaTiempoResponseDTO> tarifasTiempo,

    @Schema(description = "Lista de tarifas por evento vigentes ordenadas por número de intentos ascendente")
    List<TarifaEventoResponseDTO> tarifasEvento
) {
    public CatalogoAtraccionVentaDTO {
        if (tarifasTiempo == null) {
            tarifasTiempo = Collections.emptyList();
        }
        if (tarifasEvento == null) {
            tarifasEvento = Collections.emptyList();
        }
    }

    @JsonProperty("tieneCupo")
    @Schema(description = "Indica si hay cupo disponible para ingresar", example = "true")
    public boolean tieneCupo() {
        return aforoDisponible != null && aforoDisponible > 0;
    }

    @JsonProperty("ninosActivos")
    @Schema(description = "Alias para ocupación actual en frontend", example = "5")
    public Integer ninosActivos() {
        return ocupacionActual;
    }
}
