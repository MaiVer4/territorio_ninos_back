package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ChangePasswordRequest;
import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios (CRUD)")
@SecurityRequirement(name = "Bearer Token")
public interface UsuarioApi {

    @Operation(summary = "Registrar nuevo usuario", description = "Crear un nuevo usuario en el sistema (solo ADMINISTRADOR)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para crear usuario"),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado")
    })
    ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRegistroDTO registroDTO);

    @Operation(summary = "Obtener todos los usuarios", description = "Obtiene la lista de todos los usuarios activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    ResponseEntity<List<UsuarioResponseDTO>> obtenerTodos();

    @Operation(summary = "Obtener usuario por ID", description = "Obtiene un usuario específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id);

    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente (ADMINISTRADOR y TAQUILLERO)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para actualizar usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "Correo ya en uso")
    })
    ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody UsuarioUpdateDTO updateDTO);

    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario tras validar la actual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña actual incorrecta"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    ResponseEntity<?> cambiarPassword(@PathVariable Long id,
                                      @Valid @RequestBody ChangePasswordRequest request);

    @Operation(summary = "Inactivar usuario", description = "Inactiva un usuario del sistema (cambio a INACTIVO, solo ADMINISTRADOR)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario inactivado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para inactivar usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    ResponseEntity<UsuarioResponseDTO> inactivar(@PathVariable Long id);

    @Operation(summary = "Activar usuario", description = "Activa un usuario del sistema (cambio a ACTIVO, solo ADMINISTRADOR)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario activado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para activar usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    ResponseEntity<UsuarioResponseDTO> activar(@PathVariable Long id);

    @Operation(summary = "Obtener usuarios por rol", description = "Obtiene la lista de usuarios filtrada por rol (ADMINISTRADOR, TAQUILLERO, OPERARIO)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista filtrada por rol obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Rol inválido"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    ResponseEntity<List<UsuarioResponseDTO>> obtenerPorRol(@PathVariable String rol);

    @Operation(summary = "Eliminar usuario permanentemente", description = "Elimina un usuario del sistema de forma definitiva (solo ADMINISTRADOR)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para eliminar usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    ResponseEntity<Void> eliminar(@PathVariable Long id);
}
