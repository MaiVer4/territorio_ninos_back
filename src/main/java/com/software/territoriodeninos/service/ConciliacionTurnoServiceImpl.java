package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.DetalleTurnoAuditadoDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.DetalleVentaRepository;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.repository.TurnoCajaRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.repository.VentaRepository;
import com.software.territoriodeninos.util.EstadoConciliacionTurno;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConciliacionTurnoServiceImpl implements ConciliacionTurnoService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtraccionRepository atraccionRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final SesionJuegoRepository sesionJuegoRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReporteTurnoDTO> obtenerReporteTurnos(LocalDateTime inicio, LocalDateTime fin, Long usuarioId) {
        LocalDateTime fechaInicio = inicio != null ? inicio : LocalDateTime.now().minusDays(30).with(LocalTime.MIN);
        LocalDateTime fechaFin = fin != null ? fin : LocalDateTime.now().with(LocalTime.MAX);

        List<TurnoCaja> turnos = turnoCajaRepository.findTurnosCerradosPorFiltros(fechaInicio, fechaFin, usuarioId);
        if (turnos.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> usuarioIds = turnos.stream()
                .map(TurnoCaja::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nombresCajerosPorId = usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre, (a, b) -> a));

        Set<Long> turnoIds = turnos.stream()
                .map(TurnoCaja::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Venta> ventasLote = ventaRepository.findByTurnoCajaIdIn(turnoIds);
        Map<Long, List<Venta>> ventasPorTurnoId = ventasLote != null && !ventasLote.isEmpty()
                ? ventasLote.stream()
                    .filter(v -> v.getTurnoCaja() != null && v.getTurnoCaja().getId() != null)
                    .collect(Collectors.groupingBy(v -> v.getTurnoCaja().getId()))
                : Collections.emptyMap();

        return turnos.stream()
                .map(t -> construirReporteTurno(
                        t,
                        nombresCajerosPorId.get(t.getUsuarioId()),
                        ventasPorTurnoId.get(t.getId())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DetalleTurnoAuditadoDTO obtenerDetalleTurno(Long turnoId) {
        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el turno de caja con ID: " + turnoId));

        List<Venta> ventas = ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(turnoId);
        ReporteTurnoDTO reporteTurno = construirReporteTurno(turno, null, ventas);

        List<Long> ventaIds = ventas.stream().map(Venta::getId).filter(Objects::nonNull).toList();
        Map<Long, List<Ticket>> ticketsPorDetalleId = Collections.emptyMap();
        if (!ventaIds.isEmpty() && ticketRepository != null) {
            List<Ticket> ticketsLote = ticketRepository.findByDetalleVentaVentaIdInOrderByFechaEmisionAsc(ventaIds);
            if (ticketsLote != null && !ticketsLote.isEmpty()) {
                ticketsPorDetalleId = ticketsLote.stream()
                        .filter(t -> t.getDetalleVenta() != null && t.getDetalleVenta().getId() != null)
                        .collect(Collectors.groupingBy(t -> t.getDetalleVenta().getId()));
            }
        }
        final Map<Long, List<Ticket>> mapaTickets = ticketsPorDetalleId;

        List<DetalleTurnoAuditadoDTO.VentaAuditadaDTO> ventasAuditadas = ventas.stream().map(venta -> {
            List<DetalleTurnoAuditadoDTO.DetalleLineaAuditadaDTO> lineas = venta.getDetalles().stream().map(detalle -> {
                List<Ticket> ticketsDeLinea = mapaTickets.get(detalle.getId());
                if (ticketsDeLinea == null && detalle.getTickets() != null) {
                    ticketsDeLinea = detalle.getTickets();
                } else if (ticketsDeLinea == null) {
                    ticketsDeLinea = Collections.emptyList();
                }

                List<DetalleTurnoAuditadoDTO.TicketAuditadoDTO> boletas = ticketsDeLinea.stream().map(ticket ->
                        DetalleTurnoAuditadoDTO.TicketAuditadoDTO.builder()
                                .uuidCodigo(ticket.getUuidCodigo())
                                .estadoTicket(ticket.getEstadoTicket() != null ? ticket.getEstadoTicket().name() : null)
                                .fechaEmision(ticket.getFechaEmision())
                                .fechaConsumo(ticket.getFechaConsumo())
                                .build()
                ).collect(Collectors.toList());

                return DetalleTurnoAuditadoDTO.DetalleLineaAuditadaDTO.builder()
                        .lineaId(detalle.getId())
                        .atraccionId(detalle.getAtraccion() != null ? detalle.getAtraccion().getId() : null)
                        .atraccionNombre(detalle.getAtraccion() != null ? detalle.getAtraccion().getNombre() : "N/A")
                        .menorNombre(detalle.getMenor() != null ? detalle.getMenor().getNombreCompleto() : "Sin menor asociado")
                        .tipoTarifa(detalle.getTipoTarifa() != null ? detalle.getTipoTarifa().name() : null)
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getPrecioUnitario())
                        .subtotal(detalle.getSubtotal())
                        .boletas(boletas)
                        .build();
            }).collect(Collectors.toList());

            return DetalleTurnoAuditadoDTO.VentaAuditadaDTO.builder()
                    .ventaId(venta.getId())
                    .fechaVenta(venta.getFechaVenta())
                    .clienteNombre(venta.getCliente() != null ? venta.getCliente().getNombreCompleto() : "Consumidor Final")
                    .clienteDocumento(venta.getCliente() != null ? venta.getCliente().getNumeroDocumento() : "N/A")
                    .metodoPago(venta.getMetodoPago() != null ? venta.getMetodoPago().name() : null)
                    .estadoVenta(venta.getEstadoVenta() != null ? venta.getEstadoVenta().name() : null)
                    .montoTotal(venta.getMontoTotal())
                    .lineas(lineas)
                    .build();
        }).collect(Collectors.toList());

        return DetalleTurnoAuditadoDTO.builder()
                .turno(reporteTurno)
                .ventas(ventasAuditadas)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RotacionAtraccionDTO> obtenerRotacionAtracciones(
            LocalDateTime inicio,
            LocalDateTime fin,
            String periodo,
            String criterioOrden
    ) {
        LocalDateTime fechaFin = fin != null ? fin : LocalDateTime.now();
        LocalDateTime fechaInicio = inicio;
        if (fechaInicio == null) {
            String p = periodo != null ? periodo.toUpperCase().trim() : "MENSUAL";
            fechaInicio = switch (p) {
                case "DIARIO" -> LocalDate.now().atStartOfDay();
                case "SEMANAL" -> fechaFin.minusDays(7);
                default -> fechaFin.minusDays(30);
            };
        }

        List<Atraccion> atracciones = atraccionRepository.findAll();

        // Ingresos brutos por atracción (HU-37.02)
        Map<Long, BigDecimal> ingresosPorAtraccion = new HashMap<>();
        List<Object[]> rawIngresos = detalleVentaRepository.sumIngresosBrutosPorAtraccion(fechaInicio, fechaFin);
        for (Object[] row : rawIngresos) {
            Long atraccionId = (Long) row[0];
            BigDecimal total = (BigDecimal) row[1];
            ingresosPorAtraccion.put(atraccionId, total != null ? total : BigDecimal.ZERO);
        }

        // Métricas de uso (conteo y duración promedio) por atracción (HU-37.02)
        Map<Long, Long> usosPorAtraccion = new HashMap<>();
        Map<Long, Double> duracionPromedioSegundos = new HashMap<>();
        List<Object[]> rawMetricas = sesionJuegoRepository.findMetricasUsoPorAtraccion(fechaInicio, fechaFin);
        for (Object[] row : rawMetricas) {
            Long atraccionId = (Long) row[0];
            Long count = ((Number) row[1]).longValue();
            Double avgSeconds = ((Number) row[2]).doubleValue();
            usosPorAtraccion.put(atraccionId, count);
            duracionPromedioSegundos.put(atraccionId, avgSeconds);
        }

        // Hora de mayor afluencia horaria (HU-37.02)
        Map<Long, List<LocalDateTime>> iniciosPorAtraccion = new HashMap<>();
        List<Object[]> rawHoras = sesionJuegoRepository.findHorasInicioPorPeriodo(fechaInicio, fechaFin);
        for (Object[] row : rawHoras) {
            Long atraccionId = (Long) row[0];
            LocalDateTime hInicio = (LocalDateTime) row[1];
            iniciosPorAtraccion.computeIfAbsent(atraccionId, k -> new ArrayList<>()).add(hInicio);
        }

        // Horas operativas en el periodo para el cálculo de capacidad instalada (HU-37.03)
        double duracionHoras = Math.max(1.0, Duration.between(fechaInicio, fechaFin).toMinutes() / 60.0);

        List<RotacionAtraccionDTO> resultado = new ArrayList<>();

        for (Atraccion atraccion : atracciones) {
            Long atraccionId = atraccion.getId();
            Long totalUsos = usosPorAtraccion.getOrDefault(atraccionId, 0L);
            BigDecimal ingresosBrutos = ingresosPorAtraccion.getOrDefault(atraccionId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            double avgSegundos = duracionPromedioSegundos.getOrDefault(atraccionId, 0.0);
            double avgMinutos = BigDecimal.valueOf(avgSegundos / 60.0).setScale(2, RoundingMode.HALF_UP).doubleValue();

            // Algoritmo matemático de ocupación relativa (HU-37.03)
            int duracionBase = (atraccion.getDuracionMinutosBase() != null && atraccion.getDuracionMinutosBase() > 0)
                    ? atraccion.getDuracionMinutosBase()
                    : 15;
            double slotsPorHora = 60.0 / duracionBase;
            int capacidadInstalada = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
            double capacidadTeoricaPeriodo = capacidadInstalada * slotsPorHora * duracionHoras;

            double tasaOcupacion = capacidadTeoricaPeriodo > 0
                    ? BigDecimal.valueOf((totalUsos / capacidadTeoricaPeriodo) * 100.0).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            // Determinación de franja horaria pico
            String horaMayorAfluencia = calcularHoraPico(iniciosPorAtraccion.get(atraccionId));

            resultado.add(RotacionAtraccionDTO.builder()
                    .atraccionId(atraccionId)
                    .nombreAtraccion(atraccion.getNombre())
                    .tipoAtraccion(atraccion.getTipo())
                    .capacidadMaxima(capacidadInstalada)
                    .cantidadTotalUsos(totalUsos)
                    .ingresosBrutos(ingresosBrutos)
                    .tiempoPromedioMinutos(avgMinutos)
                    .porcentajeOcupacion(tasaOcupacion)
                    .horaMayorAfluencia(horaMayorAfluencia)
                    .build());
        }

        // Ordenamiento por criterio solicitado (HU-37.04)
        String orden = criterioOrden != null ? criterioOrden.toUpperCase().trim() : "USOS";
        switch (orden) {
            case "INGRESOS" -> resultado.sort((a, b) -> b.ingresosBrutos().compareTo(a.ingresosBrutos()));
            case "OCUPACION" -> resultado.sort((a, b) -> Double.compare(b.porcentajeOcupacion(), a.porcentajeOcupacion()));
            default -> resultado.sort((a, b) -> Long.compare(b.cantidadTotalUsos(), a.cantidadTotalUsos()));
        }

        return resultado;
    }

    private ReporteTurnoDTO construirReporteTurno(TurnoCaja turno) {
        return construirReporteTurno(turno, null, null);
    }

    private ReporteTurnoDTO construirReporteTurno(TurnoCaja turno, String nombreCajeroPreCargado, List<Venta> ventasPreCargadas) {
        String nombreCajero = nombreCajeroPreCargado != null
                ? nombreCajeroPreCargado
                : usuarioRepository.findById(turno.getUsuarioId())
                        .map(Usuario::getNombre)
                        .orElse("Cajero #" + turno.getUsuarioId());

        List<Venta> ventas = ventasPreCargadas != null
                ? ventasPreCargadas
                : ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(turno.getId());

        List<Venta> ventasValidas = ventas.stream()
                .filter(v -> v.getEstadoVenta() == EstadoVenta.PAGADO || v.getEstadoVenta() == EstadoVenta.COMPLETADA)
                .toList();

        BigDecimal ventasEfectivo = ventasValidas.stream()
                .filter(v -> v.getMetodoPago() == MetodoPago.EFECTIVO)
                .map(Venta::getMontoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal ventasElectronico = ventasValidas.stream()
                .filter(v -> v.getMetodoPago() != MetodoPago.EFECTIVO)
                .map(Venta::getMontoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal baseInicial = turno.getMontoBase() != null
                ? turno.getMontoBase().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // totalEsperadoEfectivo = montoBase + ventasEfectivoSistema (Notion: CA-02)
        BigDecimal totalEsperadoEfectivo = baseInicial.add(ventasEfectivo).setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoFisicoDeclarado = turno.getMontoEfectivoReportado() != null
                ? turno.getMontoEfectivoReportado().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Algoritmo matemático de discrepancias (Notion: CA-03)
        // diferencia = montoFisicoDeclarado - totalEsperadoEfectivo
        BigDecimal diferencia = montoFisicoDeclarado.subtract(totalEsperadoEfectivo).setScale(2, RoundingMode.HALF_UP);

        EstadoConciliacionTurno estadoConciliacion;
        int comparacion = diferencia.compareTo(BigDecimal.ZERO);
        if (comparacion == 0) {
            estadoConciliacion = EstadoConciliacionTurno.CUADRADO;
        } else if (comparacion > 0) {
            estadoConciliacion = EstadoConciliacionTurno.SOBRANTE;
        } else {
            estadoConciliacion = EstadoConciliacionTurno.FALTANTE;
        }

        BigDecimal totalFacturado = ventasEfectivo.add(ventasElectronico).setScale(2, RoundingMode.HALF_UP);

        return ReporteTurnoDTO.builder()
                .turnoId(turno.getId())
                .usuarioId(turno.getUsuarioId())
                .nombreCajero(nombreCajero)
                .fechaApertura(turno.getFechaApertura())
                .fechaCierre(turno.getFechaCierre())
                .baseInicial(baseInicial)
                .ventasEfectivoSistema(ventasEfectivo)
                .ventasElectronicoSistema(ventasElectronico)
                .totalEsperadoEfectivo(totalEsperadoEfectivo)
                .montoFisicoDeclarado(montoFisicoDeclarado)
                .diferencia(diferencia)
                .estadoConciliacion(estadoConciliacion)
                .estadoTurno(turno.getEstado())
                .totalFacturado(totalFacturado)
                .totalTransacciones(ventasValidas.size())
                .observaciones(turno.getObservaciones())
                .build();
    }

    private String calcularHoraPico(List<LocalDateTime> fechas) {
        if (fechas == null || fechas.isEmpty()) {
            return "Sin afluencia registrada";
        }

        Map<Integer, Long> conteoPorHora = fechas.stream()
                .collect(Collectors.groupingBy(LocalDateTime::getHour, Collectors.counting()));

        return conteoPorHora.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> String.format("%02d:00 - %02d:00", entry.getKey(), (entry.getKey() + 1) % 24))
                .orElse("Sin afluencia registrada");
    }
}
