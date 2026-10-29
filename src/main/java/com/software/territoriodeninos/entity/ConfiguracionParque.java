package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracion_parque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionParque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreParque;

    private String nombreComercial;

    private String razonSocial;

    @Column(nullable = false)
    private String nit;

    private String direccion;

    private String telefono;

    private String ciudad;

    @Column(length = 2000)
    private String politicasSeguridad;

    @Builder.Default
    private Integer anchoPapelMm = 80;

    private String mensajePie;

    @Column(length = 2000)
    private String pieTicketLegal;

    @Column(nullable = false)
    @Builder.Default
    private Integer tiempoGraciaGeneralSegundos = 60;

    @Column(name = "aplica_impuesto", nullable = false)
    @Builder.Default
    private Boolean aplicaImpuesto = true;

    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal porcentajeImpuesto = BigDecimal.ZERO;
}
