package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "menores", indexes = {
    @Index(name = "idx_menor_documento", columnList = "numero_documento"),
    @Index(name = "idx_menor_cliente", columnList = "cliente_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "edad")
    private Integer edad;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        calcularEdadSiAplica();
    }

    @PreUpdate
    protected void onUpdate() {
        calcularEdadSiAplica();
    }

    public void calcularEdadSiAplica() {
        if (this.fechaNacimiento != null) {
            this.edad = Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
        }
    }

    @Transient
    public Integer getEdadActual() {
        if (this.fechaNacimiento != null) {
            return Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
        }
        return this.edad;
    }
}
