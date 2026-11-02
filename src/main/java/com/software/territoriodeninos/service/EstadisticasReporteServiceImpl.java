package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.DatasetGraficoDTO;
import com.software.territoriodeninos.dto.GraficoTendenciaDTO;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.VentaRepository;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EstadisticasReporteServiceImpl implements EstadisticasReporteService {

    private final SesionJuegoRepository sesionJuegoRepository;
    private final VentaRepository ventaRepository;

    @Override
    @Transactional(readOnly = true)
    public GraficoTendenciaDTO obtenerHorasPico(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime fechaFin = fin != null ? fin : LocalDateTime.now();
        LocalDateTime fechaInicio = inicio != null ? inicio : fechaFin.minusDays(7);

        List<Object[]> rawHoras = sesionJuegoRepository.findHorasInicioPorPeriodo(fechaInicio, fechaFin);

        // Agrupación en intervalos de 1 hora entre las 08:00 y las 20:00 (HU-40.02)
        Map<Integer, Long> conteoPorHora = new HashMap<>();
        for (int h = 8; h < 20; h++) {
            conteoPorHora.put(h, 0L);
        }

        long totalEnRango = 0;
        for (Object[] row : rawHoras) {
            LocalDateTime horaInicio = (LocalDateTime) row[1];
            if (horaInicio != null) {
                int hour = horaInicio.getHour();
                if (hour >= 8 && hour < 20) {
                    conteoPorHora.put(hour, conteoPorHora.get(hour) + 1);
                    totalEnRango++;
                }
            }
        }

        List<DatasetGraficoDTO.PuntoGraficoDTO> puntos = new ArrayList<>();
        for (int h = 8; h < 20; h++) {
            long conteo = conteoPorHora.get(h);
            double pct = totalEnRango > 0
                    ? BigDecimal.valueOf((conteo * 100.0) / totalEnRango).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            String etiqueta = String.format("%02d:00 - %02d:00", h, h + 1);
            puntos.add(DatasetGraficoDTO.PuntoGraficoDTO.builder()
                    .etiqueta(etiqueta)
                    .valor((double) conteo)
                    .porcentajeRelativo(pct)
                    .build());
        }

        DatasetGraficoDTO dataset = DatasetGraficoDTO.builder()
                .nombreSerie("Afluencia de Menores por Franja Horaria (08:00 - 20:00)")
                .puntos(puntos)
                .build();

        return GraficoTendenciaDTO.builder()
                .titulo("Horas Pico de Afluencia")
                .periodo(fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE) + " al " + fechaFin.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .totalAcumulado((double) totalEnRango)
                .dataset(dataset)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GraficoTendenciaDTO obtenerDistribucionMetodosPago(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime fechaFin = fin != null ? fin : LocalDateTime.now();
        LocalDateTime fechaInicio = inicio != null ? inicio : fechaFin.minusDays(30);

        List<Object[]> rawMetodos = ventaRepository.sumVentasPorMetodoPago(fechaInicio, fechaFin);

        BigDecimal totalRecaudado = BigDecimal.ZERO;
        Map<String, BigDecimal> montosPorMetodo = new LinkedHashMap<>();

        for (Object[] row : rawMetodos) {
            MetodoPago metodo = (MetodoPago) row[0];
            BigDecimal monto = (BigDecimal) row[1];
            if (metodo != null && monto != null) {
                montosPorMetodo.put(metodo.name(), monto);
                totalRecaudado = totalRecaudado.add(monto);
            }
        }

        List<DatasetGraficoDTO.PuntoGraficoDTO> puntos = new ArrayList<>();
        double totalDouble = totalRecaudado.doubleValue();

        for (Map.Entry<String, BigDecimal> entry : montosPorMetodo.entrySet()) {
            double monto = entry.getValue().doubleValue();
            double pct = totalDouble > 0
                    ? BigDecimal.valueOf((monto * 100.0) / totalDouble).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            puntos.add(DatasetGraficoDTO.PuntoGraficoDTO.builder()
                    .etiqueta(entry.getKey())
                    .valor(monto)
                    .porcentajeRelativo(pct)
                    .build());
        }

        DatasetGraficoDTO dataset = DatasetGraficoDTO.builder()
                .nombreSerie("Distribución de Ingresos por Método de Pago")
                .puntos(puntos)
                .build();

        return GraficoTendenciaDTO.builder()
                .titulo("Distribución por Métodos de Pago")
                .periodo(fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE) + " al " + fechaFin.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .totalAcumulado(totalDouble)
                .dataset(dataset)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GraficoTendenciaDTO obtenerEvolucionIngresosUltimos7Dias() {
        LocalDate hoy = LocalDate.now();
        LocalDate hace7Dias = hoy.minusDays(6);
        LocalDateTime inicio = hace7Dias.atStartOfDay();
        LocalDateTime fin = hoy.atTime(java.time.LocalTime.MAX);

        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicio, fin);

        Map<LocalDate, BigDecimal> ingresosPorDia = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            ingresosPorDia.put(hace7Dias.plusDays(i), BigDecimal.ZERO);
        }

        BigDecimal total7Dias = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            if (venta.getEstadoVenta() == EstadoVenta.PAGADO || venta.getEstadoVenta() == EstadoVenta.COMPLETADA) {
                LocalDate dia = venta.getFechaVenta().toLocalDate();
                BigDecimal actual = ingresosPorDia.getOrDefault(dia, BigDecimal.ZERO);
                BigDecimal nuevo = actual.add(venta.getMontoTotal() != null ? venta.getMontoTotal() : BigDecimal.ZERO);
                ingresosPorDia.put(dia, nuevo);
                total7Dias = total7Dias.add(venta.getMontoTotal() != null ? venta.getMontoTotal() : BigDecimal.ZERO);
            }
        }

        List<DatasetGraficoDTO.PuntoGraficoDTO> puntos = new ArrayList<>();
        double totalDouble = total7Dias.doubleValue();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDate dia = hace7Dias.plusDays(i);
            BigDecimal montoDia = ingresosPorDia.getOrDefault(dia, BigDecimal.ZERO);
            double val = montoDia.doubleValue();
            double pct = totalDouble > 0
                    ? BigDecimal.valueOf((val * 100.0) / totalDouble).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            puntos.add(DatasetGraficoDTO.PuntoGraficoDTO.builder()
                    .etiqueta(dia.format(dtf))
                    .valor(val)
                    .porcentajeRelativo(pct)
                    .build());
        }

        DatasetGraficoDTO dataset = DatasetGraficoDTO.builder()
                .nombreSerie("Evolución Diaria de Ventas")
                .puntos(puntos)
                .build();

        return GraficoTendenciaDTO.builder()
                .titulo("Evolución de Ingresos - Últimos 7 Días")
                .periodo(hace7Dias.format(dtf) + " al " + hoy.format(dtf))
                .totalAcumulado(totalDouble)
                .dataset(dataset)
                .build();
    }
}
