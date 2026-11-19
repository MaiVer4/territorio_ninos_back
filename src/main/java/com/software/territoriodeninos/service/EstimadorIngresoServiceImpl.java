package com.software.territoriodeninos.service;

import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import com.software.territoriodeninos.util.EstadoTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

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
        int base = duracionBase(atraccion);
        PriorityQueue<LocalDateTime> slots = construirSlots(atraccion, base, ahora);

        // Boletas en espera reservan cupo en orden de emisión (FIFO)
        for (Ticket enEspera : obtenerEnEsperaVigentes(atraccion, ahora)) {
            LocalDateTime inicio = max(slots.poll(), ahora);
            slots.add(inicio.plusMinutes(duracionTicket(enEspera, base)));
        }

        List<LocalDateTime> resultado = new ArrayList<>(duracionesMinutos.size());
        for (Integer duracion : duracionesMinutos) {
            LocalDateTime inicio = max(slots.poll(), ahora);
            resultado.add(inicio.isAfter(ahora) ? inicio : null);
            slots.add(inicio.plusMinutes(duracion != null && duracion > 0 ? duracion : base));
        }
        return resultado;
    }

    @Override
    public LocalDateTime estimarProximoIngreso(Atraccion atraccion, LocalDateTime ahora) {
        return estimar(atraccion, List.of(duracionBase(atraccion)), ahora).get(0);
    }

    // --- Auxiliares ---

    private PriorityQueue<LocalDateTime> construirSlots(Atraccion atraccion, int base, LocalDateTime ahora) {
        int capacidad = atraccion.getCapacidadMaxima() != null && atraccion.getCapacidadMaxima() > 0
                ? atraccion.getCapacidadMaxima() : 1;
        List<SesionJuego> activas = sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(atraccion.getId(), ESTADOS_ACTIVOS);

        PriorityQueue<LocalDateTime> slots = new PriorityQueue<>();
        for (SesionJuego s : activas) {
            slots.add(max(horaLiberacion(s, base, ahora), ahora));
        }
        for (int i = activas.size(); i < capacidad; i++) {
            slots.add(ahora);
        }
        // Si hay más sesiones que capacidad, solo los primeros cupos en liberarse cuentan como slots
        while (slots.size() > capacidad) {
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
                .filter(t -> t.getHoraIngresoEstimada() == null
                        ? !t.getFechaEmision().isBefore(limite)
                        : !t.getHoraIngresoEstimada().isBefore(limite))
                .toList();
    }

    private int duracionTicket(Ticket ticket, int base) {
        Long tarifaId = ticket.getDetalleVenta() != null ? ticket.getDetalleVenta().getTarifaTiempoId() : null;
        if (tarifaId == null || tarifaTiempoRepository == null) {
            return base;
        }
        return tarifaTiempoRepository.findById(tarifaId)
                .map(TarifaTiempo::getDuracionMinutos)
                .filter(Objects::nonNull)
                .orElse(base);
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
