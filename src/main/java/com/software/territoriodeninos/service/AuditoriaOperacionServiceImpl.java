package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.IncidenteDetalleDTO;
import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;
import com.software.territoriodeninos.entity.AuditoriaTicket;
import com.software.territoriodeninos.entity.LogIncidentePausa;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AuditoriaTicketRepository;
import com.software.territoriodeninos.repository.LogIncidentePausaRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaOperacionServiceImpl implements AuditoriaOperacionService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditoriaTicketRepository auditoriaTicketRepository;
    private final LogIncidentePausaRepository logIncidentePausaRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarTransicionTicket(
            UUID ticketUuid,
            String estadoOrigen,
            String estadoDestino,
            String motivo,
            Long usuarioId,
            String dispositivoInfo
    ) {
        AuditoriaTicket registro = AuditoriaTicket.builder()
                .ticketUuid(ticketUuid)
                .estadoOrigen(estadoOrigen)
                .estadoDestino(estadoDestino)
                .motivo(motivo)
                .usuarioId(usuarioId)
                .dispositivoInfo(dispositivoInfo)
                .fechaEvento(LocalDateTime.now())
                .build();

        try {
            auditoriaTicketRepository.save(registro);
        } catch (Exception ex) {
            log.warn("Fallo transitorio al guardar auditoría de ticket {} ({} -> {}), reintentando...",
                    ticketUuid, estadoOrigen, estadoDestino);
            try {
                auditoriaTicketRepository.saveAndFlush(registro);
            } catch (Exception retryEx) {
                log.error("AUDITORIA_FALLIDA [ticket={}, origen={}, destino={}, usuario={}, motivo={}]: {}",
                        ticketUuid, estadoOrigen, estadoDestino, usuarioId, motivo, retryEx.getMessage(), retryEx);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineTicketDTO> consultarTimelinePorCriterio(String criterio) {
        if (criterio == null || criterio.isBlank()) {
            throw new IllegalArgumentException("El criterio de búsqueda de ticket no puede ser vacío.");
        }

        UUID ticketUuid = null;
        try {
            ticketUuid = UUID.fromString(criterio.trim());
        } catch (IllegalArgumentException ignored) {
        }

        List<Ticket> tickets;
        if (ticketUuid != null) {
            Ticket ticket = ticketRepository.findByUuidCodigo(ticketUuid)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró ningún ticket con UUID: " + criterio));
            tickets = List.of(ticket);
        } else {
            tickets = ticketRepository.findByDetalleVentaVentaClienteNumeroDocumentoOrderByFechaEmisionDesc(criterio.trim());
            if (tickets.isEmpty()) {
                throw new RecursoNoEncontradoException("No se encontraron boletas asociadas al documento del acudiente: " + criterio);
            }
        }

        Map<Long, String> usuariosCache = new HashMap<>();

        return tickets.stream().map(ticket -> {
            List<AuditoriaTicket> auditorias = auditoriaTicketRepository.findByTicketUuidOrderByFechaEventoAsc(ticket.getUuidCodigo());

            List<TimelineTicketDTO.EventoTimelineTicketDTO> eventos = auditorias.stream().map(a -> {
                String operarioNombre = "Sistema Automático";
                if (a.getUsuarioId() != null) {
                    operarioNombre = usuariosCache.computeIfAbsent(a.getUsuarioId(), id ->
                            usuarioRepository.findById(id).map(Usuario::getNombre).orElse("Operario #" + id)
                    );
                }

                return TimelineTicketDTO.EventoTimelineTicketDTO.builder()
                        .id(a.getId())
                        .estadoOrigen(a.getEstadoOrigen())
                        .estadoDestino(a.getEstadoDestino())
                        .motivo(a.getMotivo())
                        .fechaEvento(a.getFechaEvento())
                        .fechaLegible(a.getFechaEvento() != null ? a.getFechaEvento().format(FORMATTER) : null)
                        .usuarioId(a.getUsuarioId())
                        .usuarioNombre(operarioNombre)
                        .dispositivoInfo(a.getDispositivoInfo())
                        .build();
            }).collect(Collectors.toList());

            String atraccionNombre = "N/A";
            String clienteNombre = "N/A";
            String clienteDocumento = "N/A";
            String menorNombre = "Sin menor asociado";

            if (ticket.getDetalleVenta() != null) {
                if (ticket.getDetalleVenta().getAtraccion() != null) {
                    atraccionNombre = ticket.getDetalleVenta().getAtraccion().getNombre();
                }
                if (ticket.getDetalleVenta().getVenta() != null && ticket.getDetalleVenta().getVenta().getCliente() != null) {
                    clienteNombre = ticket.getDetalleVenta().getVenta().getCliente().getNombreCompleto();
                    clienteDocumento = ticket.getDetalleVenta().getVenta().getCliente().getNumeroDocumento();
                }
            }
            if (ticket.getMenor() != null) {
                menorNombre = ticket.getMenor().getNombreCompleto();
            }

            return TimelineTicketDTO.builder()
                    .ticketUuid(ticket.getUuidCodigo())
                    .atraccionNombre(atraccionNombre)
                    .clienteNombre(clienteNombre)
                    .clienteDocumento(clienteDocumento)
                    .menorNombre(menorNombre)
                    .estadoActual(ticket.getEstadoTicket() != null ? ticket.getEstadoTicket().name() : null)
                    .fechaEmision(ticket.getFechaEmision())
                    .fechaConsumo(ticket.getFechaConsumo())
                    .eventos(eventos)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenIncidentesAuditoriaDTO consultarIncidentes(
            LocalDateTime inicio,
            LocalDateTime fin,
            Long atraccionId,
            String tipoPausa
    ) {
        LocalDateTime fechaFin = fin != null ? fin : LocalDateTime.now();
        LocalDateTime fechaInicio = inicio != null ? inicio : fechaFin.minusDays(30);

        List<LogIncidentePausa> logs = logIncidentePausaRepository.findIncidentesConFiltros(
                fechaInicio,
                fechaFin,
                atraccionId,
                tipoPausa
        );

        long inactividadOperativaSeg = 0L;
        long inactividadTecnicaSeg = 0L;
        long inactividadTotalSeg = 0L;

        List<IncidenteDetalleDTO> incidentes = new ArrayList<>();

        for (LogIncidentePausa log : logs) {
            int duracionSeg = 0;
            if (log.getDuracionSegundos() != null) {
                duracionSeg = log.getDuracionSegundos();
            } else if (log.getFechaFinPausa() != null) {
                duracionSeg = (int) Duration.between(log.getFechaInicioPausa(), log.getFechaFinPausa()).getSeconds();
            } else {
                duracionSeg = (int) Duration.between(log.getFechaInicioPausa(), LocalDateTime.now()).getSeconds();
            }

            double duracionMin = BigDecimal.valueOf(duracionSeg / 60.0).setScale(2, RoundingMode.HALF_UP).doubleValue();

            String tipo = log.getTipoPausa() != null ? log.getTipoPausa().toUpperCase() : "OPERATIVA";
            if (tipo.contains("TECNIC") || tipo.contains("MANTENIMIENTO")) {
                inactividadTecnicaSeg += duracionSeg;
            } else {
                inactividadOperativaSeg += duracionSeg;
            }
            inactividadTotalSeg += duracionSeg;

            Long aId = null;
            String atraccionNombre = "N/A";
            if (log.getAtraccion() != null) {
                aId = log.getAtraccion().getId();
                atraccionNombre = log.getAtraccion().getNombre();
            } else if (log.getSesionJuego() != null && log.getSesionJuego().getAtraccion() != null) {
                aId = log.getSesionJuego().getAtraccion().getId();
                atraccionNombre = log.getSesionJuego().getAtraccion().getNombre();
            }

            Long opId = log.getOperario() != null ? log.getOperario().getId() : null;
            String operarioNombre = log.getOperario() != null ? log.getOperario().getNombre() : "Operario no asignado";

            incidentes.add(IncidenteDetalleDTO.builder()
                    .incidenteId(log.getId())
                    .atraccionId(aId)
                    .atraccionNombre(atraccionNombre)
                    .tipoPausa(tipo)
                    .motivo(log.getMotivo())
                    .esPausaGeneral(log.getEsPausaGeneral())
                    .operarioId(opId)
                    .operarioNombre(operarioNombre)
                    .fechaInicioPausa(log.getFechaInicioPausa())
                    .fechaFinPausa(log.getFechaFinPausa())
                    .duracionSegundos(duracionSeg)
                    .duracionMinutos(duracionMin)
                    .build());
        }

        double totalMinutos = BigDecimal.valueOf(inactividadTotalSeg / 60.0).setScale(2, RoundingMode.HALF_UP).doubleValue();

        return ResumenIncidentesAuditoriaDTO.builder()
                .totalIncidentes(incidentes.size())
                .tiempoTotalInactividadSegundos(inactividadTotalSeg)
                .tiempoTotalInactividadMinutos(totalMinutos)
                .inactividadOperativaSegundos(inactividadOperativaSeg)
                .inactividadTecnicaSegundos(inactividadTecnicaSeg)
                .incidentes(incidentes)
                .build();
    }
}
