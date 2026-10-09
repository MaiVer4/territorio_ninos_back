package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String nit;

    private String direccion;

    private String telefono;

    @Column(length = 1000)
    private String pieTicketLegal;

    @Column(nullable = false)
    @Builder.Default
    private Integer tiempoGraciaGeneralSegundos = 60;
}
