package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.software.territoriodeninos.entity.Menor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.Period;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para datos de un menor a cargo")
public record MenorDTO(
    @Schema(description = "ID del menor en base de datos", example = "1")
    Long id,

    @Size(max = 20, message = "El documento del menor no puede exceder 20 caracteres")
    @JsonAlias({"documento", "numero_documento", "doc", "identificacion"})
    @Schema(description = "Número de documento del menor (TI, RC, etc.)", example = "1020304050")
    String numeroDocumento,

    @NotBlank(message = "El nombre completo del menor es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre completo debe tener entre 2 y 150 caracteres")
    @JsonAlias({"nombre", "nombre_completo"})
    @Schema(description = "Nombre y apellidos completos del menor", example = "Mateo Gómez")
    String nombreCompleto,

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @JsonAlias({"fecha_nacimiento", "fechaNac"})
    @Schema(description = "Fecha de nacimiento del menor", example = "2018-05-15")
    LocalDate fechaNacimiento,

    @Min(value = 0, message = "La edad mínima es 0")
    @Max(value = 17, message = "La edad máxima para un menor es 17 años")
    @Schema(description = "Edad del menor en años", example = "6")
    Integer edad
) {

    public Menor toEntity() {
        Integer calculatedEdad = this.edad;
        if (this.fechaNacimiento != null) {
            calculatedEdad = Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
        }

        return Menor.builder()
                .id(this.id)
                .numeroDocumento(this.numeroDocumento != null ? this.numeroDocumento.trim() : null)
                .nombreCompleto(this.nombreCompleto != null ? this.nombreCompleto.trim() : null)
                .fechaNacimiento(this.fechaNacimiento)
                .edad(calculatedEdad)
                .build();
    }

    public static MenorDTO fromEntity(Menor m) {
        if (m == null) {
            return null;
        }
        Integer edad = m.getEdad();
        if (edad == null && m.getFechaNacimiento() != null) {
            edad = Period.between(m.getFechaNacimiento(), LocalDate.now()).getYears();
        }
        return new MenorDTO(
                m.getId(),
                m.getNumeroDocumento(),
                m.getNombreCompleto(),
                m.getFechaNacimiento(),
                edad
        );
    }
}
