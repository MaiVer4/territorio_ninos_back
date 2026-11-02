package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "log_escaneo_qr", indexes = {
    @Index(name = "idx_log_escaneo_uuid", columnList = "uuid_leido"),
    @Index(name = "idx_log_escaneo_operario", columnList = "operario_id"),
    @Index(name = "idx_log_escaneo_atraccion", columnList = "atraccion_id"),
    @Index(name = "idx_log_escaneo_fecha", columnList = "fecha_escaneo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEscaneoQr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_leido", nullable = false, updatable = false)
    private UUID uuidLeido;

    @Column(name = "ticket_encontrado", nullable = false, updatable = false)
    @Builder.Default
    private Boolean ticketEncontrado = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atraccion_id", nullable = false, updatable = false)
    private Atraccion atraccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id", nullable = false, updatable = false)
    private Usuario operario;

    @Column(name = "direccion_ip", length = 64, updatable = false)
    private String direccionIp;

    @Column(name = "timestamp_cliente", updatable = false)
    private LocalDateTime timestampCliente;

    @Column(name = "fecha_escaneo", nullable = false, updatable = false)
    private LocalDateTime fechaEscaneo;

    @Column(name = "resultado", nullable = false, length = 40, updatable = false)
    private String resultado;

    @Column(name = "detalle", length = 500, updatable = false)
    private String detalle;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEscaneo == null) {
            this.fechaEscaneo = LocalDateTime.now();
        }
        if (this.ticketEncontrado == null) {
            this.ticketEncontrado = false;
        }
    }
}
