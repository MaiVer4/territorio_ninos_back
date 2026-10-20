package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoTurnoCaja;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_caja", indexes = {
    @Index(name = "idx_turno_usuario_estado", columnList = "usuario_id, estado, fecha_apertura DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "monto_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoBase;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "ventas_efectivo_sistema", precision = 12, scale = 2)
    private BigDecimal ventasEfectivoSistema;

    @Column(name = "monto_efectivo_reportado", precision = 12, scale = 2)
    private BigDecimal montoEfectivoReportado;

    @Column(name = "diferencia", precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTurnoCaja estado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    public void prePersist() {
        if (this.fechaApertura == null) {
            this.fechaApertura = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoTurnoCaja.ABIERTO;
        }
        if (this.ventasEfectivoSistema == null) {
            this.ventasEfectivoSistema = BigDecimal.ZERO;
        }
    }
}
