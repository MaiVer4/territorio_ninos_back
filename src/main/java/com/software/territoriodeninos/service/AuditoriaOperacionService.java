package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditoriaOperacionService {

    /**
     * Registra de forma inmutable una transición en el ciclo de vida de un ticket (HU-38.01, HU-38.04).
     */
    void registrarTransicionTicket(
            UUID ticketUuid,
            String estadoOrigen,
            String estadoDestino,
            String motivo,
            Long usuarioId,
            String dispositivoInfo
    );

    /**
     * Consulta la línea temporal de auditoría de tickets por UUID o por documento de identidad del acudiente (HU-38.05).
     */
    List<TimelineTicketDTO> consultarTimelinePorCriterio(String criterio);

    /**
     * Consulta incidentes de parada y calcula tiempos acumulados de inactividad operativa vs técnica (HU-41.03, HU-41.04).
     */
    ResumenIncidentesAuditoriaDTO consultarIncidentes(
            LocalDateTime inicio,
            LocalDateTime fin,
            Long atraccionId,
            String tipoPausa
    );
}
