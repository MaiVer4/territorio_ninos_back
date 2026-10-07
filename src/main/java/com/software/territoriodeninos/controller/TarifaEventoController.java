package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.service.TarifaEventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/tarifas-evento", "/api/event-rates"})
@RequiredArgsConstructor
public class TarifaEventoController implements TarifaEventoApi {

    private final TarifaEventoService tarifaEventoService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TarifaEventoResponseDTO> crear(@Valid @RequestBody TarifaEventoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifaEventoService.crear(request));
    }

    @Override
    @GetMapping({"/atraccion/{atraccionId}", "/attraction/{atraccionId}"})
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<List<TarifaEventoResponseDTO>> obtenerPorAtraccion(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(tarifaEventoService.obtenerPorAtraccion(atraccionId));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<TarifaEventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tarifaEventoService.obtenerPorId(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TarifaEventoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody TarifaEventoRequestDTO request) {
        return ResponseEntity.ok(tarifaEventoService.actualizar(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tarifaEventoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
