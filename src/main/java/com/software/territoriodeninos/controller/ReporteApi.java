package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.DetalleTurnoAuditadoDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Reportes y Auditoría Contable", description = "Endpoints administrativos para conciliación financiera de turnos y métricas de rotación por atracción")
public interface ReporteApi {

    @Operation(
            summary = "Consultar reporte de conciliación de turnos",
            description = "Recupera turnos cerrados calculando diferencias monetarias (FALTANTE, SOBRANTE, CUADRADO) con filtros de fecha y cajero"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte obtenido exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReporteTurnoDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<List<ReporteTurnoDTO>> obtenerReporteTurnos(
            @Parameter(description = "Fecha inicial (ISO DATE_TIME)", example = "2026-11-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,

            @Parameter(description = "Fecha final (ISO DATE_TIME)", example = "2026-11-02T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,

            @Parameter(description = "ID del usuario cajero opcional", example = "2")
            @RequestParam(required = false) Long usuarioId
    );

    @Operation(
            summary = "Detalle auditado de un turno de caja",
            description = "Obtiene el resumen contable, todas las ventas registradas y las boletas asociadas al turno auditado"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del turno auditado obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DetalleTurnoAuditadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Turno de caja no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<DetalleTurnoAuditadoDTO> obtenerDetalleTurno(
            @Parameter(description = "ID del turno a auditar", example = "5")
            @PathVariable Long id
    );

    @Operation(
            summary = "Reporte de rotación y demanda por atracción",
            description = "Calcula usos totales, ingresos brutos, tiempo promedio de permanencia, franja pico y tasa de ocupación relativa"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas de rotación calculadas exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RotacionAtraccionDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<List<RotacionAtraccionDTO>> obtenerRotacionAtracciones(
            @Parameter(description = "Fecha inicial del rango", example = "2026-10-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,

            @Parameter(description = "Fecha final del rango", example = "2026-10-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,

            @Parameter(description = "Agrupación temporal: DIARIO, SEMANAL, MENSUAL", example = "MENSUAL")
            @RequestParam(required = false, defaultValue = "MENSUAL") String periodo,

            @Parameter(description = "Criterio de ordenación: USOS, INGRESOS, OCUPACION", example = "USOS")
            @RequestParam(required = false, defaultValue = "USOS") String criterioOrden
    );

    @Operation(
            summary = "Exportar reporte en formato PDF",
            description = "Descarga documento PDF con membrete institucional, paginación y formato de moneda (HU-39.02, HU-39.03, HU-39.05)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo PDF generado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<byte[]> exportarPdf(
            @Parameter(description = "Tipo de reporte: TURNOS, ROTACION, INCIDENTES", example = "TURNOS")
            @RequestParam(required = false, defaultValue = "TURNOS") String tipoReporte,

            @Parameter(description = "Fecha inicial del rango", example = "2026-11-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,

            @Parameter(description = "Fecha final del rango", example = "2026-11-02T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,

            @Parameter(description = "ID del cajero opcional", example = "1")
            @RequestParam(required = false) Long usuarioId,

            @Parameter(description = "ID de atracción opcional", example = "2")
            @RequestParam(required = false) Long atraccionId
    );

    @Operation(
            summary = "Exportar reporte en formato CSV",
            description = "Descarga archivo CSV compatible con Microsoft Excel con codificación UTF-8 BOM (HU-39.04, HU-39.05)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo CSV generado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<byte[]> exportarCsv(
            @Parameter(description = "Tipo de reporte: TURNOS, ROTACION, INCIDENTES", example = "TURNOS")
            @RequestParam(required = false, defaultValue = "TURNOS") String tipoReporte,

            @Parameter(description = "Fecha inicial del rango", example = "2026-11-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,

            @Parameter(description = "Fecha final del rango", example = "2026-11-02T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,

            @Parameter(description = "ID del cajero opcional", example = "1")
            @RequestParam(required = false) Long usuarioId,

            @Parameter(description = "ID de atracción opcional", example = "2")
            @RequestParam(required = false) Long atraccionId
    );

    @Operation(
            summary = "Métricas estadísticas de horas pico de afluencia",
            description = "Consulta la afluencia agregada en intervalos de 1 hora entre las 08:00 y las 20:00 (HU-40.02, HU-40.04)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estadísticas de horas pico obtenidas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.software.territoriodeninos.dto.GraficoTendenciaDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<com.software.territoriodeninos.dto.GraficoTendenciaDTO> obtenerHorasPico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin
    );

    @Operation(
            summary = "Distribución de ingresos por método de pago",
            description = "Calcula montos brutos y porcentajes relativos por método de pago (HU-40.03, HU-40.04)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribución de métodos de pago obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.software.territoriodeninos.dto.GraficoTendenciaDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<com.software.territoriodeninos.dto.GraficoTendenciaDTO> obtenerMetodosPago(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin
    );

    @Operation(
            summary = "Evolución de ingresos en los últimos 7 días",
            description = "Tendencia diaria de recaudación para los últimos 7 días con totales y porcentajes (HU-40.04)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evolución de ingresos obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.software.territoriodeninos.dto.GraficoTendenciaDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: solo Administrador")
    })
    ResponseEntity<com.software.territoriodeninos.dto.GraficoTendenciaDTO> obtenerEvolucionIngresos();
}
