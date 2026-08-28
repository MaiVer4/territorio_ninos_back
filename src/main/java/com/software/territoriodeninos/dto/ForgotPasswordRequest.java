package com.software.territoriodeninos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "El email es requerido")
    @Email(message = "El formato del email es inválido")
    private String email;
}
