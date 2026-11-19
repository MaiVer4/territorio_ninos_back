package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ReporteAtraccionEstadoDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionRequestDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionResponseDTO;
import com.software.territoriodeninos.service.ReporteAtraccionService;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes-atraccion")
@RequiredArgsConstructor
public class ReporteAtraccionController implements ReporteAtraccionApi {

    private final ReporteAtraccionService reporteAtraccionService;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ReporteAtraccionResponseDTO> crear(
            @Valid @RequestBody ReporteAtraccionRequestDTO request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteAtraccionService.crear(request, auth));
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<ReporteAtraccionResponseDTO>> listar(
            @RequestParam(required = false) Long atraccionId,
            @RequestParam(required = false) TipoReporteAtraccion tipoReporte,
            @RequestParam(required = false) EstadoReporteAtraccion estado
    ) {
        return ResponseEntity.ok(reporteAtraccionService.listar(atraccionId, tipoReporte, estado));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ReporteAtraccionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reporteAtraccionService.obtenerPorId(id));
    }

    @Override
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ReporteAtraccionResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ReporteAtraccionEstadoDTO request,
            Authentication auth
    ) {
        return ResponseEntity.ok(reporteAtraccionService.actualizarEstado(id, request, auth));
    }
}
