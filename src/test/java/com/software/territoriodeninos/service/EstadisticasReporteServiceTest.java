package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.GraficoTendenciaDTO;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.VentaRepository;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstadisticasReporteServiceTest - Pruebas de agregaciones estadísticas y gráficas (HU-40.02 a HU-40.04)")
class EstadisticasReporteServiceTest {

    @Mock
    private SesionJuegoRepository sesionJuegoRepository;

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private EstadisticasReporteServiceImpl estadisticasReporteService;

    @Test
    @DisplayName("Debe agregar horas pico en 12 franjas horarias entre 08:00 y 20:00 (HU-40.02)")
    void testObtenerHorasPico() {
        LocalDateTime h10 = LocalDateTime.now().withHour(10).withMinute(15);
        LocalDateTime h10b = LocalDateTime.now().withHour(10).withMinute(45);
        LocalDateTime h16 = LocalDateTime.now().withHour(16).withMinute(30);

        List<Object[]> mockHoras = new ArrayList<>();
        mockHoras.add(new Object[]{1L, h10});
        mockHoras.add(new Object[]{1L, h10b});
        mockHoras.add(new Object[]{1L, h16});

        when(sesionJuegoRepository.findHorasInicioPorPeriodo(any(), any()))
                .thenReturn(mockHoras);

        GraficoTendenciaDTO grafico = estadisticasReporteService.obtenerHorasPico(null, null);

        assertThat(grafico).isNotNull();
        assertThat(grafico.titulo()).isEqualTo("Horas Pico de Afluencia");
        assertThat(grafico.totalAcumulado()).isEqualTo(3.0);
        assertThat(grafico.dataset().puntos()).hasSize(12); // Franjas de 8 a 19

        // Validar franja 10:00 - 11:00 con 2 sesiones y 66.67%
        var p10 = grafico.dataset().puntos().stream()
                .filter(p -> p.etiqueta().startsWith("10:00"))
                .findFirst().orElseThrow();
        assertThat(p10.valor()).isEqualTo(2.0);
        assertThat(p10.porcentajeRelativo()).isEqualTo(66.67);
    }

    @Test
    @DisplayName("Debe calcular la distribución porcentual por método de pago (HU-40.03)")
    void testObtenerDistribucionMetodosPago() {
        List<Object[]> mockMetodos = new ArrayList<>();
        mockMetodos.add(new Object[]{MetodoPago.EFECTIVO, new BigDecimal("60000.00")});
        mockMetodos.add(new Object[]{MetodoPago.TARJETA, new BigDecimal("40000.00")});

        when(ventaRepository.sumVentasPorMetodoPago(any(), any()))
                .thenReturn(mockMetodos);

        GraficoTendenciaDTO grafico = estadisticasReporteService.obtenerDistribucionMetodosPago(null, null);

        assertThat(grafico).isNotNull();
        assertThat(grafico.totalAcumulado()).isEqualTo(100000.0);
        assertThat(grafico.dataset().puntos()).hasSize(2);

        var pEfectivo = grafico.dataset().puntos().stream()
                .filter(p -> p.etiqueta().equals("EFECTIVO"))
                .findFirst().orElseThrow();
        assertThat(pEfectivo.valor()).isEqualTo(60000.0);
        assertThat(pEfectivo.porcentajeRelativo()).isEqualTo(60.0);

        var pTarjeta = grafico.dataset().puntos().stream()
                .filter(p -> p.etiqueta().equals("TARJETA"))
                .findFirst().orElseThrow();
        assertThat(pTarjeta.valor()).isEqualTo(40000.0);
        assertThat(pTarjeta.porcentajeRelativo()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("Debe generar la evolución diaria de ingresos en los últimos 7 días (HU-40.04)")
    void testObtenerEvolucionIngresosUltimos7Dias() {
        Venta vHoy = Venta.builder()
                .id(1L)
                .fechaVenta(LocalDateTime.now())
                .montoTotal(new BigDecimal("50000.00"))
                .estadoVenta(EstadoVenta.PAGADO)
                .build();

        Venta vAyer = Venta.builder()
                .id(2L)
                .fechaVenta(LocalDateTime.now().minusDays(1))
                .montoTotal(new BigDecimal("30000.00"))
                .estadoVenta(EstadoVenta.COMPLETADA)
                .build();

        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any()))
                .thenReturn(List.of(vHoy, vAyer));

        GraficoTendenciaDTO grafico = estadisticasReporteService.obtenerEvolucionIngresosUltimos7Dias();

        assertThat(grafico).isNotNull();
        assertThat(grafico.dataset().puntos()).hasSize(7);
        assertThat(grafico.totalAcumulado()).isEqualTo(80000.0);
    }
}
