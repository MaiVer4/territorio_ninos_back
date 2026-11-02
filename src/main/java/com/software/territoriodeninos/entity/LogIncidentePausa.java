package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_incidente_pausa", indexes = {
    @Index(name = "idx_log_pausa_sesion", columnList = "sesion_juego_id"),
    @Index(name = "idx_log_pausa_operario", columnList = "operario_id"),
    @Index(name = "idx_log_pausa_fecha_atraccion", columnList = "fecha_inicio_pausa, atraccion_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogIncidentePausa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_juego_id", nullable = false)
    private SesionJuego sesionJuego;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atraccion_id")
    private Atraccion atraccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id", nullable = false)
    private Usuario operario;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "tipo_pausa", length = 50)
    @Builder.Default
    private String tipoPausa = "OPERATIVA";

    @Column(name = "es_pausa_general", nullable = false)
    @Builder.Default
    private Boolean esPausaGeneral = false;

    @Column(name = "fecha_inicio_pausa", nullable = false)
    private LocalDateTime fechaInicioPausa;

    @Column(name = "fecha_fin_pausa")
    private LocalDateTime fechaFinPausa;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @PrePersist
    protected void onCreate() {
        if (this.fechaInicioPausa == null) {
            this.fechaInicioPausa = LocalDateTime.now();
        }
        if (this.esPausaGeneral == null) {
            this.esPausaGeneral = false;
        }
    }
}
