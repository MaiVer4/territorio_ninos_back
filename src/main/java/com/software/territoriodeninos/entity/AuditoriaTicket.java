package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria_tickets", indexes = {
    @Index(name = "idx_auditoria_ticket_uuid", columnList = "ticket_uuid, fecha_evento ASC"),
    @Index(name = "idx_auditoria_ticket_fecha", columnList = "fecha_evento DESC"),
    @Index(name = "idx_auditoria_ticket_usuario", columnList = "usuario_id")
})
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditoriaTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_uuid", nullable = false, updatable = false)
    private UUID ticketUuid;

    @Column(name = "estado_origen", length = 30, updatable = false)
    private String estadoOrigen;

    @Column(name = "estado_destino", nullable = false, length = 30, updatable = false)
    private String estadoDestino;

    @Column(name = "motivo", length = 500, updatable = false)
    private String motivo;

    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(name = "dispositivo_info", length = 255, updatable = false)
    private String dispositivoInfo;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEvento == null) {
            this.fechaEvento = LocalDateTime.now();
        }
    }
}
