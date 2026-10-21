package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.util.EstadoTurnoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnoCajaResponseDTO(
    Long id,
    Long usuarioId,
    BigDecimal montoBase,
    LocalDateTime fechaApertura,
    LocalDateTime fechaCierre,
    BigDecimal ventasEfectivoSistema,
    BigDecimal montoEfectivoReportado,
    BigDecimal diferencia,
    EstadoTurnoCaja estado,
    String observaciones,
    String cajeroNombre
) {
    public TurnoCajaResponseDTO(
        Long id,
        Long usuarioId,
        BigDecimal montoBase,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal ventasEfectivoSistema,
        BigDecimal montoEfectivoReportado,
        BigDecimal diferencia,
        EstadoTurnoCaja estado,
        String observaciones
    ) {
        this(id, usuarioId, montoBase, fechaApertura, fechaCierre, ventasEfectivoSistema, montoEfectivoReportado, diferencia, estado, observaciones, null);
    }

    @JsonProperty("idTurno")
    public Long idTurno() {
        return id;
    }

    @JsonProperty("montoInicial")
    public BigDecimal montoInicial() {
        return montoBase;
    }

    @JsonProperty("montoFinal")
    public BigDecimal montoFinal() {
        return montoEfectivoReportado;
    }

    @JsonProperty("totalEsperado")
    public BigDecimal totalEsperado() {
        BigDecimal base = montoBase != null ? montoBase : BigDecimal.ZERO;
        BigDecimal ventas = ventasEfectivoSistema != null ? ventasEfectivoSistema : BigDecimal.ZERO;
        return base.add(ventas);
    }

    public static TurnoCajaResponseDTO fromEntity(TurnoCaja turno) {
        return fromEntity(turno, null);
    }

    public static TurnoCajaResponseDTO fromEntity(TurnoCaja turno, String cajeroNombre) {
        if (turno == null) {
            return null;
        }
        return new TurnoCajaResponseDTO(
            turno.getId(),
            turno.getUsuarioId(),
            turno.getMontoBase(),
            turno.getFechaApertura(),
            turno.getFechaCierre(),
            turno.getVentasEfectivoSistema(),
            turno.getMontoEfectivoReportado(),
            turno.getDiferencia(),
            turno.getEstado(),
            turno.getObservaciones(),
            cajeroNombre
        );
    }
}
