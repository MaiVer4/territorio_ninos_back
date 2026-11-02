package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.GraficoTendenciaDTO;

import java.time.LocalDateTime;

public interface EstadisticasReporteService {

    /**
     * Calcula la afluencia agregada en intervalos de 1 hora entre las 08:00 y las 20:00 (HU-40.02).
     */
    GraficoTendenciaDTO obtenerHorasPico(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Calcula la distribución monetaria y porcentual por método de pago (HU-40.03).
     */
    GraficoTendenciaDTO obtenerDistribucionMetodosPago(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Calcula la evolución diaria de ingresos facturados en los últimos 7 días (HU-40.04).
     */
    GraficoTendenciaDTO obtenerEvolucionIngresosUltimos7Dias();
}
