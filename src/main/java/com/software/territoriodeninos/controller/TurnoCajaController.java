package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import com.software.territoriodeninos.service.TurnoCajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoCajaController implements TurnoCajaApi {

    private final TurnoCajaService turnoCajaService;

    @Override
    @PostMapping("/abrir")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<TurnoCajaResponseDTO> abrirTurno(
            @Valid @RequestBody AperturaCajaDTO request,
            Authentication authentication
    ) {
        TurnoCajaResponseDTO response = turnoCajaService.abrirTurno(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/activo")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<TurnoCajaResponseDTO> obtenerTurnoActivo(Authentication authentication) {
        return turnoCajaService.obtenerTurnoActivo(authentication)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Override
    @PutMapping("/cerrar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<TurnoCajaResponseDTO> cerrarTurno(
            @Valid @RequestBody CierreCajaDTO request,
            Authentication authentication
    ) {
        TurnoCajaResponseDTO response = turnoCajaService.cerrarTurno(request, authentication);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/cerrar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<TurnoCajaResponseDTO> cerrarTurnoPost(
            @Valid @RequestBody CierreCajaDTO request,
            Authentication authentication
    ) {
        return cerrarTurno(request, authentication);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO')")
    public ResponseEntity<List<TurnoCajaResponseDTO>> obtenerHistorialTurnos(
            @RequestParam(required = false) String fecha
    ) {
        List<TurnoCajaResponseDTO> response = turnoCajaService.obtenerHistorialTurnos(fecha);
        return ResponseEntity.ok(response);
    }
}
