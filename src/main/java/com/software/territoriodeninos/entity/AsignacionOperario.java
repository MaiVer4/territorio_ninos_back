package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoAsignacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asignaciones_operario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionOperario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoAsignacion estado = EstadoAsignacion.ACTIVA;

    @Column(length = 255)
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDate.now();
        }
        if (this.horaInicio == null) {
            this.horaInicio = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoAsignacion.ACTIVA;
        }
    }
}
