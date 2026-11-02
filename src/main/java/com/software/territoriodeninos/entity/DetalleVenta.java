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
    @JoinColumn(name = "venta_id", nullable = false, updatable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atraccion_id", nullable = false, updatable = false)
    private Atraccion atraccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menor_id", updatable = false)
    private Menor menor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarifa", nullable = false, length = 30, updatable = false)
    private TipoTarifa tipoTarifa;

    @Column(name = "tarifa_tiempo_id", updatable = false)
    private Long tarifaTiempoId;

    @Column(name = "tarifa_evento_id", updatable = false)
    private Long tarifaEventoId;

    @Column(name = "cantidad", nullable = false, updatable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal subtotal;

    @OneToMany(mappedBy = "detalleVenta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<Ticket> tickets = new java.util.ArrayList<>();

    public void addTicket(Ticket ticket) {
        if (this.tickets == null) {
            this.tickets = new java.util.ArrayList<>();
        }
        this.tickets.add(ticket);
        ticket.setDetalleVenta(this);
    }

    @PrePersist
    protected void onCreate() {
        if (this.cantidad == null || this.cantidad <= 0) {
            this.cantidad = 1;
        }
        if (this.precioUnitario != null && this.subtotal == null) {
            this.subtotal = this.precioUnitario
                    .multiply(BigDecimal.valueOf(this.cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.venta != null && Boolean.TRUE.equals(this.venta.getCongelada())) {
            throw new com.software.territoriodeninos.exception.ReglaNegocioException(
                    "Los detalles de una venta ya procesada y congelada no pueden ser modificados."
            );
        }
    }
}
