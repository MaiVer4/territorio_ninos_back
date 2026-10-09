package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ChangePasswordRequest;
import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;
import com.software.territoriodeninos.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController implements UsuarioApi {
    
    private final UsuarioService usuarioService;
    
    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRegistroDTO registroDTO) {
        UsuarioResponseDTO usuarioCreado = usuarioService.registrar(registroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCreado);
    }
    
    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodos() {
        List<UsuarioResponseDTO> usuarios = usuarioService.obtenerTodos();
        return ResponseEntity.ok(usuarios);
    }
    
    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        UsuarioResponseDTO usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(usuario);
    }
    
    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO')")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody UsuarioUpdateDTO updateDTO) {
        UsuarioResponseDTO usuarioActualizado = usuarioService.actualizar(id, updateDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }
    
    @Override
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO')")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id, 
                                             @Valid @RequestBody ChangePasswordRequest request) {
        usuarioService.cambiarPassword(id, request);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente."));
    }
    
    @Override
    @PatchMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> inactivar(@PathVariable Long id) {
        UsuarioResponseDTO usuarioInactivado = usuarioService.inactivar(id);
        return ResponseEntity.ok(usuarioInactivado);
    }

    @Override
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> activar(@PathVariable Long id) {
        UsuarioResponseDTO usuarioActivado = usuarioService.activar(id);
        return ResponseEntity.ok(usuarioActivado);
    }

    @Override
    @GetMapping("/rol/{rol}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TAQUILLERO')")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerPorRol(@PathVariable String rol) {
        List<UsuarioResponseDTO> usuarios = usuarioService.obtenerPorRol(rol);
        return ResponseEntity.ok(usuarios);
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
