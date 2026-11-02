package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoSesionJuego;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_juego", indexes = {
    @Index(name = "idx_sesion_atraccion_estado", columnList = "atraccion_id, estado_sesion"),
    @Index(name = "idx_sesion_hora_fin", columnList = "hora_fin"),
    @Index(name = "idx_sesion_ticket", columnList = "ticket_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionJuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menor_id")
    private Menor menor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id", nullable = false)
    private Usuario operario;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Column(name = "duracion_segundos", nullable = false)
    @Builder.Default
    private Integer duracionSegundos = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sesion", nullable = false, length = 30)
    @Builder.Default
    private EstadoSesionJuego estadoSesion = EstadoSesionJuego.EN_CURSO;

    @Column(name = "tiempo_restante_pausa")
    private Integer tiempoRestantePausa;

    @Column(name = "fecha_pausa")
    private LocalDateTime fechaPausa;

    @Column(name = "fecha_salida_real")
    private LocalDateTime fechaSalidaReal;

    @Column(name = "segundos_exceso", nullable = false)
    @Builder.Default
    private Integer segundosExceso = 0;

    @Column(name = "intentos_totales")
    private Integer intentosTotales;

    @Column(name = "intentos_restantes")
    private Integer intentosRestantes;

    @Column(name = "alerta_pre_expiracion_enviada", nullable = false)
    @Builder.Default
    private Boolean alertaPreExpiracionEnviada = false;

    @Column(name = "alerta_expiracion_enviada", nullable = false)
    @Builder.Default
    private Boolean alertaExpiracionEnviada = false;

    @PrePersist
    protected void onCreate() {
        if (this.horaInicio == null) {
            this.horaInicio = LocalDateTime.now();
        }
        if (this.estadoSesion == null) {
            this.estadoSesion = EstadoSesionJuego.EN_CURSO;
        }
        if (this.segundosExceso == null) {
            this.segundosExceso = 0;
        }
        if (this.alertaPreExpiracionEnviada == null) {
            this.alertaPreExpiracionEnviada = false;
        }
        if (this.alertaExpiracionEnviada == null) {
            this.alertaExpiracionEnviada = false;
        }
    }
}
