package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.DetalleTurnoAuditadoDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConciliacionTurnoServiceTest - Pruebas de conciliación contable y rotación por atracción")
class ConciliacionTurnoServiceTest {

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AtraccionRepository atraccionRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @Mock
    private SesionJuegoRepository sesionJuegoRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private ConciliacionTurnoServiceImpl conciliacionTurnoService;

    private TurnoCaja turnoCuadrado;
    private TurnoCaja turnoSobrante;
    private TurnoCaja turnoFaltante;
    private Usuario cajero;
    private Atraccion atraccion;

    @BeforeEach
    void setUp() {
        cajero = Usuario.builder()
                .id(1L)
                .nombre("Laura Gómez")
                .correo("laura@territorio.com")
                .build();

        // Turno 1: Base 50000, Ventas Efectivo 50000 -> Esperado 100000, Reportado 100000 -> CUADRADO
        turnoCuadrado = TurnoCaja.builder()
                .id(101L)
                .usuarioId(1L)
                .montoBase(new BigDecimal("50000.00"))
                .montoEfectivoReportado(new BigDecimal("100000.00"))
                .estado(EstadoTurnoCaja.CERRADO)
                .fechaApertura(LocalDateTime.now().minusHours(8))
                .fechaCierre(LocalDateTime.now().minusHours(1))
                .build();

        // Turno 2: Base 50000, Ventas Efectivo 50000 -> Esperado 100000, Reportado 105000 -> SOBRANTE (+5000)
        turnoSobrante = TurnoCaja.builder()
                .id(102L)
                .usuarioId(1L)
                .montoBase(new BigDecimal("50000.00"))
                .montoEfectivoReportado(new BigDecimal("105000.00"))
                .estado(EstadoTurnoCaja.CERRADO)
                .fechaApertura(LocalDateTime.now().minusDays(1).minusHours(8))
                .fechaCierre(LocalDateTime.now().minusDays(1))
                .build();

        // Turno 3: Base 50000, Ventas Efectivo 50000 -> Esperado 100000, Reportado 90000 -> FALTANTE (-10000)
        turnoFaltante = TurnoCaja.builder()
                .id(103L)
                .usuarioId(1L)
                .montoBase(new BigDecimal("50000.00"))
                .montoEfectivoReportado(new BigDecimal("90000.00"))
                .estado(EstadoTurnoCaja.CERRADO)
                .fechaApertura(LocalDateTime.now().minusDays(2).minusHours(8))
                .fechaCierre(LocalDateTime.now().minusDays(2))
                .build();

        atraccion = Atraccion.builder()
                .id(1L)
                .nombre("Inflable Mágico")
                .tipo(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(10)
                .duracionMinutosBase(15)
                .tarifaMinuto(new BigDecimal("500.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Debe clasificar correctamente turnos en CUADRADO, SOBRANTE y FALTANTE (HU-36.03, HU-36.04)")
    void testConciliacionDiferenciasMonetarias() {
        when(turnoCajaRepository.findTurnosCerradosPorFiltros(any(), any(), any()))
                .thenReturn(List.of(turnoCuadrado, turnoSobrante, turnoFaltante));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cajero));

        // Ventas de turnoCuadrado: 1 en efectivo por 50000 y 1 electrónica por 30000
        Venta v1 = Venta.builder()
                .id(1L)
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.PAGADO)
                .montoTotal(new BigDecimal("50000.00"))
                .build();
        Venta v2 = Venta.builder()
                .id(2L)
                .metodoPago(MetodoPago.TARJETA)
                .estadoVenta(EstadoVenta.PAGADO)
                .montoTotal(new BigDecimal("30000.00"))
                .build();

        when(ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(101L)).thenReturn(List.of(v1, v2));
        when(ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(102L)).thenReturn(List.of(v1));
        when(ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(103L)).thenReturn(List.of(v1));

        List<ReporteTurnoDTO> reportes = conciliacionTurnoService.obtenerReporteTurnos(null, null, null);

        assertThat(reportes).hasSize(3);

        ReporteTurnoDTO repCuadrado = reportes.get(0);
        assertThat(repCuadrado.estadoConciliacion()).isEqualTo(EstadoConciliacionTurno.CUADRADO);
        assertThat(repCuadrado.diferencia()).isEqualByComparingTo("0.00");
        assertThat(repCuadrado.totalEsperadoEfectivo()).isEqualByComparingTo("100000.00");
        assertThat(repCuadrado.ventasEfectivoSistema()).isEqualByComparingTo("50000.00");
        assertThat(repCuadrado.ventasElectronicoSistema()).isEqualByComparingTo("30000.00");
        assertThat(repCuadrado.totalFacturado()).isEqualByComparingTo("80000.00");

        ReporteTurnoDTO repSobrante = reportes.get(1);
        assertThat(repSobrante.estadoConciliacion()).isEqualTo(EstadoConciliacionTurno.SOBRANTE);
        assertThat(repSobrante.diferencia()).isEqualByComparingTo("5000.00");

        ReporteTurnoDTO repFaltante = reportes.get(2);
        assertThat(repFaltante.estadoConciliacion()).isEqualTo(EstadoConciliacionTurno.FALTANTE);
        assertThat(repFaltante.diferencia()).isEqualByComparingTo("-10000.00");
    }

