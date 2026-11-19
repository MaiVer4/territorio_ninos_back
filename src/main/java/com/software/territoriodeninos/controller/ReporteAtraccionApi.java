package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ReporteAtraccionEstadoDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionRequestDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionResponseDTO;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Reportes de Atracciones", description = "Registro y seguimiento de daños y falta de suplementos en atracciones")
public interface ReporteAtraccionApi {

    @Operation(summary = "Crear reporte de daño o falta de suplementos en una atracción")
    ResponseEntity<ReporteAtraccionResponseDTO> crear(@Valid @RequestBody ReporteAtraccionRequestDTO request, Authentication auth);

    @Operation(summary = "Listar reportes de atracciones con filtros opcionales")
    ResponseEntity<List<ReporteAtraccionResponseDTO>> listar(
            @RequestParam(required = false) Long atraccionId,
            @RequestParam(required = false) TipoReporteAtraccion tipoReporte,
            @RequestParam(required = false) EstadoReporteAtraccion estado
    );

    @Operation(summary = "Consultar detalle de un reporte de atracción")
    ResponseEntity<ReporteAtraccionResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Actualizar estado de atención/resolución de un reporte")
    ResponseEntity<ReporteAtraccionResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ReporteAtraccionEstadoDTO request,
            Authentication auth
    );
}
