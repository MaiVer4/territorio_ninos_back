package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.mapper.UsuarioMapper;
import com.software.territoriodeninos.repository.RolRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoUsuario;
import com.software.territoriodeninos.util.NombreRol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    
    @Override
    public UsuarioResponseDTO registrar(UsuarioRegistroDTO registroDTO) {
        if (usuarioRepository.existsByCorreo(registroDTO.getCorreo())) {
            throw new ConflictoException("El correo " + registroDTO.getCorreo() + " ya está registrado");
        }
        
        NombreRol nombreRolEnum;
        try {
            nombreRolEnum = NombreRol.valueOf(registroDTO.getNombreRol().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ConflictoException("Rol inválido: " + registroDTO.getNombreRol());
        }
        
        Rol rol = rolRepository.findByNombre(nombreRolEnum)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol " + registroDTO.getNombreRol() + " no encontrado"));
        
        Usuario usuario = new Usuario(
                registroDTO.getNombre(),
                registroDTO.getCorreo(),
                passwordEncoder.encode(registroDTO.getPassword()),
                rol
        );
        
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return usuarioMapper.toResponseDTO(usuarioGuardado);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> obtenerTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponseDTO)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con ID " + id + " no encontrado"));
        
        if (!usuario.estaActivo()) {
            throw new RecursoNoEncontradoException("Usuario inactivo con ID " + id);
        }
        
        return usuarioMapper.toResponseDTO(usuario);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoAndEstado(correo, EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con correo " + correo + " no encontrado o está inactivo"));
        
        return usuarioMapper.toResponseDTO(usuario);
    }
    
    @Override
    public UsuarioResponseDTO actualizar(Long id, UsuarioUpdateDTO updateDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con ID " + id + " no encontrado"));
        
        verificarBOLA(usuario, false);
        
        if (StringUtils.hasText(updateDTO.getNombre())) {
            usuario.setNombre(updateDTO.getNombre());
        }
        
        if (StringUtils.hasText(updateDTO.getCorreo())) {
            if (!usuario.getCorreo().equalsIgnoreCase(updateDTO.getCorreo()) &&
                    usuarioRepository.existsByCorreo(updateDTO.getCorreo())) {
                throw new ConflictoException("El correo " + updateDTO.getCorreo() + " ya está en uso");
            }
            usuario.setCorreo(updateDTO.getCorreo());
        }
        
        if (StringUtils.hasText(updateDTO.getEstado())) {
            try {
                usuario.setEstado(EstadoUsuario.valueOf(updateDTO.getEstado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ConflictoException("Estado inválido: " + updateDTO.getEstado());
            }
        }
        
        return usuarioMapper.toResponseDTO(usuario);
    }
    
    @Override
    public void cambiarPassword(Long id, com.software.territoriodeninos.dto.ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con ID " + id + " no encontrado"));
                
        verificarBOLA(usuario, true);
        
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new com.software.territoriodeninos.exception.ConflictoException("La contraseña actual es incorrecta");
        }
        
        usuario.setPassword(passwordEncoder.encode(request.getNuevaContrasena()));
    }
    
    @Override
    public UsuarioResponseDTO inactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con ID " + id + " no encontrado"));
        
        usuario.inactivar();
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Override
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con ID " + id + " no encontrado"));
        usuarioRepository.delete(usuario);
    }

    private void verificarBOLA(Usuario usuarioTarget, boolean soloMismoUsuario) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.access.AccessDeniedException("No autenticado");
        }
        String correoAutenticado = auth.getName();
        if (usuarioTarget.getCorreo().equalsIgnoreCase(correoAutenticado)) {
            return;
        }
        if (soloMismoUsuario) {
            throw new org.springframework.security.access.AccessDeniedException("Solo puedes modificar tu propia contraseña");
        }
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        if (!esAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Privilegios insuficientes para modificar a otro usuario");
        }
    }
}
