package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoAtraccion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atracciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atraccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaMinuto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAtraccion estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoAtraccion.ACTIVO;
        }
    }
}
