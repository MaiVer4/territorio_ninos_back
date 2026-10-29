package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.TipoTarifa;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "detalles_venta", indexes = {
    @Index(name = "idx_detalle_venta", columnList = "venta_id"),
    @Index(name = "idx_detalle_atraccion", columnList = "atraccion_id"),
    @Index(name = "idx_detalle_menor", columnList = "menor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menor_id")
    private Menor menor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarifa", nullable = false, length = 30)
    private TipoTarifa tipoTarifa;

    @Column(name = "tarifa_tiempo_id")
    private Long tarifaTiempoId;

    @Column(name = "tarifa_evento_id")
    private Long tarifaEventoId;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @PrePersist
    @PreUpdate
    protected void calcularSubtotalSiAplica() {
        if (this.cantidad == null || this.cantidad <= 0) {
            this.cantidad = 1;
        }
        if (this.precioUnitario != null && this.subtotal == null) {
            this.subtotal = this.precioUnitario
                    .multiply(BigDecimal.valueOf(this.cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}
