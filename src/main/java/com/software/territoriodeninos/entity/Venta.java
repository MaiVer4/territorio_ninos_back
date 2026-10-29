package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas", indexes = {
    @Index(name = "idx_venta_turno_caja", columnList = "turno_caja_id, fecha_venta DESC"),
    @Index(name = "idx_venta_cliente", columnList = "cliente_id"),
    @Index(name = "idx_venta_fecha", columnList = "fecha_venta DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_caja_id", nullable = false)
    private TurnoCaja turnoCaja;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "impuestos", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @Column(name = "cambio", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cambio = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_venta", nullable = false, length = 20)
    @Builder.Default
    private EstadoVenta estadoVenta = EstadoVenta.COMPLETADA;

    @Column(name = "fecha_venta", nullable = false, updatable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.fechaVenta == null) {
            this.fechaVenta = LocalDateTime.now();
        }
        if (this.estadoVenta == null) {
            this.estadoVenta = EstadoVenta.COMPLETADA;
        }
        if (this.descuento == null) {
            this.descuento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.impuestos == null) {
            this.impuestos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.montoTotal == null) {
            this.montoTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public void addDetalle(DetalleVenta detalle) {
        if (this.detalles == null) {
            this.detalles = new ArrayList<>();
        }
        this.detalles.add(detalle);
        detalle.setVenta(this);
    }

    public void removeDetalle(DetalleVenta detalle) {
        if (this.detalles != null) {
            this.detalles.remove(detalle);
            detalle.setVenta(null);
        }
    }
}
