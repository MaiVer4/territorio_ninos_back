package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.service.TarifaTiempoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/tarifas-tiempo", "/api/rates"})
@RequiredArgsConstructor
public class TarifaTiempoController implements TarifaTiempoApi {

    private final TarifaTiempoService tarifaTiempoService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TarifaTiempoResponseDTO> crear(@Valid @RequestBody TarifaTiempoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifaTiempoService.crear(request));
    }

    @Override
    @GetMapping({"/atraccion/{atraccionId}", "/attraction/{atraccionId}"})
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<List<TarifaTiempoResponseDTO>> obtenerPorAtraccion(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(tarifaTiempoService.obtenerPorAtraccion(atraccionId));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<TarifaTiempoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tarifaTiempoService.obtenerPorId(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TarifaTiempoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody TarifaTiempoRequestDTO request) {
        return ResponseEntity.ok(tarifaTiempoService.actualizar(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tarifaTiempoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
