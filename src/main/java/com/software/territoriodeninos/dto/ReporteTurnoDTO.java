package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.EstadoConciliacionTurno;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ReporteTurnoDTO(
        @Schema(description = "ID del turno de caja", example = "10")
        Long turnoId,

        @Schema(description = "ID del cajero/usuario", example = "3")
        Long usuarioId,

        @Schema(description = "Nombre del cajero", example = "Carlos Pérez")
        String nombreCajero,

        @Schema(description = "Fecha y hora de apertura del turno")
        LocalDateTime fechaApertura,

        @Schema(description = "Fecha y hora de cierre del turno")
        LocalDateTime fechaCierre,

        @Schema(description = "Base inicial en efectivo asignada", example = "50000.00")
        BigDecimal baseInicial,

        @Schema(description = "Ventas en efectivo registradas por el sistema", example = "120000.00")
        BigDecimal ventasEfectivoSistema,

        @Schema(description = "Ventas por medios electrónicos registradas por el sistema", example = "85000.00")
        BigDecimal ventasElectronicoSistema,

        @Schema(description = "Total esperado en efectivo (base inicial + ventas en efectivo)", example = "170000.00")
        BigDecimal totalEsperadoEfectivo,

        @Schema(description = "Monto físico en efectivo reportado por el cajero al cierre", example = "170000.00")
        BigDecimal montoFisicoDeclarado,

        @Schema(description = "Diferencia monetaria detectada (físico reportado - total esperado)", example = "0.00")
        BigDecimal diferencia,

        @Schema(description = "Estado algorítmico de la conciliación: CUADRADO, SOBRANTE o FALTANTE")
        EstadoConciliacionTurno estadoConciliacion,

        @Schema(description = "Estado actual del turno (ABIERTO o CERRADO)")
        EstadoTurnoCaja estadoTurno,

        @Schema(description = "Total general facturado en el turno (efectivo + electrónicos)", example = "205000.00")
        BigDecimal totalFacturado,

        @Schema(description = "Total de transacciones de venta procesadas en el turno", example = "15")
        Integer totalTransacciones,

        @Schema(description = "Observaciones o justificaciones registradas al cierre")
        String observaciones
) {}
