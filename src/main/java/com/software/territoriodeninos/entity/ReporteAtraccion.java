package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.SeveridadReporte;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_atraccion", indexes = {
    @Index(name = "idx_reporte_atraccion_atraccion", columnList = "atraccion_id"),
    @Index(name = "idx_reporte_atraccion_estado", columnList = "estado"),
    @Index(name = "idx_reporte_atraccion_fecha", columnList = "fecha_reporte")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteAtraccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_reporta_id", nullable = false)
    private Usuario usuarioReporta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", nullable = false, length = 40)
    private TipoReporteAtraccion tipoReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, length = 20)
    @Builder.Default
    private SeveridadReporte severidad = SeveridadReporte.MEDIA;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoReporteAtraccion estado = EstadoReporteAtraccion.ABIERTO;

    @Column(name = "atraccion_desactivada", nullable = false)
    @Builder.Default
    private Boolean atraccionDesactivada = false;

    @Column(name = "fecha_reporte", nullable = false, updatable = false)
    private LocalDateTime fechaReporte;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_resuelve_id")
    private Usuario usuarioResuelve;

    @Column(name = "observaciones_resolucion", length = 1000)
    private String observacionesResolucion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaReporte == null) {
            this.fechaReporte = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoReporteAtraccion.ABIERTO;
        }
        if (this.severidad == null) {
            this.severidad = SeveridadReporte.MEDIA;
        }
        if (this.atraccionDesactivada == null) {
            this.atraccionDesactivada = false;
        }
    }
}
