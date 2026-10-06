package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;
import com.software.territoriodeninos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios (CRUD)")
@SecurityRequirement(name = "Bearer Token")
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Registrar nuevo usuario", description = "Crear un nuevo usuario en el sistema (solo ADMINISTRADOR)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para crear usuario"),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado")
    })
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRegistroDTO registroDTO) {
        UsuarioResponseDTO usuarioCreado = usuarioService.registrar(registroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCreado);
    }
    
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    @Operation(summary = "Obtener todos los usuarios", description = "Obtiene la lista de todos los usuarios activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodos() {
        List<UsuarioResponseDTO> usuarios = usuarioService.obtenerTodos();
        return ResponseEntity.ok(usuarios);
    }
    
    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene un usuario específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        UsuarioResponseDTO usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(usuario);
    }
    
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO')")
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
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody UsuarioUpdateDTO updateDTO) {
        UsuarioResponseDTO usuarioActualizado = usuarioService.actualizar(id, updateDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }
    
    @PatchMapping("/{id}/password")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario tras validar la actual")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id, 
                                             @Valid @RequestBody com.software.territoriodeninos.dto.ChangePasswordRequest request) {
        usuarioService.cambiarPassword(id, request);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Contraseña actualizada exitosamente."));
    }
    
    @PatchMapping("/{id}/inactivar")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Inactivar usuario", description = "Inactiva un usuario del sistema (cambio a INACTIVO, solo ADMINISTRADOR)")
    public ResponseEntity<UsuarioResponseDTO> inactivar(@PathVariable Long id) {
        UsuarioResponseDTO usuarioInactivado = usuarioService.inactivar(id);
        return ResponseEntity.ok(usuarioInactivado);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar usuario permanentemente", description = "Elimina un usuario del sistema de forma definitiva (solo ADMINISTRADOR)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
