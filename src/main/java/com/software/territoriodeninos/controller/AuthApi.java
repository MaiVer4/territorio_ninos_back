package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@Tag(
    name = "Autenticación",
    description = """
        Endpoints para autenticación y recuperación de contraseña.

        **Flujo de recuperación de contraseña (2 pasos):**
        1. `POST /forgot-password` → El usuario ingresa su correo. El sistema genera un token UUID \\
        con vigencia de 15 minutos y lo envía por email.
        2. `PATCH /reset-password` → El usuario utiliza el token recibido en el correo junto con \\
        su nueva contraseña para completar el cambio.
        """
)
public interface AuthApi {

    @Operation(
        summary = "Iniciar sesión",
        description = """
            Autentica al usuario con correo y contraseña.
            Devuelve un **token JWT Bearer** que debe incluirse en el header \\
            `Authorization: Bearer <token>` en todas las peticiones protegidas.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa. Se devuelve el token JWT.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "token": "eyJhbGciOiJIUzI1NiJ9...",
                      "tipoToken": "Bearer",
                      "correo": "admin@ejemplo.com",
                      "rol": "ADMINISTRADOR"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos.",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (campos vacíos o formato incorrecto).",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO);

    @Operation(
        summary = "Paso 1 — Solicitar enlace de recuperación",
        description = """
            **Primer paso** del flujo de recuperación de contraseña.

            El usuario envía su correo electrónico. Si el correo está registrado y activo en el sistema, \\
            se generará un token UUID con vigencia de **15 minutos** y se enviará un enlace de \\
            recuperación al correo indicado.

            > ⚠️ La respuesta siempre devuelve HTTP 200 independientemente de si el correo existe o no, \\
            para evitar la enumeración de usuarios registrados (seguridad).

            **Siguiente paso:** usar el token recibido en el correo en `PATCH /reset-password`.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Solicitud procesada. Si el correo existe, se enviará el enlace.",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\\n  \\\"mensaje\\\": \\\"Si el correo existe, se enviará un enlace de recuperación.\\\"\\n}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "El campo email es inválido o está vacío.",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

    @Operation(
        summary = "Paso 2 — Confirmar nueva contraseña con token",
        description = """
            **Segundo y último paso** del flujo de recuperación de contraseña.

            El usuario envía el **token UUID** que recibió en su correo (generado en `POST /forgot-password`) \\
            junto con su nueva contraseña. El sistema valida que el token:
            - Exista en la base de datos.
            - No haya expirado (vigencia de 15 minutos).

            Si la validación es exitosa, la contraseña se actualiza y el token queda invalidado \\
            (no puede usarse nuevamente).

            > ℹ️ Tras cambiar la contraseña, el usuario debe iniciar sesión nuevamente con `POST /login`.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contraseña actualizada exitosamente. El token queda invalidado.",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\\n  \\\"mensaje\\\": \\\"Contraseña actualizada exitosamente.\\\"\\n}")
            )
        ),
        @ApiResponse(responseCode = "401",
            description = "Token inválido, inexistente o expirado. Solicite un nuevo enlace con `POST /forgot-password`.",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400",
            description = "Datos inválidos: token vacío o contraseña con menos de 8 caracteres.",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request);

    @Operation(
        summary = "Cerrar sesión",
        description = """
            Cierra la sesión del usuario actual. 
            Valida reglas de negocio (ej: si un TAQUILLERO tiene caja activa no puede cerrar sesión).
            Limpia el SecurityContext local.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sesión cerrada exitosamente.",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "No se puede cerrar sesión por reglas de negocio (ej: caja abierta).",
            content = @Content(mediaType = "application/json")
        )
    })
    ResponseEntity<?> logout();
}
