package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;
import com.software.territoriodeninos.service.AuditoriaOperacionService;
import com.software.territoriodeninos.util.FechaParametroUtil;
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
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long atraccionId,
            @RequestParam(required = false) String tipoPausa
    ) {
        LocalDateTime inicio = FechaParametroUtil.parseInicio(fechaInicio, "fechaInicio");
        LocalDateTime fin = FechaParametroUtil.parseFin(fechaFin, "fechaFin");
        ResumenIncidentesAuditoriaDTO resumen = auditoriaOperacionService.consultarIncidentes(
                inicio,
                fin,
                atraccionId,
                tipoPausa
        );
        return ResponseEntity.ok(resumen);
    }
}
