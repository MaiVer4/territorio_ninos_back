package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoAtraccion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atracciones")
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private com.software.territoriodeninos.util.TipoAtraccion tipo = com.software.territoriodeninos.util.TipoAtraccion.TEMPORIZADA;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacidadMaxima = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer duracionMinutosBase = 15;

    @Column(nullable = false)
    @Builder.Default
    private Integer toleranciaMinutos = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer caidasLimite = 3;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoAtraccion.ACTIVO;
        }
        if (this.tipo == null) {
            this.tipo = com.software.territoriodeninos.util.TipoAtraccion.TEMPORIZADA;
        }
        if (this.capacidadMaxima == null) {
            this.capacidadMaxima = 1;
        }
        if (this.duracionMinutosBase == null) {
            this.duracionMinutosBase = 15;
        }
        if (this.toleranciaMinutos == null) {
            this.toleranciaMinutos = 0;
        }
        if (this.caidasLimite == null) {
            this.caidasLimite = 3;
        }
    }
}
