package com.software.territoriodeninos.service;

import java.time.LocalDateTime;

public interface ExportacionReporteService {

    /**
     * Genera un documento PDF formateado con encabezado institucional dinámico, paginación y formato de moneda (HU-39.02, HU-39.03).
     */
    byte[] exportarPdf(String tipoReporte, LocalDateTime inicio, LocalDateTime fin, Long usuarioId, Long atraccionId);

    /**
     * Genera un archivo CSV con codificación UTF-8 y marca BOM para compatibilidad con Microsoft Excel (HU-39.04).
     */
    byte[] exportarCsv(String tipoReporte, LocalDateTime inicio, LocalDateTime fin, Long usuarioId, Long atraccionId);
}
