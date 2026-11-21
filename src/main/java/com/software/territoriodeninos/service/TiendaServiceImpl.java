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
        List<Atraccion> atracciones = atraccionRepository.findAll();
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

        Map<Long, Integer> duracionesTarifaTiempo = new HashMap<>();
        Map<Long, List<TarifaTiempoResponseDTO>> tarifasTiempoPorAtraccion = new HashMap<>();
        Map<Long, List<TarifaEventoResponseDTO>> tarifasEventoPorAtraccion = new HashMap<>();

        for (Atraccion atr : atracciones) {
            List<TarifaTiempo> tarifasTiempo = tarifaTiempoRepository.findByAtraccionIdAndActivoTrue(atr.getId());
            if (tarifasTiempo != null) {
                for (TarifaTiempo tt : tarifasTiempo) {
                    if (tt.getId() != null && tt.getDuracionMinutos() != null) {
                        duracionesTarifaTiempo.put(tt.getId(), tt.getDuracionMinutos());
                    }
                }
            }
            List<TarifaTiempoResponseDTO> ttDTOs = (tarifasTiempo != null ? tarifasTiempo : List.<TarifaTiempo>of()).stream()
                    .map(t -> new TarifaTiempoResponseDTO(t.getId(), atr.getId(), atr.getNombre(), t.getDuracionMinutos(), t.getPrecio(), t.getActivo(), t.getFechaCreacion()))
                    .toList();
            tarifasTiempoPorAtraccion.put(atr.getId(), ttDTOs);

            List<TarifaEvento> tarifasEvento = tarifaEventoRepository.findByAtraccionIdAndActivoTrue(atr.getId());
            List<TarifaEventoResponseDTO> teDTOs = (tarifasEvento != null ? tarifasEvento : List.<TarifaEvento>of()).stream()
                    .map(t -> new TarifaEventoResponseDTO(t.getId(), atr.getId(), atr.getNombre(), t.getIntentos(), t.getPrecio(), t.getActivo(), t.getFechaCreacion()))
                    .toList();
            tarifasEventoPorAtraccion.put(atr.getId(), teDTOs);
        }

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
                    ingresoTexto
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

        // 2. Localizar o registrar cliente
        String doc = request.documento().trim();
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
                .map(existente -> {
                    boolean modificado = false;
                    if (!nombresFinal.isBlank()) {
                        existente.setNombres(nombresFinal);
                        modificado = true;
                    }
                    if (!apellidosFinal.isBlank()) {
                        existente.setApellidos(apellidosFinal);
                        modificado = true;
                    }
                    if (request.telefono() != null && !request.telefono().isBlank()) {
                        existente.setTelefono(request.telefono().trim());
                        modificado = true;
                    }
                    if (request.correo() != null && !request.correo().isBlank()) {
                        existente.setCorreo(request.correo().trim().toLowerCase());
                        modificado = true;
                    }
                    return modificado ? clienteRepository.save(existente) : existente;
                })
                .orElseGet(() -> {
                    Cliente nuevo = Cliente.builder()
                            .numeroDocumento(doc)
                            .nombres(nombresFinal)
                            .apellidos(apellidosFinal)
                            .telefono(request.telefono().trim())
                            .correo(request.correo().trim().toLowerCase())
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

            if (item.tarifaTiempoId() != null) {
                TarifaTiempo tt = tarifaTiempoRepository.findById(item.tarifaTiempoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa de tiempo con ID " + item.tarifaTiempoId() + " no encontrada"));
                precioUnitario = tt.getPrecio();
                modalidadTexto = tt.getDuracionMinutos() + " min";
                tipoTarifa = TipoTarifa.TIEMPO;
                if (tt.getDuracionMinutos() != null && tt.getDuracionMinutos() > 0) {
                    duracionItem = tt.getDuracionMinutos();
                }
            } else if (item.tarifaEventoId() != null) {
                TarifaEvento te = tarifaEventoRepository.findById(item.tarifaEventoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa de evento con ID " + item.tarifaEventoId() + " no encontrada"));
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

        invalidarCacheDespuesDeCommit();

        // 7. Enviar correo de respaldo con resumen de factura y hora estimada de ingreso
        emailService.enviarBoletasCompraOnline(
                cliente.getCorreo(),
                cliente.getNombres() + " " + cliente.getApellidos(),
                codigoOrden,
                subtotalAcumulado,
                impuestos,
                total,
                request.metodoPago(),
                ticketsEmitidos
        );

        return new CompraOnlineResponseDTO(
                ventaGuardada.getId(),
                codigoOrden,
                cliente.getNumeroDocumento(),
                cliente.getNombres() + " " + cliente.getApellidos(),
                cliente.getCorreo(),
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
    public List<TicketOnlineDTO> consultarBoletosActivos(String documento, String correo) {
        if (documento == null || documento.isBlank()) {
            throw new ReglaNegocioException("El número de documento es obligatorio para consultar boletos.");
        }

        String docLimpio = documento.trim();
        String correoLimpio = (correo != null && !correo.isBlank()) ? correo.trim().toLowerCase() : null;
        LocalDateTime desdeHoy = LocalDate.now().atStartOfDay();

        List<Ticket> tickets = ticketRepository.buscarBoletosActivosCliente(docLimpio, correoLimpio, desdeHoy);

        return tickets.stream()
                .map(t -> {
                    Atraccion a = t.getDetalleVenta() != null ? t.getDetalleVenta().getAtraccion() : null;
                    Menor m = t.getMenor();
                    String modTexto = (t.getDetalleVenta() != null && t.getDetalleVenta().getTipoTarifa() != null)
                            ? t.getDetalleVenta().getTipoTarifa().name()
                            : "TICKET";
                    String tipoTarifaStr = (t.getDetalleVenta() != null && t.getDetalleVenta().getTipoTarifa() != null)
                            ? t.getDetalleVenta().getTipoTarifa().name()
                            : "ESTANDAR";
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
                            t.getDetalleVenta() != null ? t.getDetalleVenta().getPrecioUnitario() : BigDecimal.ZERO,
                            t.getEstadoTicket(),
                            t.getFechaEmision(),
                            t.getUuidCodigo().toString(),
                            horaEstimada,
                            ingresoTexto
                    );
                })
                .toList();
    }

    private void invalidarCacheDespuesDeCommit() {
        if (atraccionService == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    atraccionService.invalidarCacheCatalogo();
                }
            });
        } else {
            atraccionService.invalidarCacheCatalogo();
        }
    }
}
