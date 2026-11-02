package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.TicketPrintDataDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.DetalleVenta;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.TicketPrintFormatter;
import com.software.territoriodeninos.util.TipoTarifa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPrintServiceImpl implements TicketPrintService {

    private final TicketRepository ticketRepository;
    private final ConfiguracionParqueService configuracionParqueService;
    private final TicketPrintFormatter ticketPrintFormatter;
    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final TarifaEventoRepository tarifaEventoRepository;

    @Override
    @Transactional(readOnly = true)
    public String generarPayloadImpresion(UUID uuid) {
        if (uuid == null) {
            throw new ReglaNegocioException("El UUID del ticket es obligatorio");
        }

        Ticket ticket = ticketRepository.findByUuidCodigo(uuid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + uuid));

        DetalleVenta detalle = ticket.getDetalleVenta();
        Atraccion atraccion = detalle != null ? detalle.getAtraccion() : null;

        String nombreAtraccion = atraccion != null ? atraccion.getNombre() : "Atracción TDN";
        String tiempoOModalidad = resolverTiempoOModalidad(detalle, atraccion);
        String nombreMenor = resolverNombreMenor(ticket, detalle);

        TicketPrintDataDTO printData = TicketPrintDataDTO.builder()
                .uuidTicket(ticket.getUuidCodigo())
                .nombreAtraccion(nombreAtraccion)
                .nombreMenor(nombreMenor)
                .tiempoOModalidad(tiempoOModalidad)
                .precioUnitario(detalle != null ? detalle.getPrecioUnitario() : null)
                .fechaEmision(ticket.getFechaEmision())
                .build();

        ConfiguracionParqueDTO config = configuracionParqueService.obtenerConfiguracion();

        log.info("Generando payload térmico ESC/POS para ticket UUID: {}", uuid);
        return ticketPrintFormatter.formatear(printData, config);
    }

    private String resolverTiempoOModalidad(DetalleVenta detalle, Atraccion atraccion) {
        if (detalle == null) {
            return "Estándar";
        }

        TipoTarifa tipo = detalle.getTipoTarifa();
        if (TipoTarifa.TIEMPO.equals(tipo)) {
            if (detalle.getTarifaTiempoId() != null) {
                return tarifaTiempoRepository.findById(detalle.getTarifaTiempoId())
                        .map(t -> t.getDuracionMinutos() + " min")
                        .orElseGet(() -> (atraccion != null && atraccion.getDuracionMinutosBase() != null
                                ? atraccion.getDuracionMinutosBase() + " min"
                                : "Tiempo"));
            }
            return (atraccion != null && atraccion.getDuracionMinutosBase() != null)
                    ? atraccion.getDuracionMinutosBase() + " min"
                    : "Tiempo";
        }

        if (TipoTarifa.MINUTO_BASE.equals(tipo)) {
            return (atraccion != null && atraccion.getDuracionMinutosBase() != null)
                    ? atraccion.getDuracionMinutosBase() + " min (Base)"
                    : "Minuto Base";
        }

        if (TipoTarifa.EVENTO.equals(tipo)) {
            if (detalle.getTarifaEventoId() != null) {
                return tarifaEventoRepository.findById(detalle.getTarifaEventoId())
                        .map(e -> e.getIntentos() + " intentos")
                        .orElseGet(() -> (atraccion != null && atraccion.getCaidasLimite() != null
                                ? atraccion.getCaidasLimite() + " caídas"
                                : "Eventos / Caídas"));
            }
            return (atraccion != null && atraccion.getCaidasLimite() != null)
                    ? atraccion.getCaidasLimite() + " caídas"
                    : "Eventos / Caídas";
        }

        return tipo != null ? tipo.name() : "Estándar";
    }

    private String resolverNombreMenor(Ticket ticket, DetalleVenta detalle) {
        if (ticket.getMenor() != null && ticket.getMenor().getNombreCompleto() != null) {
            return ticket.getMenor().getNombreCompleto().trim();
        }
        if (detalle != null && detalle.getMenor() != null && detalle.getMenor().getNombreCompleto() != null) {
            return detalle.getMenor().getNombreCompleto().trim();
        }
        return "Sin menor asignado";
    }
}
