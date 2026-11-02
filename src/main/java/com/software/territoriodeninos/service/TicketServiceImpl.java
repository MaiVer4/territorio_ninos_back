package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TicketConsultaDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.TipoTarifa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final TicketRepository ticketRepository;
    private final TarifaTiempoRepository tarifaTiempoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TicketConsultaDTO> buscarTickets(String criterio) {
        if (criterio == null || criterio.isBlank()) {
            throw new ReglaNegocioException("El criterio de búsqueda es obligatorio (cédula de acudiente o UUID)");
        }

        String criterioLimpio = criterio.trim();
        List<Ticket> tickets = new ArrayList<>();

        if (UUID_PATTERN.matcher(criterioLimpio).matches()) {
            try {
                UUID uuid = UUID.fromString(criterioLimpio);
                Optional<Ticket> optTicket = ticketRepository.findByUuidCodigo(uuid);
                optTicket.ifPresent(tickets::add);
            } catch (IllegalArgumentException e) {
                log.warn("Criterio '{}' aparentaba ser UUID pero no pudo ser parseado", criterioLimpio);
            }
        }

        // Si no se encontró por UUID o el criterio no era UUID, buscamos por cédula de acudiente
        if (tickets.isEmpty()) {
            tickets = ticketRepository.buscarPorDocumentoAcudiente(criterioLimpio);
        }

        if (tickets.isEmpty()) {
            return Collections.emptyList();
        }

        return mapearTicketsAConsultaDTO(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketConsultaDTO obtenerTicketPorUuid(UUID uuid) {
        if (uuid == null) {
            throw new ReglaNegocioException("El UUID del ticket es obligatorio");
        }

        Ticket ticket = ticketRepository.findByUuidCodigo(uuid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + uuid));

        return mapearTicketsAConsultaDTO(List.of(ticket)).get(0);
    }

    private List<TicketConsultaDTO> mapearTicketsAConsultaDTO(List<Ticket> tickets) {
        Set<Long> tarifaTiempoIds = tickets.stream()
                .map(Ticket::getDetalleVenta)
                .filter(Objects::nonNull)
                .map(DetalleVenta::getTarifaTiempoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Integer> minutosPorTarifaId = Collections.emptyMap();
        if (!tarifaTiempoIds.isEmpty()) {
            minutosPorTarifaId = tarifaTiempoRepository.findAllById(tarifaTiempoIds).stream()
                    .collect(Collectors.toMap(
                            TarifaTiempo::getId,
                            TarifaTiempo::getDuracionMinutos,
                            (existente, reemplazo) -> existente
                    ));
        }

        final Map<Long, Integer> mapaMinutos = minutosPorTarifaId;

        return tickets.stream()
                .map(t -> construirTicketConsultaDTO(t, mapaMinutos))
                .toList();
    }

    private TicketConsultaDTO construirTicketConsultaDTO(Ticket t, Map<Long, Integer> mapaMinutos) {
        DetalleVenta dv = t.getDetalleVenta();
        Atraccion atraccion = dv != null ? dv.getAtraccion() : null;
        Venta venta = dv != null ? dv.getVenta() : null;
        Cliente cliente = venta != null ? venta.getCliente() : null;
        Menor menor = t.getMenor() != null ? t.getMenor() : (dv != null ? dv.getMenor() : null);

        Integer minutosContratados = null;
        if (dv != null && dv.getTipoTarifa() != null) {
            if (TipoTarifa.TIEMPO.equals(dv.getTipoTarifa()) && dv.getTarifaTiempoId() != null) {
                minutosContratados = mapaMinutos.get(dv.getTarifaTiempoId());
            } else if (TipoTarifa.MINUTO_BASE.equals(dv.getTipoTarifa())) {
                minutosContratados = dv.getCantidad() != null && dv.getCantidad() > 0 ? dv.getCantidad() : 1;
            }
        }

        return new TicketConsultaDTO(
                t.getUuidCodigo(),
                t.getEstadoTicket(),
                minutosContratados,
                t.getFechaEmision(),
                menor != null ? menor.getId() : null,
                menor != null ? menor.getNombreCompleto() : null,
                menor != null ? menor.getNumeroDocumento() : null,
                atraccion != null ? atraccion.getId() : null,
                atraccion != null ? atraccion.getNombre() : null,
                dv != null ? dv.getTipoTarifa() : null,
                venta != null ? venta.getId() : null,
                dv != null ? dv.getId() : null,
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getNumeroDocumento() : null,
                cliente != null ? cliente.getNombreCompleto() : null
        );
    }
}
