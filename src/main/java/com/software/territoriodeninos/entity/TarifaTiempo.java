package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarifas_tiempo", uniqueConstraints = {
    @UniqueConstraint(name = "uq_atraccion_duracion", columnNames = {"atraccion_id", "duracion_minutos"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaTiempo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }
}
