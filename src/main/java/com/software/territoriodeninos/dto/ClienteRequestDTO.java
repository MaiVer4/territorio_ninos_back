package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para registro o creación de un cliente / acudiente")
public record ClienteRequestDTO(

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 3, max = 20, message = "El documento debe tener entre 3 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "El documento solo puede contener letras, números y guiones")
    @JsonAlias({"documento", "numero_documento", "doc", "identificacion"})
    @Schema(description = "Número de documento de identidad del acudiente", example = "1098765432")
    String numeroDocumento,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @JsonAlias({"nombre"})
    @Schema(description = "Nombres del cliente", example = "Carlos Andrés")
    String nombres,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @JsonAlias({"apellido"})
    @Schema(description = "Apellidos del cliente", example = "Pérez Gómez")
    String apellidos,

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^[0-9+()\\s-]*$", message = "Formato de teléfono no válido")
    @JsonAlias({"tel", "celular", "movil"})
    @Schema(description = "Número de teléfono o celular", example = "+57 300 123 4567")
    String telefono,

    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    @JsonAlias({"email", "correoElectronico", "correo_electronico"})
    @Schema(description = "Correo electrónico de contacto", example = "carlos.perez@example.com")
    String correo,

    @Valid
    @JsonAlias({"hijos", "ninos", "acompaniantes"})
    @Schema(description = "Lista opcional de menores de edad a cargo")
    List<MenorDTO> menores
) {
    public ClienteRequestDTO(
        String numeroDocumento,
        String nombres,
        String apellidos,
        String telefono,
        String correo
    ) {
        this(numeroDocumento, nombres, apellidos, telefono, correo, null);
    }
}
