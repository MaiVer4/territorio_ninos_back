package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.exception.ReglaNegocioException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Entidad JPA inmutable para auditoría de reimpresión de tickets (HU-22.01, HU-22.02 / Notion: CA-03).
 * Registra id, ticket_id, usuario_id, motivo y fechaReimpresion impidiendo cualquier mutación o eliminación.
 */
@Entity
@Immutable
@Table(
    name = "log_reimpresion",
    indexes = {
        @Index(name = "idx_log_reimpresion_ticket", columnList = "ticket_id"),
        @Index(name = "idx_log_reimpresion_usuario", columnList = "usuario_id"),
        @Index(name = "idx_log_reimpresion_fecha", columnList = "fecha_reimpresion DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogReimpresion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", referencedColumnName = "uuid_codigo", nullable = false, updatable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "motivo", nullable = false, length = 500, updatable = false)
    private String motivo;

    @Column(name = "fecha_reimpresion", nullable = false, updatable = false)
    private LocalDateTime fechaReimpresion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaReimpresion == null) {
            this.fechaReimpresion = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        throw new ReglaNegocioException("Los registros de auditoría de reimpresión son inmutables y no pueden ser modificados.");
    }

    @PreRemove
    protected void onRemove() {
        throw new ReglaNegocioException("Los registros de auditoría de reimpresión son inmutables y no pueden ser eliminados.");
    }
}
