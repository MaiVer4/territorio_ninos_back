package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.DetalleTurnoAuditadoDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ConciliacionTurnoService {

    /**
     * Consulta y consolida el reporte contable de turnos cerrados en un rango de fechas (HU-36.03, HU-36.04).
     */
    List<ReporteTurnoDTO> obtenerReporteTurnos(LocalDateTime inicio, LocalDateTime fin, Long usuarioId);

    /**
     * Obtiene el desglose auditado detallado de un turno específico (ventas y boletas) (HU-36.06).
     */
    DetalleTurnoAuditadoDTO obtenerDetalleTurno(Long turnoId);

    /**
     * Obtiene métricas agregadas de rotación, demanda y tasa de ocupación por atracción (HU-37.01, HU-37.03).
     */
    List<RotacionAtraccionDTO> obtenerRotacionAtracciones(LocalDateTime inicio, LocalDateTime fin, String periodo, String criterioOrden);
}
