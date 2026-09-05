package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import com.software.territoriodeninos.service.AtraccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atracciones")
@RequiredArgsConstructor
public class AtraccionController implements AtraccionApi {

    private final AtraccionService atraccionService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AtraccionResponseDTO> crear(@Valid @RequestBody AtraccionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atraccionService.crear(request));
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO')")
    public ResponseEntity<List<AtraccionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(atraccionService.obtenerTodas());
    }

    @Override
    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<List<AtraccionResponseDTO>> obtenerActivas() {
        return ResponseEntity.ok(atraccionService.obtenerActivas());
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<AtraccionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(atraccionService.obtenerPorId(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AtraccionResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody AtraccionRequestDTO request) {
        return ResponseEntity.ok(atraccionService.actualizar(id, request));
    }

    @Override
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AtraccionResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(atraccionService.cambiarEstado(id, estado));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        atraccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
