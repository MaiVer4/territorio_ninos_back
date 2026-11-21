package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.dto.tienda.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiendaServiceImpl implements TiendaService {

    private static final List<EstadoSesionJuego> ESTADOS_SESION_ACTIVAS =
            List.of(EstadoSesionJuego.EN_CURSO, EstadoSesionJuego.PAUSADA);
    private static final List<EstadoTicket> ESTADOS_TICKET_ACTIVOS =
            List.of(EstadoTicket.PAGADO, EstadoTicket.EN_USO, EstadoTicket.PAUSADO);
    private static final Set<MetodoPago> METODOS_PAGO_ONLINE = EnumSet.of(
            MetodoPago.NEQUI,
            MetodoPago.PSE,
            MetodoPago.TARJETA,
            MetodoPago.TARJETA_DEBITO,
            MetodoPago.TARJETA_CREDITO,
            MetodoPago.DAVIPLATA,
            MetodoPago.TRANSFERENCIA
    );

    private final AtraccionRepository atraccionRepository;
    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final TarifaEventoRepository tarifaEventoRepository;
    private final ClienteRepository clienteRepository;
    private final MenorRepository menorRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final TicketRepository ticketRepository;
    private final AuditoriaTicketRepository auditoriaTicketRepository;
    private final ConfiguracionParqueRepository configuracionParqueRepository;
    private final EmailService emailService;
    private final SesionJuegoRepository sesionJuegoRepository;
    private final EstimadorIngresoService estimadorIngresoService;
    private final AtraccionService atraccionService;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoAtraccionOnlineDTO> obtenerCatalogoPublico() {
        List<Atraccion> atracciones = atraccionRepository.findByEstadoInOrderByNombreAsc(
                List.of(EstadoAtraccion.ACTIVO)
        );
        if (atracciones == null || atracciones.isEmpty()) {
            return List.of();
        }

        List<Long> atraccionIds = atracciones.stream()
                .map(Atraccion::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Integer> ocupacionPorAtraccion = new HashMap<>();
        if (sesionJuegoRepository != null && !atraccionIds.isEmpty()) {
            List<SesionJuego> sesiones = sesionJuegoRepository.findByAtraccionIdInAndEstadoSesionIn(
                    atraccionIds, ESTADOS_SESION_ACTIVAS
            );
            if (sesiones != null) {
                for (SesionJuego s : sesiones) {
                    if (s.getAtraccion() != null && s.getAtraccion().getId() != null) {
                        ocupacionPorAtraccion.merge(s.getAtraccion().getId(), 1, Integer::sum);
                    }
                }
            }
        }

        Map<Long, Atraccion> atraccionesPorId = new HashMap<>();
        for (Atraccion atr : atracciones) {
            if (atr.getId() != null) {
                atraccionesPorId.put(atr.getId(), atr);
            }
        }

        Map<Long, Integer> duracionesTarifaTiempo = new HashMap<>();
        Map<Long, List<TarifaTiempoResponseDTO>> tarifasTiempoPorAtraccion = new HashMap<>();
        Map<Long, List<TarifaEventoResponseDTO>> tarifasEventoPorAtraccion = new HashMap<>();

        if (!atraccionIds.isEmpty()) {
            List<TarifaTiempo> todasTarifasTiempo = tarifaTiempoRepository
                    .findByAtraccionIdInAndActivoTrueOrderByDuracionMinutosAsc(atraccionIds);
            if (todasTarifasTiempo != null) {
                for (TarifaTiempo tt : todasTarifasTiempo) {
                    if (tt.getAtraccion() == null || tt.getAtraccion().getId() == null) continue;
                    Long atrId = tt.getAtraccion().getId();
                    Atraccion atr = atraccionesPorId.get(atrId);
                    if (tt.getId() != null && tt.getDuracionMinutos() != null) {
                        duracionesTarifaTiempo.put(tt.getId(), tt.getDuracionMinutos());
                    }
                    tarifasTiempoPorAtraccion
                            .computeIfAbsent(atrId, k -> new ArrayList<>())
                            .add(new TarifaTiempoResponseDTO(
                                    tt.getId(),
                                    atrId,
                                    atr != null ? atr.getNombre() : tt.getAtraccion().getNombre(),
                                    tt.getDuracionMinutos(),
                                    tt.getPrecio(),
                                    tt.getActivo(),
                                    tt.getFechaCreacion()
                            ));
                }
            }

            List<TarifaEvento> todasTarifasEvento = tarifaEventoRepository
                    .findByAtraccionIdInAndActivoTrueOrderByIntentosAsc(atraccionIds);
            if (todasTarifasEvento != null) {
                for (TarifaEvento te : todasTarifasEvento) {
                    if (te.getAtraccion() == null || te.getAtraccion().getId() == null) continue;
                    Long atrId = te.getAtraccion().getId();
                    Atraccion atr = atraccionesPorId.get(atrId);
                    tarifasEventoPorAtraccion
                            .computeIfAbsent(atrId, k -> new ArrayList<>())
                            .add(new TarifaEventoResponseDTO(
                                    te.getId(),
                                    atrId,
                                    atr != null ? atr.getNombre() : te.getAtraccion().getNombre(),
                                    te.getIntentos(),
                                    te.getPrecio(),
                                    te.getActivo(),
                                    te.getFechaCreacion()
                            ));
                }
            }
        }

        ConfiguracionParque config = configuracionParqueRepository != null
                ? configuracionParqueRepository.findFirstByOrderByIdAsc().orElse(null)
                : null;
        BigDecimal porcentajeImpuesto = (config != null && Boolean.TRUE.equals(config.getAplicaImpuesto())
                && config.getPorcentajeImpuesto() != null)
                ? config.getPorcentajeImpuesto()
                : BigDecimal.ZERO;

        Map<Long, LocalDateTime> proximosIngresos = estimadorIngresoService != null
                ? estimadorIngresoService.estimarProximoIngresoBatch(atracciones, LocalDateTime.now(), duracionesTarifaTiempo)
                : Collections.emptyMap();

        List<CatalogoAtraccionOnlineDTO> resultado = new ArrayList<>();
        for (Atraccion atr : atracciones) {
            List<TarifaTiempoResponseDTO> ttDTOs = tarifasTiempoPorAtraccion.getOrDefault(atr.getId(), List.of());
            List<TarifaEventoResponseDTO> teDTOs = tarifasEventoPorAtraccion.getOrDefault(atr.getId(), List.of());

            boolean disponible = atr.getEstado() == EstadoAtraccion.ACTIVO && (!ttDTOs.isEmpty() || !teDTOs.isEmpty() || atr.getTarifaMinuto() != null);
            int capacidad = atr.getCapacidadMaxima() != null ? atr.getCapacidadMaxima() : 1;
            int ocupacionActual = ocupacionPorAtraccion.getOrDefault(atr.getId(), 0);
            int cuposLibres = Math.max(0, capacidad - ocupacionActual);
            LocalDateTime horaProximoIngreso = proximosIngresos != null ? proximosIngresos.get(atr.getId()) : null;
            String ingresoTexto = IngresoTextoFormatter.texto(horaProximoIngreso, atr.getTipo());

            resultado.add(new CatalogoAtraccionOnlineDTO(
                    atr.getId(),
                    atr.getNombre(),
                    atr.getDescripcion(),
                    null,
                    atr.getTipo(),
                    atr.getEstado(),
                    atr.getCapacidadMaxima(),
                    atr.getDuracionMinutosBase(),
                    atr.getEdadMinima(),
                    atr.getEdadMaxima(),
                    disponible,
                    ttDTOs,
                    teDTOs,
                    atr.getTarifaMinuto(),
                    ocupacionActual,
                    cuposLibres,
                    horaProximoIngreso,
                    ingresoTexto,
                    porcentajeImpuesto
            ));
        }

        return resultado;
    }

    @Override
    @Transactional
    public CompraOnlineResponseDTO procesarCompraOnline(CompraOnlineRequestDTO request) {
        // 1. Protección Honeypot contra bots y spam
        if (request.website() != null && !request.website().isBlank()) {
            log.warn("Honeypot activado en compra online para documento {}", request.documento());
            throw new ReglaNegocioException("La solicitud no pudo ser procesada.");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new ReglaNegocioException("El carrito de compras no contiene boletos.");
        }

        if (request.metodoPago() == null || !METODOS_PAGO_ONLINE.contains(request.metodoPago())) {
            throw new ReglaNegocioException("El método de pago '" + request.metodoPago() + "' no está habilitado para compras online.");
        }

        // 2. Localizar o registrar cliente (sin sobrescribir PII de clientes existentes desde canal anónimo)
        String doc = request.documento().trim();
        String correoSolicitud = request.correo().trim().toLowerCase();
        String nombresRaw = request.nombres().trim();
        String apellidosRaw = (request.apellidos() != null && !request.apellidos().isBlank())
                ? request.apellidos().trim()
                : "";
        if (apellidosRaw.isEmpty()) {
            int idx = nombresRaw.lastIndexOf(' ');
            if (idx > 0) {
                apellidosRaw = nombresRaw.substring(idx + 1).trim();
                nombresRaw = nombresRaw.substring(0, idx).trim();
            } else {
                apellidosRaw = "-";
            }
        }
        final String nombresFinal = nombresRaw;
        final String apellidosFinal = apellidosRaw;

        Cliente cliente = clienteRepository.findByNumeroDocumento(doc)
                .orElseGet(() -> {
                    Cliente nuevo = Cliente.builder()
                            .numeroDocumento(doc)
                            .nombres(nombresFinal)
                            .apellidos(apellidosFinal)
                            .telefono(request.telefono().trim())
                            .correo(correoSolicitud)
                            .fechaRegistro(LocalDateTime.now())
                            .build();
                    return clienteRepository.save(nuevo);
                });

        // 3. Obtener configuración de impuestos
        ConfiguracionParque config = configuracionParqueRepository.findFirstByOrderByIdAsc().orElse(null);
        BigDecimal porcentajeImpuesto = (config != null && Boolean.TRUE.equals(config.getAplicaImpuesto()) && config.getPorcentajeImpuesto() != null)
                ? config.getPorcentajeImpuesto()
                : BigDecimal.ZERO;

        // 4. Procesar y validar cada ítem
        BigDecimal subtotalAcumulado = BigDecimal.ZERO;
        String codigoOrden = "ORD-" + (System.currentTimeMillis() % 1000000) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        record ItemProcesado(
                Atraccion atraccion,
                Menor menor,
                TipoTarifa tipoTarifa,
                Long tarifaTiempoId,
                Long tarifaEventoId,
                String modalidadTexto,
                BigDecimal precioUnitario,
                int cantidad,
                int duracionMinutos
        ) {}

        List<ItemProcesado> itemsProcesados = new ArrayList<>();

        for (ItemCompraOnlineDTO item : request.items()) {
            Atraccion atraccion = atraccionRepository.findById(item.atraccionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Atracción con ID " + item.atraccionId() + " no encontrada"));

            if (atraccion.getEstado() != EstadoAtraccion.ACTIVO) {
                throw new ReglaNegocioException("La atracción '" + atraccion.getNombre() + "' no se encuentra disponible para venta.");
            }

            // Validar restricción de edad del menor
            if (atraccion.getEdadMinima() != null && item.menorEdad() < atraccion.getEdadMinima()) {
                throw new ReglaNegocioException("El niño '" + item.menorNombre() + "' (" + item.menorEdad() +
                        " años) no cumple con la edad mínima (" + atraccion.getEdadMinima() + " años) para '" + atraccion.getNombre() + "'.");
            }
            if (atraccion.getEdadMaxima() != null && item.menorEdad() > atraccion.getEdadMaxima()) {
                throw new ReglaNegocioException("El niño '" + item.menorNombre() + "' (" + item.menorEdad() +
                        " años) supera la edad máxima permitida (" + atraccion.getEdadMaxima() + " años) para '" + atraccion.getNombre() + "'.");
            }

            // Buscar o crear el Menor vinculado al Cliente
            String nombreMenor = item.menorNombre().trim();
            Menor menor = menorRepository.findByClienteIdAndNombreCompletoIgnoreCase(cliente.getId(), nombreMenor)
                    .orElseGet(() -> {
                        Menor m = Menor.builder()
                                .cliente(cliente)
                                .nombreCompleto(nombreMenor)
                                .edad(item.menorEdad())
                                .fechaRegistro(LocalDateTime.now())
                                .build();
                        return menorRepository.save(m);
                    });

            BigDecimal precioUnitario;
            String modalidadTexto;
            TipoTarifa tipoTarifa;
            int duracionBase = (atraccion.getDuracionMinutosBase() != null && atraccion.getDuracionMinutosBase() > 0)
                    ? atraccion.getDuracionMinutosBase() : 15;
            int duracionItem = duracionBase;

            if (item.tarifaTiempoId() != null && item.tarifaEventoId() != null) {
                throw new ReglaNegocioException("Cada ítem debe especificar únicamente una tarifa de tiempo o de evento.");
            }

            if (item.tarifaTiempoId() != null) {
                TarifaTiempo tt = tarifaTiempoRepository.findById(item.tarifaTiempoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa de tiempo con ID " + item.tarifaTiempoId() + " no encontrada"));
                if (!Boolean.TRUE.equals(tt.getActivo())
                        || tt.getAtraccion() == null
                        || !atraccion.getId().equals(tt.getAtraccion().getId())) {
                    throw new ReglaNegocioException("La tarifa de tiempo seleccionada no pertenece a la atracción '" + atraccion.getNombre() + "' o no está activa.");
                }
                precioUnitario = tt.getPrecio();
                modalidadTexto = tt.getDuracionMinutos() + " min";
                tipoTarifa = TipoTarifa.TIEMPO;
                if (tt.getDuracionMinutos() != null && tt.getDuracionMinutos() > 0) {
                    duracionItem = tt.getDuracionMinutos();
                }
            } else if (item.tarifaEventoId() != null) {
                TarifaEvento te = tarifaEventoRepository.findById(item.tarifaEventoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa de evento con ID " + item.tarifaEventoId() + " no encontrada"));
                if (!Boolean.TRUE.equals(te.getActivo())
                        || te.getAtraccion() == null
                        || !atraccion.getId().equals(te.getAtraccion().getId())) {
                    throw new ReglaNegocioException("La tarifa de evento seleccionada no pertenece a la atracción '" + atraccion.getNombre() + "' o no está activa.");
                }
                precioUnitario = te.getPrecio();
                modalidadTexto = te.getIntentos() + " caídas";
                tipoTarifa = TipoTarifa.EVENTO;
            } else {
                precioUnitario = (atraccion.getTarifaMinuto() != null)
                        ? atraccion.getTarifaMinuto().multiply(BigDecimal.valueOf(duracionBase))
                        : BigDecimal.valueOf(10000);
                modalidadTexto = duracionBase + " min (Base)";
                tipoTarifa = TipoTarifa.MINUTO_BASE;
            }

            BigDecimal subtotalLinea = precioUnitario.multiply(BigDecimal.valueOf(item.cantidad())).setScale(2, RoundingMode.HALF_UP);
            subtotalAcumulado = subtotalAcumulado.add(subtotalLinea);

            itemsProcesados.add(new ItemProcesado(
                    atraccion,
                    menor,
                    tipoTarifa,
                    item.tarifaTiempoId(),
                    item.tarifaEventoId(),
                    modalidadTexto,
                    precioUnitario,
                    item.cantidad(),
                    duracionItem
            ));
        }

        BigDecimal impuestos = subtotalAcumulado.multiply(porcentajeImpuesto).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalAcumulado.add(impuestos).setScale(2, RoundingMode.HALF_UP);

        // 5. Persistir la Venta Online
        Venta venta = Venta.builder()
                .cliente(cliente)
                .turnoCaja(null) // Ventas online no pertenecen a un turno físico de caja
                .canalVenta(CanalVenta.ONLINE)
                .codigoOrden(codigoOrden)
                .subtotal(subtotalAcumulado)
                .descuento(BigDecimal.ZERO)
                .impuestos(impuestos)
                .montoTotal(total)
                .montoRecibido(total)
                .cambio(BigDecimal.ZERO)
                .metodoPago(request.metodoPago())
                .estadoVenta(EstadoVenta.PAGADO)
                .fechaVenta(LocalDateTime.now())
                .congelada(true)
                .observaciones("Compra Web Online - Acudiente: " + cliente.getNombres() + " " + cliente.getApellidos())
                .build();

        Venta ventaGuardada = ventaRepository.save(venta);

        // 5.1 Estimar horas de ingreso escalonadas en cola FIFO por atracción
        Map<Long, List<Integer>> duracionesPorAtraccion = new LinkedHashMap<>();
        Map<Long, Atraccion> atraccionesPorId = new HashMap<>();
        for (ItemProcesado ip : itemsProcesados) {
            Long atrId = ip.atraccion().getId();
            atraccionesPorId.putIfAbsent(atrId, ip.atraccion());
            List<Integer> lista = duracionesPorAtraccion.computeIfAbsent(atrId, k -> new ArrayList<>());
            for (int c = 0; c < ip.cantidad(); c++) {
                lista.add(ip.duracionMinutos());
            }
        }
        Map<Long, Iterator<LocalDateTime>> horasPorAtraccion = new HashMap<>();
        LocalDateTime ahoraEstimacion = LocalDateTime.now();
        for (Map.Entry<Long, List<Integer>> entry : duracionesPorAtraccion.entrySet()) {
            Atraccion atr = atraccionesPorId.get(entry.getKey());
            List<Integer> duraciones = entry.getValue();
            List<LocalDateTime> horas = estimadorIngresoService != null
                    ? estimadorIngresoService.estimar(atr, duraciones, ahoraEstimacion)
                    : Collections.nCopies(duraciones.size(), (LocalDateTime) null);
            horasPorAtraccion.put(entry.getKey(), horas.iterator());
        }

        // 6. Persistir Detalles y Tickets digitales
        List<TicketOnlineDTO> ticketsEmitidos = new ArrayList<>();

        for (ItemProcesado item : itemsProcesados) {
            BigDecimal subtotalDetalle = item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())).setScale(2, RoundingMode.HALF_UP);

            DetalleVenta detalle = DetalleVenta.builder()
                    .venta(ventaGuardada)
                    .atraccion(item.atraccion())
                    .menor(item.menor())
                    .tipoTarifa(item.tipoTarifa())
                    .tarifaTiempoId(item.tarifaTiempoId())
                    .tarifaEventoId(item.tarifaEventoId())
                    .cantidad(item.cantidad())
                    .precioUnitario(item.precioUnitario())
                    .subtotal(subtotalDetalle)
                    .build();

            DetalleVenta detalleGuardado = detalleVentaRepository.save(detalle);
            Iterator<LocalDateTime> itHoras = horasPorAtraccion.get(item.atraccion().getId());

            for (int i = 0; i < item.cantidad(); i++) {
                UUID uuidCodigo = UUID.randomUUID();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime horaEstimada = (itHoras != null && itHoras.hasNext()) ? itHoras.next() : null;
                String ingresoTexto = IngresoTextoFormatter.texto(horaEstimada, item.atraccion().getTipo());

                Ticket ticket = Ticket.builder()
                        .uuidCodigo(uuidCodigo)
                        .detalleVenta(detalleGuardado)
                        .menor(item.menor())
                        .estadoTicket(EstadoTicket.PAGADO)
                        .fechaEmision(now)
                        .horaIngresoEstimada(horaEstimada)
                        .build();

                Ticket ticketGuardado = ticketRepository.save(ticket);

                // Auditoría de emisión
                AuditoriaTicket audit = AuditoriaTicket.builder()
                        .ticketUuid(uuidCodigo)
                        .estadoOrigen(null)
                        .estadoDestino("PAGADO")
                        .motivo("EMISION_ONLINE: Orden #" + codigoOrden)
                        .fechaEvento(now)
                        .dispositivoInfo("PORTAL_WEB_PUBLICO")
                        .build();
                auditoriaTicketRepository.save(audit);

                ticketsEmitidos.add(new TicketOnlineDTO(
                        ticketGuardado.getUuidCodigo(),
                        item.atraccion().getId(),
                        item.atraccion().getNombre(),
                        item.menor().getNombreCompleto(),
                        item.menor().getEdad(),
                        item.tipoTarifa().name(),
                        item.modalidadTexto(),
                        item.precioUnitario(),
                        ticketGuardado.getEstadoTicket(),
                        ticketGuardado.getFechaEmision(),
                        ticketGuardado.getUuidCodigo().toString(),
                        horaEstimada,
                        ingresoTexto
                ));
            }
        }

        // 7. Invalidar caché y enviar correo únicamente tras confirmar la transacción en BD
        final String nombreClienteCompleto = cliente.getNombres() + " " + cliente.getApellidos();
        final BigDecimal subtotalFinal = subtotalAcumulado;
        final BigDecimal impuestosFinal = impuestos;
        final BigDecimal totalFinal = total;
        final MetodoPago metodoPagoFinal = request.metodoPago();
        final List<TicketOnlineDTO> ticketsFinal = List.copyOf(ticketsEmitidos);

        ejecutarDespuesDeCommit(() -> {
            if (atraccionService != null) {
                atraccionService.invalidarCacheCatalogo();
            }
            emailService.enviarBoletasCompraOnline(
                    correoSolicitud,
                    nombreClienteCompleto,
                    codigoOrden,
                    subtotalFinal,
                    impuestosFinal,
                    totalFinal,
                    metodoPagoFinal,
                    ticketsFinal
            );
        });

        return new CompraOnlineResponseDTO(
                ventaGuardada.getId(),
                codigoOrden,
                cliente.getNumeroDocumento(),
                nombreClienteCompleto,
                correoSolicitud,
                subtotalAcumulado,
                impuestos,
                total,
                request.metodoPago(),
                ventaGuardada.getFechaVenta(),
                ticketsEmitidos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketOnlineDTO> consultarBoletosActivos(String documento, String correo, String codigoOrden) {
        if (documento == null || documento.isBlank()) {
            throw new ReglaNegocioException("El número de documento es obligatorio para consultar boletos.");
        }

        String docLimpio = documento.trim();
        String correoLimpio = (correo != null && !correo.isBlank()) ? correo.trim().toLowerCase() : null;
        String ordenLimpia = (codigoOrden != null && !codigoOrden.isBlank()) ? codigoOrden.trim().toUpperCase() : null;

        if (correoLimpio == null && ordenLimpia == null) {
            throw new ReglaNegocioException("Debe ingresar el correo electrónico o el código de orden para consultar sus boletos.");
        }

        LocalDateTime desdeHoy = LocalDate.now().atStartOfDay();

        List<Ticket> tickets = (ordenLimpia != null)
                ? ticketRepository.buscarBoletosActivosPorDocumentoYOrden(docLimpio, ordenLimpia, ESTADOS_TICKET_ACTIVOS, desdeHoy)
                : ticketRepository.buscarBoletosActivosPorDocumentoYCorreo(docLimpio, correoLimpio, ESTADOS_TICKET_ACTIVOS, desdeHoy);

        if (tickets == null || tickets.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> duracionesPorTarifa = new HashMap<>();
        Map<Long, Integer> intentosPorTarifa = new HashMap<>();

        Set<Long> tarifaTiempoIds = new HashSet<>();
        Set<Long> tarifaEventoIds = new HashSet<>();
        for (Ticket t : tickets) {
            if (t.getDetalleVenta() != null) {
                if (t.getDetalleVenta().getTarifaTiempoId() != null) {
                    tarifaTiempoIds.add(t.getDetalleVenta().getTarifaTiempoId());
                }
                if (t.getDetalleVenta().getTarifaEventoId() != null) {
                    tarifaEventoIds.add(t.getDetalleVenta().getTarifaEventoId());
                }
            }
        }
        if (!tarifaTiempoIds.isEmpty() && tarifaTiempoRepository != null) {
            List<TarifaTiempo> encontradas = tarifaTiempoRepository.findAllById(tarifaTiempoIds);
            if (encontradas != null) {
                for (TarifaTiempo tt : encontradas) {
                    if (tt.getId() != null && tt.getDuracionMinutos() != null) {
                        duracionesPorTarifa.put(tt.getId(), tt.getDuracionMinutos());
                    }
                }
            }
        }
        if (!tarifaEventoIds.isEmpty() && tarifaEventoRepository != null) {
            List<TarifaEvento> encontradas = tarifaEventoRepository.findAllById(tarifaEventoIds);
            if (encontradas != null) {
                for (TarifaEvento te : encontradas) {
                    if (te.getId() != null && te.getIntentos() != null) {
                        intentosPorTarifa.put(te.getId(), te.getIntentos());
                    }
                }
            }
        }

        return tickets.stream()
                .map(t -> {
                    DetalleVenta dv = t.getDetalleVenta();
                    Atraccion a = dv != null ? dv.getAtraccion() : null;
                    Menor m = t.getMenor();
                    TipoTarifa tipoTarifa = dv != null ? dv.getTipoTarifa() : null;
                    String tipoTarifaStr = tipoTarifa != null ? tipoTarifa.name() : "ESTANDAR";
                    String modTexto = resolverModalidadTexto(dv, a, duracionesPorTarifa, intentosPorTarifa);
                    LocalDateTime horaEstimada = t.getHoraIngresoEstimada();
                    String ingresoTexto = IngresoTextoFormatter.texto(horaEstimada, a != null ? a.getTipo() : null);

                    return new TicketOnlineDTO(
                            t.getUuidCodigo(),
                            a != null ? a.getId() : null,
                            a != null ? a.getNombre() : "Atracción",
                            m != null ? m.getNombreCompleto() : "General",
                            m != null ? m.getEdad() : null,
                            tipoTarifaStr,
                            modTexto,
                            dv != null ? dv.getPrecioUnitario() : BigDecimal.ZERO,
                            t.getEstadoTicket(),
                            t.getFechaEmision(),
                            t.getUuidCodigo().toString(),
                            horaEstimada,
                            ingresoTexto
                    );
                })
                .toList();
    }

    private String resolverModalidadTexto(
            DetalleVenta dv,
            Atraccion atr,
            Map<Long, Integer> duracionesPorTarifa,
            Map<Long, Integer> intentosPorTarifa
    ) {
        if (dv == null || dv.getTipoTarifa() == null) {
            return "TICKET";
        }
        if (dv.getTarifaTiempoId() != null && duracionesPorTarifa.containsKey(dv.getTarifaTiempoId())) {
            return duracionesPorTarifa.get(dv.getTarifaTiempoId()) + " min";
        }
        if (dv.getTarifaEventoId() != null && intentosPorTarifa.containsKey(dv.getTarifaEventoId())) {
            return intentosPorTarifa.get(dv.getTarifaEventoId()) + " caídas";
        }
        if (dv.getTipoTarifa() == TipoTarifa.MINUTO_BASE) {
            int base = (atr != null && atr.getDuracionMinutosBase() != null && atr.getDuracionMinutosBase() > 0)
                    ? atr.getDuracionMinutosBase() : 15;
            return base + " min (Base)";
        }
        if (dv.getTipoTarifa() == TipoTarifa.TIEMPO) {
            int base = (atr != null && atr.getDuracionMinutosBase() != null && atr.getDuracionMinutosBase() > 0)
                    ? atr.getDuracionMinutosBase() : 15;
            return base + " min";
        }
        if (dv.getTipoTarifa() == TipoTarifa.EVENTO) {
            int caidas = (atr != null && atr.getCaidasLimite() != null && atr.getCaidasLimite() > 0)
                    ? atr.getCaidasLimite() : 3;
            return caidas + " caídas";
        }
        return dv.getTipoTarifa().name();
    }

    private void ejecutarDespuesDeCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
