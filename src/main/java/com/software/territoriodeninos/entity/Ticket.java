package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoTicket;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "tickets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_ticket_uuid_codigo", columnNames = "uuid_codigo")
    },
    indexes = {
        @Index(name = "idx_ticket_uuid_codigo", columnList = "uuid_codigo"),
        @Index(name = "idx_ticket_detalle_venta", columnList = "detalle_venta_id"),
        @Index(name = "idx_ticket_menor", columnList = "menor_id"),
        @Index(name = "idx_ticket_estado", columnList = "estado_ticket")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket implements Persistable<UUID> {

    @Id
    @Column(name = "uuid_codigo", nullable = false, updatable = false)
    private UUID uuidCodigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_venta_id", nullable = false, updatable = false)
    private DetalleVenta detalleVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menor_id", updatable = false)
    private Menor menor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_ticket", nullable = false, length = 20)
    @Builder.Default
    private EstadoTicket estadoTicket = EstadoTicket.PAGADO;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return uuidCodigo;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDateTime.now();
        }
        if (this.estadoTicket == null) {
            this.estadoTicket = EstadoTicket.PAGADO;
        }
    }
}
