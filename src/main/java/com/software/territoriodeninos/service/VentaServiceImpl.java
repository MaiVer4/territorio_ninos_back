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
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.ClienteRepository;
import com.software.territoriodeninos.repository.MenorRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
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
    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final TarifaEventoRepository tarifaEventoRepository;
    private EstimadorIngresoService estimadorIngresoService;

    @Autowired(required = false)
    public void setEstimadorIngresoService(EstimadorIngresoService estimadorIngresoService) {
        this.estimadorIngresoService = estimadorIngresoService;
    }

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
            @Autowired(required = false) AuthenticatedUserProvider authenticatedUserProvider,
            @Autowired(required = false) TarifaTiempoRepository tarifaTiempoRepository,
            @Autowired(required = false) TarifaEventoRepository tarifaEventoRepository
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
        this.tarifaTiempoRepository = tarifaTiempoRepository;
        this.tarifaEventoRepository = tarifaEventoRepository;
    }

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
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this(ventaRepository, ticketRepository, clienteRepository, menorRepository, turnoCajaRepository,
                usuarioRepository, atraccionRepository, auditoriaOperacionService, aforoValidatorService,
                ventaCalculoService, uuidGeneratorService, atraccionService, authenticatedUserProvider, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request, Authentication auth) {
        log.info("Iniciando procesamiento transaccional de venta para cliente ID: {}", request != null ? request.clienteId() : null);

        // 1. Validaciones estructurales y de autorización de la solicitud
        validarSolicitudVenta(request, auth);

        // 2. Validación de cliente y turno de caja
        Cliente cliente = obtenerCliente(request);
        TurnoCaja turnoCaja = resolverTurnoCajaActivo(request.turnoCajaId(), auth);

        // 3. Validación de aforo acumulado por atracción
        validarAforoAcumulado(request.items());

        // 4. Validación individual de ítems y preparación para cotización
        ResultadoValidacionItems resultadoItems = procesarYValidarItems(request.items(), cliente);

        // 5. Liquidación financiera y cálculo de cambio (soportando modalidad IVA incluido del POS)
        CotizacionResponseDTO cotizacion = ajustarCotizacionSegunModalidadPos(
                liquidarCotizacion(resultadoItems.itemsParaCotizar(), request.descuento()),
                request.montoRecibido()
        );
        BigDecimal montoTotal = cotizacion.totalNeto();
        BigDecimal cambio = validarYCalcularCambio(request.montoRecibido(), montoTotal);

        // 6. Construcción y persistencia de la Venta congelada
        Venta ventaGuardada = persistirVenta(request, cliente, turnoCaja, cotizacion, cambio, resultadoItems);

        // 7. Generación y auditoría inmutable de tickets criptoseguros
        List<Ticket> ticketsGuardados = generarYAuditarTickets(ventaGuardada, turnoCaja, request.items());

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

    private Cliente obtenerCliente(VentaProcesarRequestDTO request) {
        Long clienteId = request.clienteId();
        if (clienteId != null && clienteId > 0) {
            return clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + clienteId));
        }

        String doc = request.clienteDocumento() != null && !request.clienteDocumento().isBlank()
                ? request.clienteDocumento().trim()
                : "222222222222";
        String nombreCompleto = request.clienteNombre() != null && !request.clienteNombre().isBlank()
                ? request.clienteNombre().trim()
                : "Cliente de Mostrador";

        return clienteRepository.findByNumeroDocumento(doc).orElseGet(() -> {
            String[] partes = nombreCompleto.split("\\s+", 2);
            String nombres = partes[0];
            String apellidos = partes.length > 1 ? partes[1] : "Mostrador";
            Cliente nuevo = Cliente.builder()
                    .numeroDocumento(doc)
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .fechaRegistro(LocalDateTime.now())
                    .build();
            return clienteRepository.save(nuevo);
        });
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
        List<Long> tarifasTiempoPorItem = new ArrayList<>();
        List<Long> tarifasEventoPorItem = new ArrayList<>();
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
            } else if (itemDto.menorNombre() != null
                    && !itemDto.menorNombre().isBlank()
                    && !"Sin menor asignado".equalsIgnoreCase(itemDto.menorNombre().trim())) {
                String nombreMenor = itemDto.menorNombre().trim();
                if (cliente.getMenores() != null) {
                    menor = cliente.getMenores().stream()
                            .filter(m -> m.getNombreCompleto() != null && m.getNombreCompleto().equalsIgnoreCase(nombreMenor))
                            .findFirst()
                            .orElse(null);
                }
                if (menor == null && menorRepository != null && cliente.getId() != null) {
                    Menor nuevoMenor = Menor.builder()
                            .cliente(cliente)
                            .nombreCompleto(nombreMenor)
                            .edad(itemDto.menorEdad())
                            .fechaRegistro(LocalDateTime.now())
                            .build();
                    menor = menorRepository.save(nuevoMenor);
                    cliente.addMenor(menor);
                }
            }

            int cantidad = (itemDto.cantidad() != null && itemDto.cantidad() > 0) ? itemDto.cantidad() : 1;
            if (itemDto.precioUnitario() == null || itemDto.precioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                throw new ReglaNegocioException("El precio unitario no puede ser nulo ni negativo");
            }
            if (itemDto.tipoTarifa() == null) {
                throw new ReglaNegocioException("El tipo de tarifa es obligatorio para cada ítem");
            }

            Long tarifaTiempoId = itemDto.tarifaTiempoId();
            if (TipoTarifa.TIEMPO.equals(itemDto.tipoTarifa()) && tarifaTiempoId == null) {
                if (tarifaTiempoRepository != null) {
                    List<TarifaTiempo> tarifasActivas = tarifaTiempoRepository.findByAtraccionIdAndActivoTrue(atraccion.getId());
                    if (!tarifasActivas.isEmpty()) {
                        tarifaTiempoId = tarifasActivas.stream()
                                .filter(t -> t.getPrecio() != null && itemDto.precioUnitario() != null && t.getPrecio().compareTo(itemDto.precioUnitario()) == 0)
                                .map(TarifaTiempo::getId)
                                .findFirst()
                                .orElse(tarifasActivas.get(0).getId());
                    }
                }
                if (tarifaTiempoId == null) {
                    throw new ReglaNegocioException("Debe especificar tarifaTiempoId para tarifas de tipo TIEMPO");
                }
            }

            Long tarifaEventoId = itemDto.tarifaEventoId();
            if (TipoTarifa.EVENTO.equals(itemDto.tipoTarifa()) && tarifaEventoId == null) {
                if (tarifaEventoRepository != null) {
                    List<TarifaEvento> tarifasActivas = tarifaEventoRepository.findByAtraccionIdAndActivoTrue(atraccion.getId());
                    if (!tarifasActivas.isEmpty()) {
                        tarifaEventoId = tarifasActivas.stream()
                                .filter(e -> e.getPrecio() != null && itemDto.precioUnitario() != null && e.getPrecio().compareTo(itemDto.precioUnitario()) == 0)
                                .map(TarifaEvento::getId)
                                .findFirst()
                                .orElse(tarifasActivas.get(0).getId());
                    }
                }
                if (tarifaEventoId == null) {
                    throw new ReglaNegocioException("Debe especificar tarifaEventoId para tarifas de tipo EVENTO");
                }
            }

            if (TipoTarifa.MINUTO_BASE.equals(itemDto.tipoTarifa())) {
                if (atraccion.getTarifaMinuto() == null || itemDto.precioUnitario().compareTo(atraccion.getTarifaMinuto()) != 0) {
                    throw new ReglaNegocioException("El precio unitario enviado ($" + itemDto.precioUnitario() +
                            ") no coincide con la tarifa base por minuto de la atracción ($" + atraccion.getTarifaMinuto() + ")");
                }
            }

            itemsParaCotizar.add(new ItemCotizacionDTO(
                    atraccion.getId(),
                    tarifaTiempoId,
                    tarifaEventoId,
                    atraccion.getNombre(),
                    cantidad,
                    itemDto.precioUnitario(),
                    itemDto.subtotal()
            ));
            atraccionesPorItem.add(atraccion);
            menoresPorItem.add(menor);
            tarifasTiempoPorItem.add(tarifaTiempoId);
            tarifasEventoPorItem.add(tarifaEventoId);
        }

        return new ResultadoValidacionItems(itemsParaCotizar, atraccionesPorItem, menoresPorItem, tarifasTiempoPorItem, tarifasEventoPorItem);
    }

    private CotizacionResponseDTO liquidarCotizacion(List<ItemCotizacionDTO> itemsParaCotizar, BigDecimal descuento) {
        CotizacionRequestDTO cotizacionRequest = new CotizacionRequestDTO(itemsParaCotizar, null, descuento);
        return ventaCalculoService.cotizarVenta(cotizacionRequest);
    }

    private CotizacionResponseDTO ajustarCotizacionSegunModalidadPos(
            CotizacionResponseDTO cotizacion,
            BigDecimal montoRecibidoParam
    ) {
        if (cotizacion == null || montoRecibidoParam == null) {
            return cotizacion;
        }
        // Si la configuración tiene IVA activo pero el POS opera con tarifas de IVA incluido
        // (montoRecibido >= baseGravable y montoRecibido < totalNeto con IVA sumado encima)
        if (cotizacion.impuestos() != null
                && cotizacion.impuestos().compareTo(BigDecimal.ZERO) > 0
                && cotizacion.baseGravable() != null
                && montoRecibidoParam.compareTo(cotizacion.baseGravable()) >= 0
                && montoRecibidoParam.compareTo(cotizacion.totalNeto()) < 0) {
            BigDecimal totalIncluido = cotizacion.baseGravable().setScale(2, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(
                    cotizacion.porcentajeImpuesto().divide(new BigDecimal("100.00"), 6, RoundingMode.HALF_UP)
            );
            BigDecimal baseNeta = totalIncluido.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal impIncluido = totalIncluido.subtract(baseNeta).setScale(2, RoundingMode.HALF_UP);
            return new CotizacionResponseDTO(
                    cotizacion.subtotales(),
                    cotizacion.items(),
                    baseNeta,
                    cotizacion.descuento(),
                    baseNeta,
                    cotizacion.porcentajeImpuesto(),
                    impIncluido,
                    totalIncluido
            );
        }
        return cotizacion;
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
                    .tarifaTiempoId(resultadoItems.tarifasTiempoPorItem().get(i))
                    .tarifaEventoId(resultadoItems.tarifasEventoPorItem().get(i))
                    .cantidad(cant)
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalLinea)
                    .build();

            venta.addDetalle(detalle);
        }

        return ventaRepository.save(venta);
    }

    private List<Ticket> generarYAuditarTickets(
            Venta ventaGuardada,
            TurnoCaja turnoCaja,
            List<DetalleVentaRequestDTO> itemsRequest
    ) {
        List<Ticket> ticketsGenerados = new ArrayList<>();
        List<DetalleVenta> detalles = ventaGuardada.getDetalles();
        Map<Long, List<Integer>> duracionesPorAtraccion = new java.util.LinkedHashMap<>();
        for (DetalleVenta d : detalles) {
            int c = (d.getCantidad() != null && d.getCantidad() > 0) ? d.getCantidad() : 1;
            int dur = resolverDuracionMinutos(d);
            List<Integer> lista = duracionesPorAtraccion.computeIfAbsent(d.getAtraccion().getId(), k -> new ArrayList<>());
            for (int i = 0; i < c; i++) lista.add(dur);
        }
        Map<Long, java.util.Iterator<LocalDateTime>> horasPorAtraccion = new HashMap<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (DetalleVenta d : detalles) {
            Long atrId = d.getAtraccion().getId();
            if (!horasPorAtraccion.containsKey(atrId)) {
                List<Integer> duraciones = duracionesPorAtraccion.get(atrId);
                List<LocalDateTime> horas = estimadorIngresoService != null
                        ? estimadorIngresoService.estimar(d.getAtraccion(), duraciones, ahora)
                        : java.util.Collections.nCopies(duraciones.size(), (LocalDateTime) null);
                horasPorAtraccion.put(atrId, horas.iterator());
            }
        }
        for (int idx = 0; idx < detalles.size(); idx++) {
            DetalleVenta detalle = detalles.get(idx);
            DetalleVentaRequestDTO itemDto = (itemsRequest != null && idx < itemsRequest.size()) ? itemsRequest.get(idx) : null;
            int cant = (detalle.getCantidad() != null && detalle.getCantidad() > 0) ? detalle.getCantidad() : 1;

            List<UUID> uuids = null;
            if (cant == 1 && itemDto != null && itemDto.uuid() != null && !itemDto.uuid().isBlank()) {
                try {
                    UUID uuidPropuesto = UUID.fromString(itemDto.uuid().trim());
                    if (!ticketRepository.existsByUuidCodigo(uuidPropuesto)) {
                        uuids = List.of(uuidPropuesto);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Si el formato no es UUID estándar, se delega al generador criptoseguro
                }
            }
            if (uuids == null) {
                uuids = uuidGeneratorService.generarUuidsUnicos(cant);
            }

            List<LocalDateTime> horasIngreso = estimarHorasIngreso(detalle, uuids.size(), horasPorAtraccion);
            for (int u = 0; u < uuids.size(); u++) {
                UUID uuid = uuids.get(u);
                Ticket ticket = Ticket.builder()
                        .uuidCodigo(uuid)
                        .detalleVenta(detalle)
                        .menor(detalle.getMenor())
                        .estadoTicket(EstadoTicket.PAGADO)
                        .fechaEmision(ventaGuardada.getFechaVenta())
                        .horaIngresoEstimada(horasIngreso.get(u))
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

    private List<LocalDateTime> estimarHorasIngreso(DetalleVenta detalle, int cantidad,
                                                    Map<Long, java.util.Iterator<LocalDateTime>> horasPorAtraccion) {
        java.util.Iterator<LocalDateTime> it = horasPorAtraccion.get(detalle.getAtraccion().getId());
        List<LocalDateTime> horas = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            horas.add(it != null && it.hasNext() ? it.next() : null);
        }
        return horas;
    }

    private int resolverDuracionMinutos(DetalleVenta d) {
        Atraccion a = d.getAtraccion();
        int base = a != null && a.getDuracionMinutosBase() != null && a.getDuracionMinutosBase() > 0
                ? a.getDuracionMinutosBase() : 15;
        if (d.getTarifaTiempoId() != null && tarifaTiempoRepository != null) {
            return tarifaTiempoRepository.findById(d.getTarifaTiempoId())
                    .map(TarifaTiempo::getDuracionMinutos).orElse(base);
        }
        return base;
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
                t.getFechaEmision(),
                t.getHoraIngresoEstimada(),
                com.software.territoriodeninos.util.IngresoTextoFormatter.texto(t.getHoraIngresoEstimada(),
                        d != null && d.getAtraccion() != null ? d.getAtraccion().getTipo() : null)
        );
    }

    /**
     * Objeto de transferencia interno inmutable para agrupar las listas resultantes de la validación de ítems.
     */
    private record ResultadoValidacionItems(
            List<ItemCotizacionDTO> itemsParaCotizar,
            List<Atraccion> atraccionesPorItem,
            List<Menor> menoresPorItem,
            List<Long> tarifasTiempoPorItem,
            List<Long> tarifasEventoPorItem
    ) {}
}
