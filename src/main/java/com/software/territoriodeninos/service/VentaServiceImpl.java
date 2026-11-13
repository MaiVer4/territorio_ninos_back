package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;
import com.software.territoriodeninos.dto.DetalleVentaRequestDTO;
import com.software.territoriodeninos.dto.DetalleVentaResponseDTO;
import com.software.territoriodeninos.dto.ItemCotizacionDTO;
import com.software.territoriodeninos.dto.TicketResponseDTO;
import com.software.territoriodeninos.dto.VentaProcesarRequestDTO;
import com.software.territoriodeninos.dto.VentaResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.DetalleVenta;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.ClienteRepository;
import com.software.territoriodeninos.repository.MenorRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.repository.TurnoCajaRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.repository.VentaRepository;
import com.software.territoriodeninos.security.AuthenticatedUserProvider;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import com.software.territoriodeninos.util.NombreRol;
import com.software.territoriodeninos.util.TipoTarifa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación de la lógica de negocio para la gestión y procesamiento de ventas.
 * Aplica principios SOLID: alta cohesión, métodos especializados y bajo acoplamiento.
 */
@Slf4j
@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final MenorRepository menorRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtraccionRepository atraccionRepository;
    private final AuditoriaOperacionService auditoriaOperacionService;
    private final AforoValidatorService aforoValidatorService;
    private final VentaCalculoService ventaCalculoService;
    private final UUIDGeneratorService uuidGeneratorService;
    private final AtraccionService atraccionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Autowired
    public VentaServiceImpl(
            VentaRepository ventaRepository,
            TicketRepository ticketRepository,
            ClienteRepository clienteRepository,
            MenorRepository menorRepository,
            TurnoCajaRepository turnoCajaRepository,
            UsuarioRepository usuarioRepository,
            AtraccionRepository atraccionRepository,
            AuditoriaOperacionService auditoriaOperacionService,
            AforoValidatorService aforoValidatorService,
            VentaCalculoService ventaCalculoService,
            UUIDGeneratorService uuidGeneratorService,
            AtraccionService atraccionService,
            @Autowired(required = false) AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.ventaRepository = ventaRepository;
        this.ticketRepository = ticketRepository;
        this.clienteRepository = clienteRepository;
        this.menorRepository = menorRepository;
        this.turnoCajaRepository = turnoCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.atraccionRepository = atraccionRepository;
        this.auditoriaOperacionService = auditoriaOperacionService;
        this.aforoValidatorService = aforoValidatorService;
        this.ventaCalculoService = ventaCalculoService;
        this.uuidGeneratorService = uuidGeneratorService;
        this.atraccionService = atraccionService;
        this.authenticatedUserProvider = authenticatedUserProvider != null
                ? authenticatedUserProvider
                : new AuthenticatedUserProvider(usuarioRepository);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request, Authentication auth) {
        log.info("Iniciando procesamiento transaccional de venta para cliente ID: {}", request != null ? request.clienteId() : null);

        // 1. Validaciones estructurales y de autorización de la solicitud
        validarSolicitudVenta(request, auth);

        // 2. Validación de cliente y turno de caja
        Cliente cliente = obtenerCliente(request.clienteId());
        TurnoCaja turnoCaja = resolverTurnoCajaActivo(request.turnoCajaId(), auth);

        // 3. Validación de aforo acumulado por atracción
        validarAforoAcumulado(request.items());

        // 4. Validación individual de ítems y preparación para cotización
        ResultadoValidacionItems resultadoItems = procesarYValidarItems(request.items(), cliente);

        // 5. Liquidación financiera y cálculo de cambio
        CotizacionResponseDTO cotizacion = liquidarCotizacion(resultadoItems.itemsParaCotizar(), request.descuento());
        BigDecimal montoTotal = cotizacion.totalNeto();
        BigDecimal cambio = validarYCalcularCambio(request.montoRecibido(), montoTotal);

        // 6. Construcción y persistencia de la Venta congelada
        Venta ventaGuardada = persistirVenta(request, cliente, turnoCaja, cotizacion, cambio, resultadoItems);

        // 7. Generación y auditoría inmutable de tickets criptoseguros
        List<Ticket> ticketsGuardados = generarYAuditarTickets(ventaGuardada, turnoCaja);

        // 8. Actualizaciones colaterales (balance de caja y caché de aforo)
        actualizarBalanceCajaSiAplica(turnoCaja, request.metodoPago(), montoTotal);
        invalidarCacheAforo();

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

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listarVentasPorTurno(Long turnoId, Authentication auth) {
        if (turnoId == null) {
            throw new ReglaNegocioException("El ID del turno de caja es obligatorio");
        }

        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado con ID: " + turnoId));

        Usuario usuarioAutenticado = resolverUsuarioEnSesion(auth);
        if (usuarioAutenticado != null && usuarioAutenticado.getRol() != null
                && NombreRol.TAQUILLERO.equals(usuarioAutenticado.getRol().getNombre())
                && !turno.getUsuarioId().equals(usuarioAutenticado.getId())) {
            throw new AccessDeniedException("No tiene permisos para ver las ventas de otro cajero.");
        }

        return obtenerVentasPorTurno(turnoId);
    }

    // =========================================================================
    // Métodos auxiliares de descomposición (SRP - Responsabilidad Única)
    // =========================================================================

    private void validarSolicitudVenta(VentaProcesarRequestDTO request, Authentication auth) {
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
        // V-07: Restricción de descuentos manuales exclusivamente al rol ADMINISTRADOR
        if (request.descuento() != null && request.descuento().compareTo(BigDecimal.ZERO) > 0) {
            boolean esAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
            if (!esAdmin) {
                throw new ReglaNegocioException("Los cajeros no tienen autorización para aplicar descuentos manuales.");
            }
        }
    }

    private Cliente obtenerCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + clienteId));
    }

    private void validarAforoAcumulado(List<DetalleVentaRequestDTO> items) {
        Map<Long, Integer> cantidadesPorAtraccion = new HashMap<>();
        for (DetalleVentaRequestDTO itemDto : items) {
            if (itemDto != null) {
                if (itemDto.atraccionId() == null) {
                    throw new ReglaNegocioException("El ID de la atracción es obligatorio para cada ítem");
                }
                int cant = (itemDto.cantidad() != null && itemDto.cantidad() > 0) ? itemDto.cantidad() : 1;
                cantidadesPorAtraccion.merge(itemDto.atraccionId(), cant, Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> entry : cantidadesPorAtraccion.entrySet()) {
            aforoValidatorService.validarCapacidadAtraccion(entry.getKey(), entry.getValue());
        }
    }

    private ResultadoValidacionItems procesarYValidarItems(List<DetalleVentaRequestDTO> items, Cliente cliente) {
        List<ItemCotizacionDTO> itemsParaCotizar = new ArrayList<>();
        List<Atraccion> atraccionesPorItem = new ArrayList<>();
        List<Menor> menoresPorItem = new ArrayList<>();
        Map<Long, Atraccion> atraccionesCache = new HashMap<>();
        Map<Long, Menor> menoresCache = new HashMap<>();

        for (DetalleVentaRequestDTO itemDto : items) {
            if (itemDto == null) {
                throw new ReglaNegocioException("Los ítems de la venta no pueden ser nulos");
            }
            if (itemDto.atraccionId() == null) {
                throw new ReglaNegocioException("El ID de la atracción es obligatorio para cada ítem");
            }

            Atraccion atraccion = atraccionesCache.computeIfAbsent(itemDto.atraccionId(), id ->
                    atraccionRepository.findById(id)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id))
            );

            if (!EstadoAtraccion.ACTIVO.equals(atraccion.getEstado())) {
                throw new ReglaNegocioException("La atracción '" + atraccion.getNombre() +
                        "' no se encuentra activa (estado: " + atraccion.getEstado() + ") y no admite ventas");
            }

            Menor menor = null;
            if (itemDto.menorId() != null) {
                menor = menoresCache.computeIfAbsent(itemDto.menorId(), id ->
                        menorRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Menor no encontrado con ID: " + id))
                );
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
                if (atraccion.getTarifaMinuto() == null || itemDto.precioUnitario().compareTo(atraccion.getTarifaMinuto()) != 0) {
                    throw new ReglaNegocioException("El precio unitario enviado ($" + itemDto.precioUnitario() +
                            ") no coincide con la tarifa base por minuto de la atracción ($" + atraccion.getTarifaMinuto() + ")");
                }
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

        return new ResultadoValidacionItems(itemsParaCotizar, atraccionesPorItem, menoresPorItem);
    }

    private CotizacionResponseDTO liquidarCotizacion(List<ItemCotizacionDTO> itemsParaCotizar, BigDecimal descuento) {
        CotizacionRequestDTO cotizacionRequest = new CotizacionRequestDTO(itemsParaCotizar, null, descuento);
        return ventaCalculoService.cotizarVenta(cotizacionRequest);
    }

    private BigDecimal validarYCalcularCambio(BigDecimal montoRecibidoParam, BigDecimal montoTotal) {
        BigDecimal montoRecibido = montoRecibidoParam.setScale(2, RoundingMode.HALF_UP);
        if (montoRecibido.compareTo(montoTotal) < 0) {
            throw new ReglaNegocioException("El monto recibido ($" + montoRecibido +
                    ") es inferior al total neto a pagar ($" + montoTotal + ")");
        }
        return montoRecibido.subtract(montoTotal).setScale(2, RoundingMode.HALF_UP);
    }

    private Venta persistirVenta(
            VentaProcesarRequestDTO request,
            Cliente cliente,
            TurnoCaja turnoCaja,
            CotizacionResponseDTO cotizacion,
            BigDecimal cambio,
            ResultadoValidacionItems resultadoItems
    ) {
        BigDecimal montoTotal = cotizacion.totalNeto();
        BigDecimal montoRecibido = request.montoRecibido().setScale(2, RoundingMode.HALF_UP);

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
            Atraccion atr = resultadoItems.atraccionesPorItem().get(i);
            Menor menor = resultadoItems.menoresPorItem().get(i);
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

        return ventaRepository.save(venta);
    }

    private List<Ticket> generarYAuditarTickets(Venta ventaGuardada, TurnoCaja turnoCaja) {
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

        for (Ticket t : ticketsGuardados) {
            auditoriaOperacionService.registrarTransicionTicket(
                    t.getUuidCodigo(),
                    null,
                    EstadoTicket.PAGADO.name(),
                    "Emisión de ticket tras venta exitosa #" + ventaGuardada.getId(),
                    turnoCaja.getUsuarioId(),
                    "Terminal Taquilla"
            );
        }

        return ticketsGuardados;
    }

    private void actualizarBalanceCajaSiAplica(TurnoCaja turnoCaja, MetodoPago metodoPago, BigDecimal montoTotal) {
        if (MetodoPago.EFECTIVO.equals(metodoPago)) {
            BigDecimal ventasActuales = turnoCaja.getVentasEfectivoSistema() != null
                    ? turnoCaja.getVentasEfectivoSistema()
                    : BigDecimal.ZERO;
            turnoCaja.setVentasEfectivoSistema(ventasActuales.add(montoTotal).setScale(2, RoundingMode.HALF_UP));
            turnoCajaRepository.save(turnoCaja);
            log.info("Balance de ventas en efectivo del turno #{} actualizado a ${}", turnoCaja.getId(), turnoCaja.getVentasEfectivoSistema());
        }
    }

    private void invalidarCacheAforo() {
        if (atraccionService != null) {
            atraccionService.invalidarCacheCatalogo();
        }
    }

    private Usuario resolverUsuarioEnSesion(Authentication auth) {
        Usuario usuario = authenticatedUserProvider != null
                ? authenticatedUserProvider.resolverUsuario(auth).orElse(null)
                : null;
        if (usuario == null) {
            Authentication effectiveAuth = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
            if (effectiveAuth != null && effectiveAuth.isAuthenticated() && !"anonymousUser".equals(effectiveAuth.getPrincipal())) {
                usuario = usuarioRepository.findByCorreo(effectiveAuth.getName()).orElse(null);
            }
        }
        return usuario;
    }

    private TurnoCaja resolverTurnoCajaActivo(Long turnoCajaId, Authentication auth) {
        Usuario usuarioAutenticado = resolverUsuarioEnSesion(auth);

        if (turnoCajaId != null) {
            TurnoCaja turno = turnoCajaRepository.findById(turnoCajaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado con ID: " + turnoCajaId));
            if (!EstadoTurnoCaja.ABIERTO.equals(turno.getEstado())) {
                throw new ReglaNegocioException("El turno de caja especificado (ID " + turnoCajaId + ") no se encuentra ABIERTO");
            }
            if (usuarioAutenticado != null && usuarioAutenticado.getRol() != null
                    && NombreRol.TAQUILLERO.equals(usuarioAutenticado.getRol().getNombre())
                    && !turno.getUsuarioId().equals(usuarioAutenticado.getId())) {
                throw new AccessDeniedException("No puede registrar ventas en el turno de caja de otro cajero.");
            }
            return turno;
        }

        if (usuarioAutenticado != null) {
            return turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioAutenticado.getId(), EstadoTurnoCaja.ABIERTO)
                    .orElseThrow(() -> new ReglaNegocioException("No se encontró ningún turno de caja ABIERTO para el cajero en sesión"));
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

        String nombreCompletoCliente = v.getCliente() != null ? v.getCliente().getNombreCompleto() : "";

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

    /**
     * Objeto de transferencia interno inmutable para agrupar las listas resultantes de la validación de ítems.
     */
    private record ResultadoValidacionItems(
            List<ItemCotizacionDTO> itemsParaCotizar,
            List<Atraccion> atraccionesPorItem,
            List<Menor> menoresPorItem
    ) {}
}
