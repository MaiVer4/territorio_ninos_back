package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @Schema(description = "Número de documento de identidad del acudiente", example = "1098765432")
    String numeroDocumento,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Schema(description = "Nombres del cliente", example = "Carlos Andrés")
    String nombres,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Schema(description = "Apellidos del cliente", example = "Pérez Gómez")
    String apellidos,

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^[0-9+()\\s-]*$", message = "Formato de teléfono no válido")
    @Schema(description = "Número de teléfono o celular", example = "+57 300 123 4567")
    String telefono,

    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    @Schema(description = "Correo electrónico de contacto", example = "carlos.perez@example.com")
    String correo,

    @Valid
    @Schema(description = "Lista opcional de menores de edad a cargo")
    List<MenorDTO> menores
) {

    @JsonCreator
    public static ClienteRequestDTO fromJson(
        @JsonProperty("numeroDocumento") @JsonAlias({"documento", "numero_documento", "doc", "identificacion"}) String numeroDocumento,
        @JsonProperty("nombres") @JsonAlias({"nombre"}) String nombres,
        @JsonProperty("apellidos") @JsonAlias({"apellido"}) String apellidos,
        @JsonProperty("nombreCompleto") @JsonAlias({"nombre_completo"}) String nombreCompleto,
        @JsonProperty("telefono") @JsonAlias({"tel", "celular", "movil"}) String telefono,
        @JsonProperty("correo") @JsonAlias({"email", "correoElectronico", "correo_electronico"}) String correo,
        @JsonProperty("menores") @JsonAlias({"hijos", "ninos", "acompaniantes"}) List<MenorDTO> menores
    ) {
        String finalNombres = nombres != null ? nombres.trim() : null;
        String finalApellidos = apellidos != null ? apellidos.trim() : null;

        if ((finalNombres == null || finalNombres.isBlank()) && nombreCompleto != null && !nombreCompleto.isBlank()) {
            String[] partes = nombreCompleto.trim().split("\\s+", 2);
            finalNombres = partes[0];
            finalApellidos = partes.length > 1 ? partes[1] : ".";
        }

        return new ClienteRequestDTO(
            numeroDocumento,
            finalNombres,
            finalApellidos,
            telefono,
            correo,
            menores
        );
    }

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
