package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.entity.ReporteAtraccion;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.SeveridadReporte;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Detalle de un reporte de novedad (daño o falta de suplementos) de una atracción")
public record ReporteAtraccionResponseDTO(
    Long id,
    Long atraccionId,
    String atraccionNombre,
    String atraccionEstadoActual,
    Long usuarioReportaId,
    String usuarioReportaNombre,
    TipoReporteAtraccion tipoReporte,
    SeveridadReporte severidad,
    String descripcion,
    EstadoReporteAtraccion estado,
    Boolean atraccionDesactivada,
    LocalDateTime fechaReporte,
    LocalDateTime fechaResolucion,
    Long usuarioResuelveId,
    String usuarioResuelveNombre,
    String observacionesResolucion
) {
    public static ReporteAtraccionResponseDTO fromEntity(ReporteAtraccion r) {
        return new ReporteAtraccionResponseDTO(
                r.getId(),
                r.getAtraccion() != null ? r.getAtraccion().getId() : null,
                r.getAtraccion() != null ? r.getAtraccion().getNombre() : null,
                r.getAtraccion() != null && r.getAtraccion().getEstado() != null ? r.getAtraccion().getEstado().name() : null,
                r.getUsuarioReporta() != null ? r.getUsuarioReporta().getId() : null,
                r.getUsuarioReporta() != null ? r.getUsuarioReporta().getNombre() : null,
                r.getTipoReporte(),
                r.getSeveridad(),
                r.getDescripcion(),
                r.getEstado(),
                Boolean.TRUE.equals(r.getAtraccionDesactivada()),
                r.getFechaReporte(),
                r.getFechaResolucion(),
                r.getUsuarioResuelve() != null ? r.getUsuarioResuelve().getId() : null,
                r.getUsuarioResuelve() != null ? r.getUsuarioResuelve().getNombre() : null,
                r.getObservacionesResolucion()
        );
    }
}
