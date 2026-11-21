package com.software.territoriodeninos.dto.tienda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.software.territoriodeninos.util.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Solicitud de compra online de tickets por acudiente invitado (Guest Checkout)")
public record CompraOnlineRequestDTO(
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 4, max = 20, message = "El documento debe tener entre 4 y 20 caracteres")
    @JsonAlias({"documentoAdulto", "numeroDocumento"})
    @Schema(example = "1098765432")
    String documento,

    @NotBlank(message = "El nombre del acudiente es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @JsonAlias({"nombreAdulto", "nombre"})
    @Schema(example = "Carlos")
    String nombres,

    @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
    @JsonAlias({"apellidoAdulto", "apellido"})
    @Schema(example = "Mendoza")
    String apellidos,

    @NotBlank(message = "El celular de contacto es obligatorio")
    @Size(min = 7, max = 20, message = "El celular debe tener entre 7 y 20 caracteres")
    @JsonAlias({"telefonoAdulto", "celular"})
    @Schema(example = "3101234567")
    String telefono,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Size(max = 100, message = "El correo no puede superar 100 caracteres")
    @JsonAlias({"correoAdulto", "email"})
    @Schema(example = "carlos.mendoza@gmail.com")
    String correo,

    @NotNull(message = "El método de pago es obligatorio")
    @Schema(example = "NEQUI")
    MetodoPago metodoPago,

    @NotEmpty(message = "Debes seleccionar al menos un ticket para comprar")
    @Size(max = 20, message = "No se pueden incluir más de 20 ítems por orden")
    @Valid
    List<ItemCompraOnlineDTO> items,

    @Size(max = 200, message = "Campo inválido")
    @Schema(description = "Campo trampa Honeypot para protección contra bots (debe dejarse vacío)")
    String website
) {}
