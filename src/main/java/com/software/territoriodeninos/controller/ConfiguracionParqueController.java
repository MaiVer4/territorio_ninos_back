package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.service.ConfiguracionParqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionParqueController implements ConfiguracionParqueApi {

    private final ConfiguracionParqueService configuracionParqueService;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<ConfiguracionParqueDTO> obtener() {
        return ResponseEntity.ok(configuracionParqueService.obtenerConfiguracion());
    }

    @Override
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConfiguracionParqueDTO> actualizar(@Valid @RequestBody ConfiguracionParqueDTO dto) {
        return ResponseEntity.ok(configuracionParqueService.actualizarConfiguracion(dto));
    }
}
