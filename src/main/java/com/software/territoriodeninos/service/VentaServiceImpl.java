package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import com.software.territoriodeninos.util.TipoTarifa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final MenorRepository menorRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtraccionRepository atraccionRepository;
    private final AforoValidatorService aforoValidatorService;
    private final VentaCalculoService ventaCalculoService;
    private final UUIDGeneratorService uuidGeneratorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request, Authentication auth) {
        log.info("Iniciando procesamiento transaccional de venta para cliente ID: {}", request != null ? request.clienteId() : null);

        if (request == null) {
            throw new ReglaNegocioException("La solicitud de venta no puede ser nula");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ReglaNegocioException("La venta debe incluir al menos un ítem detallado");
        }
        if (request.clienteId() == null) {
            throw new ReglaNegocioException("El ID del cliente es obligatorio");
        }
        if (request.metodoPago() == null) {
            throw new ReglaNegocioException("El método de pago es obligatorio");
        }
        if (request.montoRecibido() == null || request.montoRecibido().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El monto recibido no puede ser nulo ni negativo");
        }
        if (request.descuento() != null && request.descuento().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El descuento no puede ser negativo");
        }

        // 1. Validar Cliente
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + request.clienteId()));

        // 2. Resolver y Validar TurnoCaja Activo (Notion: CA-02)
        TurnoCaja turnoCaja = resolverTurnoCajaActivo(request.turnoCajaId(), auth);

        // 3. Validar Ítems Individuales y Preparar Liquidación
        List<ItemCotizacionDTO> itemsParaCotizar = new ArrayList<>();
        List<Atraccion> atraccionesPorItem = new ArrayList<>();
        List<Menor> menoresPorItem = new ArrayList<>();

        for (int i = 0; i < request.items().size(); i++) {
            DetalleVentaRequestDTO itemDto = request.items().get(i);
            if (itemDto == null) {
                throw new ReglaNegocioException("Los ítems de la venta no pueden ser nulos");
            }
            if (itemDto.atraccionId() == null) {
                throw new ReglaNegocioException("El ID de la atracción es obligatorio para cada ítem");
            }

            Atraccion atraccion = atraccionRepository.findById(itemDto.atraccionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + itemDto.atraccionId()));

            if (!EstadoAtraccion.ACTIVO.equals(atraccion.getEstado())) {
                throw new ReglaNegocioException("La atracción '" + atraccion.getNombre() +
                        "' no se encuentra activa (estado: " + atraccion.getEstado() + ") y no admite ventas");
            }

            Menor menor = null;
            if (itemDto.menorId() != null) {
                menor = menorRepository.findById(itemDto.menorId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Menor no encontrado con ID: " + itemDto.menorId()));
                if (menor.getCliente() == null || !menor.getCliente().getId().equals(cliente.getId())) {
                    throw new ReglaNegocioException("El menor con ID " + itemDto.menorId() +
                            " no está vinculado al cliente acudiente con ID " + cliente.getId());
                }
            }

            int cantidad = (itemDto.cantidad() != null && itemDto.cantidad() > 0) ? itemDto.cantidad() : 1;
            if (itemDto.precioUnitario() == null || itemDto.precioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                throw new ReglaNegocioException("El precio unitario no puede ser nulo ni negativo");
            }
            if (itemDto.tipoTarifa() == null) {
                throw new ReglaNegocioException("El tipo de tarifa es obligatorio para cada ítem");
            }

            if (TipoTarifa.TIEMPO.equals(itemDto.tipoTarifa()) && itemDto.tarifaTiempoId() == null) {
                throw new ReglaNegocioException("Debe especificar tarifaTiempoId para tarifas de tipo TIEMPO");
            }
            if (TipoTarifa.EVENTO.equals(itemDto.tipoTarifa()) && itemDto.tarifaEventoId() == null) {
                throw new ReglaNegocioException("Debe especificar tarifaEventoId para tarifas de tipo EVENTO");
            }

            if (TipoTarifa.MINUTO_BASE.equals(itemDto.tipoTarifa())) {
                if (itemDto.precioUnitario().compareTo(atraccion.getTarifaMinuto()) != 0) {
                    throw new ReglaNegocioException("El precio unitario enviado ($" + itemDto.precioUnitario() +
                            ") no coincide con la tarifa base por minuto de la atracción ($" + atraccion.getTarifaMinuto() + ")");
                }
                aforoValidatorService.validarCapacidadAtraccion(atraccion.getId(), cantidad);
            }

            itemsParaCotizar.add(new ItemCotizacionDTO(
                    atraccion.getId(),
                    itemDto.tarifaTiempoId(),
                    itemDto.tarifaEventoId(),
                    atraccion.getNombre(),
                    cantidad,
                    itemDto.precioUnitario(),
                    itemDto.subtotal()
            ));
            atraccionesPorItem.add(atraccion);
            menoresPorItem.add(menor);
        }

        // 4. Liquidar Financieramente la Venta con VentaCalculoService (Notion: CA-04)
        CotizacionRequestDTO cotizacionRequest = new CotizacionRequestDTO(itemsParaCotizar, null, request.descuento());
        CotizacionResponseDTO cotizacion = ventaCalculoService.cotizarVenta(cotizacionRequest);

        BigDecimal montoTotal = cotizacion.totalNeto();
        BigDecimal montoRecibido = request.montoRecibido().setScale(2, RoundingMode.HALF_UP);

        if (montoRecibido.compareTo(montoTotal) < 0) {
            throw new ReglaNegocioException("El monto recibido ($" + montoRecibido +
                    ") es inferior al total neto a pagar ($" + montoTotal + ")");
        }

        BigDecimal cambio = montoRecibido.subtract(montoTotal).setScale(2, RoundingMode.HALF_UP);

        // 5. Construir y Congelar la Venta (Notion: CA-02 y CA-03)
        Venta venta = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(montoTotal)
                .subtotal(cotizacion.subtotal())
                .descuento(cotizacion.descuento())
                .impuestos(cotizacion.impuestos())
                .montoRecibido(montoRecibido)
                .cambio(cambio)
                .metodoPago(request.metodoPago())
                .estadoVenta(EstadoVenta.PAGADO)
                .fechaVenta(LocalDateTime.now())
                .congelada(true)
                .observaciones(request.observaciones())
                .build();

        for (int i = 0; i < request.items().size(); i++) {
            DetalleVentaRequestDTO itemDto = request.items().get(i);
            Atraccion atr = atraccionesPorItem.get(i);
            Menor menor = menoresPorItem.get(i);
            int cant = (itemDto.cantidad() != null && itemDto.cantidad() > 0) ? itemDto.cantidad() : 1;
            BigDecimal subtotalLinea = cotizacion.subtotales().get(i);
            BigDecimal precioUnitario = itemDto.precioUnitario().setScale(2, RoundingMode.HALF_UP);

            DetalleVenta detalle = DetalleVenta.builder()
                    .atraccion(atr)
                    .menor(menor)
                    .tipoTarifa(itemDto.tipoTarifa())
                    .tarifaTiempoId(itemDto.tarifaTiempoId())
                    .tarifaEventoId(itemDto.tarifaEventoId())
                    .cantidad(cant)
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalLinea)
                    .build();

            venta.addDetalle(detalle);
        }

        Venta ventaGuardada = ventaRepository.save(venta);

        // 6. Generar Tickets Individuales Criptoseguros por cada unidad vendida (Notion: CA-01, CA-04)
        List<Ticket> ticketsGenerados = new ArrayList<>();
        for (DetalleVenta detalle : ventaGuardada.getDetalles()) {
            int cant = (detalle.getCantidad() != null && detalle.getCantidad() > 0) ? detalle.getCantidad() : 1;
            List<UUID> uuids = uuidGeneratorService.generarUuidsUnicos(cant);
            for (UUID uuid : uuids) {
                Ticket ticket = Ticket.builder()
                        .uuidCodigo(uuid)
                        .detalleVenta(detalle)
                        .menor(detalle.getMenor())
                        .estadoTicket(EstadoTicket.PAGADO)
                        .fechaEmision(ventaGuardada.getFechaVenta())
                        .build();
                detalle.addTicket(ticket);
                ticketsGenerados.add(ticket);
            }
        }
        List<Ticket> ticketsGuardados = ticketRepository.saveAll(ticketsGenerados);
        log.info("Se generaron y persistieron {} tickets para la venta ID #{}", ticketsGuardados.size(), ventaGuardada.getId());

        // 7. Actualizar Balance del Turno de Caja (Notion: CA-02)
        if (MetodoPago.EFECTIVO.equals(request.metodoPago())) {
            BigDecimal ventasActuales = turnoCaja.getVentasEfectivoSistema() != null
                    ? turnoCaja.getVentasEfectivoSistema()
                    : BigDecimal.ZERO;
            turnoCaja.setVentasEfectivoSistema(ventasActuales.add(montoTotal).setScale(2, RoundingMode.HALF_UP));
            turnoCajaRepository.save(turnoCaja);
            log.info("Balance de ventas en efectivo del turno #{} actualizado a ${}", turnoCaja.getId(), turnoCaja.getVentasEfectivoSistema());
        }

        log.info("Venta ID #{} procesada y congelada exitosamente con estado PAGADO por total ${}", ventaGuardada.getId(), montoTotal);
        return mapToResponseDTO(ventaGuardada, ticketsGuardados);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerVentaPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada con ID: " + id));
        List<Ticket> tickets = ticketRepository.findByDetalleVentaVentaIdOrderByFechaEmisionAsc(id);
        return mapToResponseDTO(venta, tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> obtenerVentasPorTurno(Long turnoCajaId) {
        List<Venta> ventas = ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(turnoCajaId);
        if (ventas.isEmpty()) {
            return List.of();
        }

        List<Long> ventaIds = ventas.stream().map(Venta::getId).toList();
        List<Ticket> todosLosTickets = ticketRepository.findByDetalleVentaVentaIdInOrderByFechaEmisionAsc(ventaIds);
        Map<Long, List<Ticket>> ticketsPorVentaId = todosLosTickets.stream()
                .collect(Collectors.groupingBy(t -> t.getDetalleVenta().getVenta().getId()));

        return ventas.stream()
                .map(v -> mapToResponseDTO(v, ticketsPorVentaId.getOrDefault(v.getId(), List.of())))
                .toList();
    }

    private TurnoCaja resolverTurnoCajaActivo(Long turnoCajaId, Authentication auth) {
        if (turnoCajaId != null) {
            TurnoCaja turno = turnoCajaRepository.findById(turnoCajaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado con ID: " + turnoCajaId));
            if (!EstadoTurnoCaja.ABIERTO.equals(turno.getEstado())) {
                throw new ReglaNegocioException("El turno de caja especificado (ID " + turnoCajaId + ") no se encuentra ABIERTO");
            }
            return turno;
        }

        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElse(null);
            if (usuario != null) {
                return turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), EstadoTurnoCaja.ABIERTO)
                        .orElseThrow(() -> new ReglaNegocioException("No se encontró ningún turno de caja ABIERTO para el cajero en sesión"));
            }
        }

        return turnoCajaRepository.findAllByOrderByFechaAperturaDesc().stream()
                .filter(t -> EstadoTurnoCaja.ABIERTO.equals(t.getEstado()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No se encontró ningún turno de caja ABIERTO en el sistema para asociar la venta"));
    }

    private VentaResponseDTO mapToResponseDTO(Venta v, List<Ticket> tickets) {
        List<DetalleVentaResponseDTO> detallesDTO = v.getDetalles().stream()
                .map(d -> new DetalleVentaResponseDTO(
                        d.getId(),
                        d.getAtraccion().getId(),
                        d.getAtraccion().getNombre(),
                        d.getMenor() != null ? d.getMenor().getId() : null,
                        d.getMenor() != null ? d.getMenor().getNombreCompleto() : null,
                        d.getTipoTarifa(),
                        d.getTarifaTiempoId(),
                        d.getTarifaEventoId(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()
                ))
                .toList();

        List<TicketResponseDTO> ticketsDTO = tickets != null
                ? tickets.stream().map(this::mapTicketToDTO).toList()
                : List.of();

        String nombreCompletoCliente = (v.getCliente().getNombres() != null ? v.getCliente().getNombres() : "") +
                (v.getCliente().getApellidos() != null ? " " + v.getCliente().getApellidos() : "");

        return new VentaResponseDTO(
                v.getId(),
                v.getCliente().getId(),
                v.getCliente().getNumeroDocumento(),
                nombreCompletoCliente.trim(),
                v.getTurnoCaja().getId(),
                v.getSubtotal(),
                v.getDescuento(),
                v.getImpuestos(),
                v.getMontoTotal(),
                v.getMontoRecibido(),
                v.getCambio(),
                v.getMetodoPago(),
                v.getEstadoVenta(),
                v.getFechaVenta(),
                v.getCongelada(),
                v.getObservaciones(),
                detallesDTO,
                ticketsDTO
        );
    }

    private TicketResponseDTO mapTicketToDTO(Ticket t) {
        DetalleVenta d = t.getDetalleVenta();
        return new TicketResponseDTO(
                t.getUuidCodigo(),
                d != null ? d.getId() : null,
                d != null && d.getAtraccion() != null ? d.getAtraccion().getId() : null,
                d != null && d.getAtraccion() != null ? d.getAtraccion().getNombre() : null,
                t.getMenor() != null ? t.getMenor().getId() : null,
                t.getMenor() != null ? t.getMenor().getNombreCompleto() : null,
                d != null ? d.getTipoTarifa() : null,
                t.getEstadoTicket(),
                t.getFechaEmision()
        );
    }
}
