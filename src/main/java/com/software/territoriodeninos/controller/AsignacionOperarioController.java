package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;
import com.software.territoriodeninos.service.AsignacionOperarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/asignaciones", "/api/assignments"})
@RequiredArgsConstructor
public class AsignacionOperarioController implements AsignacionOperarioApi {

    private final AsignacionOperarioService asignacionOperarioService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionResponseDTO> asignar(@Valid @RequestBody AsignacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionOperarioService.asignar(request));
    }

    @Override
    @PostMapping("/reasignar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionResponseDTO> reasignar(@Valid @RequestBody ReasignacionRequestDTO request) {
        return ResponseEntity.ok(asignacionOperarioService.reasignar(request));
    }

    @Override
    @PatchMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionResponseDTO> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(asignacionOperarioService.finalizar(id));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionResponseDTO> liberar(@PathVariable Long id) {
        return ResponseEntity.ok(asignacionOperarioService.finalizar(id));
    }

    @Override
    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO')")
    public ResponseEntity<List<AsignacionResponseDTO>> obtenerActivas() {
        return ResponseEntity.ok(asignacionOperarioService.obtenerActivas());
    }

    @Override
    @GetMapping({"/operario/{usuarioId}/activa", "/operator/{usuarioId}/current"})
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<AsignacionResponseDTO> obtenerActivaPorOperario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(asignacionOperarioService.obtenerActivaPorOperario(usuarioId));
    }

    @Override
    @GetMapping("/atraccion/{atraccionId}/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<List<AsignacionResponseDTO>> obtenerActivasPorAtraccion(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(asignacionOperarioService.obtenerActivasPorAtraccion(atraccionId));
    }

    @Override
    @GetMapping("/operario/{usuarioId}/historial")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AsignacionResponseDTO>> obtenerHistorialPorOperario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(asignacionOperarioService.obtenerHistorialPorOperario(usuarioId));
    }
}
