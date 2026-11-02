package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.ReimpresionRequestDTO;
import com.software.territoriodeninos.dto.TicketPrintDataDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.NombreRol;
import com.software.territoriodeninos.util.TicketPrintFormatter;
import com.software.territoriodeninos.util.TipoTarifa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private final LogReimpresionRepository logReimpresionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final AuditoriaOperacionService auditoriaOperacionService;

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
                .esReimpresion(false)
                .build();

        ConfiguracionParqueDTO config = configuracionParqueService.obtenerConfiguracion();

        log.info("Generando payload térmico ESC/POS para ticket UUID: {}", uuid);
        return ticketPrintFormatter.formatear(printData, config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String reimprimirTicket(UUID uuid, ReimpresionRequestDTO request, Authentication auth) {
        if (uuid == null) {
            throw new ReglaNegocioException("El UUID del ticket es obligatorio");
        }
        if (request == null || !StringUtils.hasText(request.motivo())) {
            throw new ReglaNegocioException("El motivo de reimpresión es obligatorio");
        }

        Ticket ticket = ticketRepository.findByUuidCodigo(uuid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + uuid));

        // 1. Validar que el ticket no se encuentre en estado CONSUMIDO (Notion: CA-04 -> HTTP 409)
        if (EstadoTicket.CONSUMIDO.equals(ticket.getEstadoTicket())) {
            throw new ConflictoException("No es posible reimprimir el ticket porque ya se encuentra en estado CONSUMIDO.");
        }
        if (EstadoTicket.ANULADO.equals(ticket.getEstadoTicket())) {
            throw new ConflictoException("No es posible reimprimir el ticket porque se encuentra ANULADO.");
        }

        // 2. Validar que la boleta pertenezca a una venta del turno activo (Notion: CA-01)
        DetalleVenta detalle = ticket.getDetalleVenta();
        Venta venta = detalle != null ? detalle.getVenta() : null;
        TurnoCaja turnoVenta = venta != null ? venta.getTurnoCaja() : null;

        if (turnoVenta == null || !EstadoTurnoCaja.ABIERTO.equals(turnoVenta.getEstado())) {
            throw new ConflictoException("La boleta no pertenece a un turno de caja actualmente activo (ABIERTO).");
        }

        Usuario usuarioAutenticado = resolverUsuarioAutenticado(auth);
        if (usuarioAutenticado == null) {
            throw new ReglaNegocioException("No se encontró el usuario en sesión para registrar la auditoría de reimpresión.");
        }

        // Si el usuario es cajero (TAQUILLERO), verificar que la venta corresponda a su propio turno activo
        if (usuarioAutenticado.getRol() != null && NombreRol.TAQUILLERO.equals(usuarioAutenticado.getRol().getNombre())) {
            TurnoCaja turnoCajero = turnoCajaRepository
                    .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioAutenticado.getId(), EstadoTurnoCaja.ABIERTO)
                    .orElse(null);
            if (turnoCajero == null || !turnoCajero.getId().equals(turnoVenta.getId())) {
                throw new ConflictoException("La boleta pertenece al turno #" + turnoVenta.getId() +
                        ", el cual no coincide con el turno activo del cajero en sesión.");
            }
        }

        // 3. Registrar auditoría inmutable en log_reimpresion y auditoria_tickets (Notion: CA-03, HU-38.04)
        LogReimpresion logReimpresion = LogReimpresion.builder()
                .ticket(ticket)
                .usuario(usuarioAutenticado)
                .motivo(request.motivo().trim())
                .fechaReimpresion(LocalDateTime.now())
                .build();
        logReimpresionRepository.save(logReimpresion);

        auditoriaOperacionService.registrarTransicionTicket(
                ticket.getUuidCodigo(),
                ticket.getEstadoTicket() != null ? ticket.getEstadoTicket().name() : "PAGADO",
                ticket.getEstadoTicket() != null ? ticket.getEstadoTicket().name() : "PAGADO",
                "Reimpresión: " + request.motivo().trim(),
                usuarioAutenticado.getId(),
                "Terminal Impresión"
        );

        // 4. Formatear con marca visible ** REIMPRESIÓN ** (Notion: CA-02)
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
                .esReimpresion(true)
                .build();

        ConfiguracionParqueDTO config = configuracionParqueService.obtenerConfiguracion();

        log.info("Ticket UUID {} reimpreso exitosamente por usuario {} con motivo: {}",
                uuid, usuarioAutenticado.getCorreo(), request.motivo().trim());

        return ticketPrintFormatter.formatear(printData, config);
    }

    private Usuario resolverUsuarioAutenticado(Authentication auth) {
        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
        }
        return null;
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
