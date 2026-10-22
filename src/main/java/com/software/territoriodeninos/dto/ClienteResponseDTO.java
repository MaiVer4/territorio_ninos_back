package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.entity.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO de respuesta con los datos de un cliente y sus menores")
public record ClienteResponseDTO(
    @Schema(description = "ID único del cliente", example = "1")
    Long id,

    @Schema(description = "Número de documento del cliente", example = "1098765432")
    String numeroDocumento,

    @Schema(description = "Nombres del cliente", example = "Carlos Andrés")
    String nombres,

    @Schema(description = "Apellidos del cliente", example = "Pérez Gómez")
    String apellidos,

    @Schema(description = "Teléfono de contacto", example = "+57 300 123 4567")
    String telefono,

    @Schema(description = "Correo electrónico", example = "carlos.perez@example.com")
    String correo,

    @Schema(description = "Fecha y hora de registro en el sistema")
    LocalDateTime fechaRegistro,

    @Schema(description = "Lista de menores de edad asociados")
    List<MenorDTO> menores
) {

    @JsonProperty("nombreCompleto")
    @Schema(description = "Nombre completo unificado", example = "Carlos Andrés Pérez Gómez")
    public String nombreCompleto() {
        String n = nombres != null ? nombres.trim() : "";
        String a = apellidos != null ? apellidos.trim() : "";
        return (n + " " + a).trim();
    }

    @JsonProperty("documento")
    @Schema(description = "Alias para número de documento", example = "1098765432")
    public String documento() {
        return numeroDocumento;
    }

    @JsonProperty("email")
    @Schema(description = "Alias para correo electrónico", example = "carlos.perez@example.com")
    public String email() {
        return correo;
    }

    public static ClienteResponseDTO fromEntity(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        List<MenorDTO> menoresDTO = (cliente.getMenores() != null)
                ? cliente.getMenores().stream().map(MenorDTO::fromEntity).toList()
                : Collections.emptyList();

        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNumeroDocumento(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getFechaRegistro(),
                menoresDTO
        );
    }
}
