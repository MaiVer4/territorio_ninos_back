package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;
import com.software.territoriodeninos.service.AuditoriaOperacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController implements AuditoriaApi {

    private final AuditoriaOperacionService auditoriaOperacionService;

    @Override
    @GetMapping("/tickets/{criterio}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<List<TimelineTicketDTO>> consultarTimelineTicket(
            @PathVariable String criterio
    ) {
        List<TimelineTicketDTO> timeline = auditoriaOperacionService.consultarTimelinePorCriterio(criterio);
        return ResponseEntity.ok(timeline);
    }

    @Override
    @GetMapping("/incidentes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
    public ResponseEntity<ResumenIncidentesAuditoriaDTO> consultarIncidentes(
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin,
            @RequestParam(required = false) Long atraccionId,
            @RequestParam(required = false) String tipoPausa
    ) {
        ResumenIncidentesAuditoriaDTO resumen = auditoriaOperacionService.consultarIncidentes(
                fechaInicio,
                fechaFin,
                atraccionId,
                tipoPausa
        );
        return ResponseEntity.ok(resumen);
    }
}
