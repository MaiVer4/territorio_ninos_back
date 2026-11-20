package com.software.territoriodeninos.service;

import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import com.software.territoriodeninos.util.EstadoTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simula la liberación de cupos de una atracción como una cola FIFO:
 * cada cupo físico es un "slot" con la hora en que queda libre; las boletas PAGADO en espera
 * toman los slots en orden de emisión y luego se asignan las nuevas boletas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstimadorIngresoServiceImpl implements EstimadorIngresoService {

    private static final List<EstadoSesionJuego> ESTADOS_ACTIVOS =
            List.of(EstadoSesionJuego.EN_CURSO, EstadoSesionJuego.PAUSADA);

    private final SesionJuegoRepository sesionJuegoRepository;
    private final TicketRepository ticketRepository;
    private final TarifaTiempoRepository tarifaTiempoRepository;

    @Override
    public List<LocalDateTime> estimar(Atraccion atraccion, List<Integer> duracionesMinutos, LocalDateTime ahora) {
        List<SesionJuego> activas = sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(atraccion.getId(), ESTADOS_ACTIVOS);
        List<Ticket> enEspera = obtenerEnEsperaVigentes(atraccion, ahora);
        Map<Long, Integer> duracionesMap = resolverDuracionesBatch(enEspera, Collections.emptyMap());
        return estimarConDatosCargados(atraccion, activas, enEspera, duracionesMinutos, ahora, duracionesMap);
    }

    @Override
    public LocalDateTime estimarProximoIngreso(Atraccion atraccion, LocalDateTime ahora) {
        return estimar(atraccion, List.of(duracionBase(atraccion)), ahora).get(0);
    }

    @Override
    public Map<Long, LocalDateTime> estimarProximoIngresoBatch(
            List<Atraccion> atracciones,
            LocalDateTime ahora,
            Map<Long, Integer> duracionesTarifaTiempo
    ) {
        if (atracciones == null || atracciones.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Atraccion> activasOperativas = atracciones.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getId() != null)
                .filter(a -> a.getEstado() == null || EstadoAtraccion.ACTIVO.equals(a.getEstado()))
                .toList();

        if (activasOperativas.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> atraccionIds = activasOperativas.stream().map(Atraccion::getId).toList();

        List<SesionJuego> todasSesiones = sesionJuegoRepository
                .findByAtraccionIdInAndEstadoSesionIn(atraccionIds, ESTADOS_ACTIVOS);
        Map<Long, List<SesionJuego>> sesionesPorAtraccion = todasSesiones == null
                ? Collections.emptyMap()
                : todasSesiones.stream()
                        .filter(s -> s.getAtraccion() != null && s.getAtraccion().getId() != null)
                        .collect(Collectors.groupingBy(s -> s.getAtraccion().getId()));

        List<Ticket> todosTickets = ticketRepository.findEnEsperaPorAtracciones(
                atraccionIds, EstadoTicket.PAGADO, ahora.toLocalDate().atStartOfDay()
        );
        LocalDateTime limite = ahora.minusMinutes(MINUTOS_EXPIRACION_RESERVA);
        List<Ticket> ticketsVigentes = todosTickets == null
                ? Collections.emptyList()
                : todosTickets.stream()
                        .filter(t -> t.getDetalleVenta() != null
                                && t.getDetalleVenta().getAtraccion() != null
                                && t.getDetalleVenta().getAtraccion().getId() != null)
                        .filter(t -> esTicketReservaVigente(t, limite))
                        .toList();

        Map<Long, List<Ticket>> enEsperaPorAtraccion = ticketsVigentes.stream()
                .collect(Collectors.groupingBy(t -> t.getDetalleVenta().getAtraccion().getId()));

        Map<Long, Integer> duracionesMap = resolverDuracionesBatch(ticketsVigentes, duracionesTarifaTiempo);

        Map<Long, LocalDateTime> resultado = new HashMap<>();
        for (Atraccion atr : activasOperativas) {
            List<SesionJuego> sesionesAtr = sesionesPorAtraccion.getOrDefault(atr.getId(), Collections.emptyList());
            List<Ticket> esperaAtr = enEsperaPorAtraccion.getOrDefault(atr.getId(), Collections.emptyList());
            LocalDateTime hora = estimarConDatosCargados(
                    atr, sesionesAtr, esperaAtr, List.of(duracionBase(atr)), ahora, duracionesMap
            ).get(0);
            if (hora != null) {
                resultado.put(atr.getId(), hora);
            }
        }
        return resultado;
    }

    // --- Auxiliares ---

    private List<LocalDateTime> estimarConDatosCargados(
            Atraccion atraccion,
            List<SesionJuego> activas,
            List<Ticket> enEspera,
            List<Integer> duracionesMinutos,
            LocalDateTime ahora,
            Map<Long, Integer> duracionesMap
    ) {
        int base = duracionBase(atraccion);
        PriorityQueue<LocalDateTime> slots = construirSlots(atraccion, activas, base, ahora);

        // Boletas en espera reservan cupo en orden de emisión (FIFO)
        for (Ticket ticketEnEspera : enEspera) {
            LocalDateTime inicio = max(slots.poll(), ahora);
            slots.add(inicio.plusMinutes(duracionTicket(ticketEnEspera, base, duracionesMap)));
        }

        List<LocalDateTime> resultado = new ArrayList<>(duracionesMinutos.size());
        for (Integer duracion : duracionesMinutos) {
            LocalDateTime inicio = max(slots.poll(), ahora);
            resultado.add(inicio.isAfter(ahora) ? inicio : null);
            slots.add(inicio.plusMinutes(duracion != null && duracion > 0 ? duracion : base));
        }
        return resultado;
    }

    private PriorityQueue<LocalDateTime> construirSlots(
            Atraccion atraccion,
            List<SesionJuego> activas,
            int base,
            LocalDateTime ahora
    ) {
        int capacidad = atraccion.getCapacidadMaxima() != null && atraccion.getCapacidadMaxima() > 0
                ? atraccion.getCapacidadMaxima() : 1;
        List<SesionJuego> sesiones = activas != null ? activas : Collections.emptyList();

        PriorityQueue<LocalDateTime> slots = new PriorityQueue<>();
        for (SesionJuego s : sesiones) {
            slots.add(max(horaLiberacion(s, base, ahora), ahora));
        }
        for (int i = sesiones.size(); i < capacidad; i++) {
            slots.add(ahora);
        }
        // Si hay más sesiones que capacidad, solo los primeros cupos en liberarse cuentan como slots
        if (slots.size() > capacidad) {
            LocalDateTime[] ordenados = slots.stream().sorted().toArray(LocalDateTime[]::new);
            slots.clear();
            for (int i = 0; i < capacidad; i++) {
                slots.add(ordenados[i]);
            }
        }
        return slots;
    }

    private LocalDateTime horaLiberacion(SesionJuego s, int base, LocalDateTime ahora) {
        if (EstadoSesionJuego.PAUSADA.equals(s.getEstadoSesion()) && s.getTiempoRestantePausa() != null) {
            return ahora.plusSeconds(Math.max(0, s.getTiempoRestantePausa()));
        }
        if (s.getHoraFin() != null) {
            return s.getHoraFin();
        }
        LocalDateTime inicio = s.getHoraInicio() != null ? s.getHoraInicio() : ahora;
        return inicio.plusMinutes(base);
    }

    private List<Ticket> obtenerEnEsperaVigentes(Atraccion atraccion, LocalDateTime ahora) {
        LocalDateTime limite = ahora.minusMinutes(MINUTOS_EXPIRACION_RESERVA);
        return ticketRepository.findEnEsperaPorAtraccion(atraccion.getId(), EstadoTicket.PAGADO, ahora.toLocalDate().atStartOfDay())
                .stream()
                .filter(t -> esTicketReservaVigente(t, limite))
                .toList();
    }

    private static boolean esTicketReservaVigente(Ticket t, LocalDateTime limite) {
        if (t.getHoraIngresoEstimada() == null) {
            return t.getFechaEmision() == null || !t.getFechaEmision().isBefore(limite);
        }
        return !t.getHoraIngresoEstimada().isBefore(limite);
    }

    private Map<Long, Integer> resolverDuracionesBatch(List<Ticket> tickets, Map<Long, Integer> precargadas) {
        Map<Long, Integer> duracionesMap = precargadas != null
                ? new HashMap<>(precargadas)
                : new HashMap<>();
        if (tarifaTiempoRepository == null || tickets == null || tickets.isEmpty()) {
            return duracionesMap;
        }
        Set<Long> faltantes = new HashSet<>();
        for (Ticket t : tickets) {
            Long tarifaId = t.getDetalleVenta() != null ? t.getDetalleVenta().getTarifaTiempoId() : null;
            if (tarifaId != null && !duracionesMap.containsKey(tarifaId)) {
                faltantes.add(tarifaId);
            }
        }
        if (!faltantes.isEmpty()) {
            List<TarifaTiempo> encontradas = tarifaTiempoRepository.findAllById(faltantes);
            if (encontradas != null) {
                for (TarifaTiempo tt : encontradas) {
                    if (tt.getId() != null && tt.getDuracionMinutos() != null) {
                        duracionesMap.put(tt.getId(), tt.getDuracionMinutos());
                    }
                }
            }
        }
        return duracionesMap;
    }

    private int duracionTicket(Ticket ticket, int base, Map<Long, Integer> duracionesMap) {
        Long tarifaId = ticket.getDetalleVenta() != null ? ticket.getDetalleVenta().getTarifaTiempoId() : null;
        if (tarifaId == null) {
            return base;
        }
        return duracionesMap.getOrDefault(tarifaId, base);
    }

    private static int duracionBase(Atraccion atraccion) {
        return atraccion.getDuracionMinutosBase() != null && atraccion.getDuracionMinutosBase() > 0
                ? atraccion.getDuracionMinutosBase() : 15;
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        return a.isAfter(b) ? a : b;
    }
}
