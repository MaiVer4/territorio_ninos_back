package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ChangePasswordRequest;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        NombreRol nombreRolEnum = parseRol(registroDTO.getNombreRol());

        Rol rol = rolRepository.findByNombre(nombreRolEnum)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol " + registroDTO.getNombreRol() + " no encontrado"));

        Usuario usuario = new Usuario(
                registroDTO.getNombre(),
                registroDTO.getCorreo(),
                passwordEncoder.encode(registroDTO.getPassword()),
                rol
        );

        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
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
        Usuario usuario = findUsuarioById(id);

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
        Usuario usuario = findUsuarioById(id);

        verificarPermisosModificacion(usuario, false);

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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        if (StringUtils.hasText(updateDTO.getEstado())) {
            if (!esAdmin) {
                throw new AccessDeniedException("Solo un administrador puede modificar el estado de un usuario");
            }
            try {
                usuario.setEstado(EstadoUsuario.valueOf(updateDTO.getEstado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ConflictoException("Estado inválido: " + updateDTO.getEstado());
            }
        }

        if (StringUtils.hasText(updateDTO.getRol())) {
            if (!esAdmin) {
                throw new AccessDeniedException("Solo un administrador puede asignar o modificar roles");
            }
            NombreRol nombreRolEnum = parseRol(updateDTO.getRol());
            Rol rol = rolRepository.findByNombre(nombreRolEnum)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol " + updateDTO.getRol() + " no encontrado"));
            usuario.setRol(rol);
        }

        return usuarioMapper.toResponseDTO(usuario);
    }

    @Override
    public void cambiarPassword(Long id, ChangePasswordRequest request) {
        Usuario usuario = findUsuarioById(id);

        verificarPermisosModificacion(usuario, true);

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new ConflictoException("La contraseña actual es incorrecta");
        }

        usuario.setPassword(passwordEncoder.encode(request.getNuevaContrasena()));
    }

    @Override
    public UsuarioResponseDTO inactivar(Long id) {
        Usuario usuario = findUsuarioById(id);
        usuario.inactivar();
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO activar(Long id) {
        Usuario usuario = findUsuarioById(id);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        log.info("Usuario {} reactivado", usuario.getCorreo());
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> obtenerPorRol(String rol) {
        NombreRol nombreRolEnum = parseRol(rol);
        return usuarioRepository.findByRolNombre(nombreRolEnum).stream()
                .map(usuarioMapper::toResponseDTO)
                .toList();
    }

    @Override
    public void eliminar(Long id) {
        Usuario usuario = findUsuarioById(id);
        usuarioRepository.delete(usuario);
    }

    // --- Métodos Privados Auxiliares ---

    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con ID " + id + " no encontrado"));
    }

    private NombreRol parseRol(String rol) {
        try {
            return NombreRol.valueOf(rol.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ConflictoException("Rol inválido: " + rol);
        }
    }

    private void verificarPermisosModificacion(Usuario usuarioTarget, boolean soloMismoUsuario) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("No autenticado");
        }
        String correoAutenticado = auth.getName();
        if (usuarioTarget.getCorreo().equalsIgnoreCase(correoAutenticado)) {
            return;
        }
        if (soloMismoUsuario) {
            throw new AccessDeniedException("Solo puedes modificar tu propia contraseña");
        }
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        if (!esAdmin) {
            throw new AccessDeniedException("Privilegios insuficientes para modificar a otro usuario");
        }
    }
}
