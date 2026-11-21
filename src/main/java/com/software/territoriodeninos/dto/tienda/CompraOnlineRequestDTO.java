package com.software.territoriodeninos.dto.tienda;

import com.software.territoriodeninos.util.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Solicitud de compra online de tickets por acudiente invitado (Guest Checkout)")
public record CompraOnlineRequestDTO(
    @NotBlank(message = "El número de documento es obligatorio")
    @Schema(example = "1098765432")
    String documento,

    @NotBlank(message = "El nombre del acudiente es obligatorio")
    @Schema(example = "Carlos")
    String nombres,

    @NotBlank(message = "Los apellidos del acudiente son obligatorios")
    @Schema(example = "Mendoza")
    String apellidos,

    @NotBlank(message = "El celular de contacto es obligatorio")
    @Schema(example = "3101234567")
    String telefono,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Schema(example = "carlos.mendoza@gmail.com")
    String correo,

    @NotNull(message = "El método de pago es obligatorio")
    @Schema(example = "NEQUI")
    MetodoPago metodoPago,

    @NotEmpty(message = "Debes seleccionar al menos un ticket para comprar")
    @Valid
    List<ItemCompraOnlineDTO> items,

    @Schema(description = "Campo trampa Honeypot para protección contra bots (debe dejarse vacío)")
    String website
) {}
