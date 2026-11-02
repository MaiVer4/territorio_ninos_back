package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.EstacionTrabajoDTO;
import com.software.territoriodeninos.service.OperacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operario")
@RequiredArgsConstructor
public class OperarioEstacionController implements OperarioEstacionApi {

    private final OperacionService operacionService;

    @Override
    @GetMapping("/estacion-activa")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<EstacionTrabajoDTO> obtenerEstacionActiva(Authentication authentication) {
        return ResponseEntity.ok(operacionService.obtenerEstacionActiva(authentication));
    }
}
