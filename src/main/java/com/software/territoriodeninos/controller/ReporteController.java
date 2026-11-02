package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.DetalleTurnoAuditadoDTO;
import com.software.territoriodeninos.dto.GraficoTendenciaDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;
import com.software.territoriodeninos.service.ConciliacionTurnoService;
import com.software.territoriodeninos.service.EstadisticasReporteService;
import com.software.territoriodeninos.service.ExportacionReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController implements ReporteApi {

    private final ConciliacionTurnoService conciliacionTurnoService;
    private final ExportacionReporteService exportacionReporteService;
    private final EstadisticasReporteService estadisticasReporteService;

    @Override
    @GetMapping("/turnos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<List<ReporteTurnoDTO>> obtenerReporteTurnos(
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin,
            @RequestParam(required = false) Long usuarioId
    ) {
        List<ReporteTurnoDTO> reporte = conciliacionTurnoService.obtenerReporteTurnos(fechaInicio, fechaFin, usuarioId);
        return ResponseEntity.ok(reporte);
    }

    @Override
    @GetMapping("/turnos/{id}/detalle")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<DetalleTurnoAuditadoDTO> obtenerDetalleTurno(
            @PathVariable Long id
    ) {
        DetalleTurnoAuditadoDTO detalle = conciliacionTurnoService.obtenerDetalleTurno(id);
        return ResponseEntity.ok(detalle);
    }

    @Override
    @GetMapping("/rotacion-atracciones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<List<RotacionAtraccionDTO>> obtenerRotacionAtracciones(
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin,
            @RequestParam(required = false, defaultValue = "MENSUAL") String periodo,
            @RequestParam(required = false, defaultValue = "USOS") String criterioOrden
    ) {
        List<RotacionAtraccionDTO> rotacion = conciliacionTurnoService.obtenerRotacionAtracciones(
                fechaInicio,
                fechaFin,
                periodo,
                criterioOrden
        );
        return ResponseEntity.ok(rotacion);
    }

    @Override
    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false, defaultValue = "TURNOS") String tipoReporte,
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long atraccionId
    ) {
        byte[] pdfBytes = exportacionReporteService.exportarPdf(tipoReporte, fechaInicio, fechaFin, usuarioId, atraccionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("reporte_" + tipoReporte.toLowerCase() + ".pdf")
                .build());
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Override
    @GetMapping("/exportar/csv")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false, defaultValue = "TURNOS") String tipoReporte,
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long atraccionId
    ) {
        byte[] csvBytes = exportacionReporteService.exportarCsv(tipoReporte, fechaInicio, fechaFin, usuarioId, atraccionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("reporte_" + tipoReporte.toLowerCase() + ".csv")
                .build());
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @Override
    @GetMapping("/graficos/horas-pico")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<GraficoTendenciaDTO> obtenerHorasPico(
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin
    ) {
        GraficoTendenciaDTO grafico = estadisticasReporteService.obtenerHorasPico(fechaInicio, fechaFin);
        return ResponseEntity.ok(grafico);
    }

    @Override
    @GetMapping("/graficos/metodos-pago")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<GraficoTendenciaDTO> obtenerMetodosPago(
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin
    ) {
        GraficoTendenciaDTO grafico = estadisticasReporteService.obtenerDistribucionMetodosPago(fechaInicio, fechaFin);
        return ResponseEntity.ok(grafico);
    }

    @Override
    @GetMapping("/graficos/evolucion-ingresos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<GraficoTendenciaDTO> obtenerEvolucionIngresos() {
        GraficoTendenciaDTO grafico = estadisticasReporteService.obtenerEvolucionIngresosUltimos7Dias();
        return ResponseEntity.ok(grafico);
    }
}