    @Test
    @DisplayName("Debe entregar el detalle auditado completo de ventas y boletas para un turno (HU-36.06)")
    void testObtenerDetalleTurno() {
        when(turnoCajaRepository.findById(101L)).thenReturn(Optional.of(turnoCuadrado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cajero));

        Ticket ticket = Ticket.builder()
                .uuidCodigo(UUID.randomUUID())
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .fechaConsumo(null)
                .build();

        DetalleVenta detalle = DetalleVenta.builder()
                .id(50L)
                .atraccion(atraccion)
                .cantidad(1)
                .precioUnitario(new BigDecimal("15000.00"))
                .subtotal(new BigDecimal("15000.00"))
                .tipoTarifa(TipoTarifa.TIEMPO)
                .tickets(new ArrayList<>(List.of(ticket)))
                .build();

        Venta venta = Venta.builder()
                .id(200L)
                .fechaVenta(LocalDateTime.now())
                .cliente(Cliente.builder().id(10L).nombres("Mario").apellidos("Bros").numeroDocumento("123456").build())
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.PAGADO)
                .montoTotal(new BigDecimal("15000.00"))
                .detalles(new ArrayList<>(List.of(detalle)))
                .build();

        when(ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(101L)).thenReturn(List.of(venta));

        DetalleTurnoAuditadoDTO resultado = conciliacionTurnoService.obtenerDetalleTurno(101L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.turno().turnoId()).isEqualTo(101L);
        assertThat(resultado.ventas()).hasSize(1);
        assertThat(resultado.ventas().get(0).clienteNombre()).isEqualTo("Mario Bros");
        assertThat(resultado.ventas().get(0).lineas()).hasSize(1);
        assertThat(resultado.ventas().get(0).lineas().get(0).boletas()).hasSize(1);
    }

    @Test
    @DisplayName("Debe calcular rotación, demanda y tasa de ocupación matemática (HU-37.01, HU-37.03)")
    void testObtenerRotacionAtracciones() {
        when(atraccionRepository.findAll()).thenReturn(List.of(atraccion));

        // Ingresos: atraccion 1 -> 450000.00
        List<Object[]> mockIngresos = new ArrayList<>();
        mockIngresos.add(new Object[]{1L, new BigDecimal("450000.00")});
        when(detalleVentaRepository.sumIngresosBrutosPorAtraccion(any(), any()))
                .thenReturn(mockIngresos);

        // Métricas: atraccion 1 -> 30 usos, duración promedio 1200 seg (20 min)
        List<Object[]> mockMetricas = new ArrayList<>();
        mockMetricas.add(new Object[]{1L, 30L, 1200.0});
        when(sesionJuegoRepository.findMetricasUsoPorAtraccion(any(), any()))
                .thenReturn(mockMetricas);

        // Horas: 2 sesiones a las 15:xx y 1 sesión a las 10:xx -> Hora pico 15:00 - 16:00
        LocalDateTime hoy15 = LocalDateTime.now().withHour(15).withMinute(10);
        LocalDateTime hoy15b = LocalDateTime.now().withHour(15).withMinute(40);
        LocalDateTime hoy10 = LocalDateTime.now().withHour(10).withMinute(15);
        List<Object[]> mockHoras = new ArrayList<>();
        mockHoras.add(new Object[]{1L, hoy15});
        mockHoras.add(new Object[]{1L, hoy15b});
        mockHoras.add(new Object[]{1L, hoy10});
        when(sesionJuegoRepository.findHorasInicioPorPeriodo(any(), any()))
                .thenReturn(mockHoras);

        LocalDateTime inicio = LocalDateTime.now().minusHours(5);
        LocalDateTime fin = LocalDateTime.now();

        List<RotacionAtraccionDTO> rotacion = conciliacionTurnoService.obtenerRotacionAtracciones(
                inicio, fin, "DIARIO", "USOS"
        );

        assertThat(rotacion).hasSize(1);
        RotacionAtraccionDTO dto = rotacion.get(0);
        assertThat(dto.nombreAtraccion()).isEqualTo("Inflable Mágico");
        assertThat(dto.cantidadTotalUsos()).isEqualTo(30L);
        assertThat(dto.ingresosBrutos()).isEqualByComparingTo("450000.00");
        assertThat(dto.tiempoPromedioMinutos()).isEqualTo(20.0);
        assertThat(dto.horaMayorAfluencia()).isEqualTo("15:00 - 16:00");
        assertThat(dto.porcentajeOcupacion()).isGreaterThan(0.0);
    }
}
