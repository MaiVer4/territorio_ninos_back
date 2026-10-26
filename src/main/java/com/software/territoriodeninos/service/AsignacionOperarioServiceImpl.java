package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.AsignacionOperario;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.AsignacionOperarioRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoAsignacion;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.NombreRol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AsignacionOperarioServiceImpl implements AsignacionOperarioService {

    private final AsignacionOperarioRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtraccionRepository atraccionRepository;

    @Override
    public AsignacionResponseDTO asignar(AsignacionRequestDTO request) {
        Usuario usuario = findAndValidateOperario(request.usuarioId());
        Atraccion atraccion = findAndValidateAtraccionActiva(request.atraccionId());

        if (asignacionRepository.existsByUsuarioIdAndEstado(usuario.getId(), EstadoAsignacion.ACTIVA)) {
            if (Boolean.TRUE.equals(request.forzarReasignacion())) {
                return ejecutarReasignacion(usuario, atraccion, request.observaciones(), true);
            }
            throw new ConflictoException("El operario ya cuenta con una asignación activa en el parque");
        }

        AsignacionOperario asignacion = buildAsignacionActiva(usuario, atraccion, request.observaciones());
        AsignacionOperario guardada = asignacionRepository.save(asignacion);
        
        log.info("Operario {} asignado a atracción {}", usuario.getCorreo(), atraccion.getNombre());
        return mapToDTO(guardada);
    }

    @Override
    public AsignacionResponseDTO reasignar(ReasignacionRequestDTO request) {
        Usuario usuario = findAndValidateOperario(request.usuarioId());
        Atraccion nuevaAtraccion = findAndValidateAtraccionActiva(request.nuevaAtraccionId());

        return ejecutarReasignacion(usuario, nuevaAtraccion, request.motivo(), false);
    }

    private AsignacionResponseDTO ejecutarReasignacion(Usuario usuario, Atraccion nuevaAtraccion, String motivo, boolean forzado) {
        AsignacionOperario asignacionActual = asignacionRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoAsignacion.ACTIVA)
                .orElseThrow(() -> new RecursoNoEncontradoException("El operario no tiene una asignación activa para reasignar"));

        // Cerrar asignación anterior
        asignacionActual.setEstado(EstadoAsignacion.REASIGNADA);
        asignacionActual.setHoraFin(LocalDateTime.now());

        if (motivo != null && !motivo.isBlank()) {
            String obs = asignacionActual.getObservaciones();
            asignacionActual.setObservaciones((obs != null && !obs.isBlank() ? obs + " | " : "") + "Reasignado: " + motivo);
        }

        // Crear nueva asignación activa
        String nuevasObservaciones = "Reasignado desde " + asignacionActual.getAtraccion().getNombre() +
                (motivo != null && !motivo.isBlank() ? " (" + motivo + ")" : "");

        AsignacionOperario nuevaAsignacion = buildAsignacionActiva(usuario, nuevaAtraccion, nuevasObservaciones);
        AsignacionOperario guardada = asignacionRepository.save(nuevaAsignacion);

        if (forzado) {
            log.info("Operario {} reasignado forzosamente de {} a {}", usuario.getCorreo(),
                    asignacionActual.getAtraccion().getNombre(), nuevaAtraccion.getNombre());
        } else {
            log.info("Operario {} reasignado de {} a {}", usuario.getCorreo(),
                    asignacionActual.getAtraccion().getNombre(), nuevaAtraccion.getNombre());
        }

        return mapToDTO(guardada);
    }

    @Override
    public AsignacionResponseDTO finalizar(Long id) {
        AsignacionOperario asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada con ID: " + id));

        if (asignacion.getEstado() != EstadoAsignacion.ACTIVA) {
            throw new ConflictoException("La asignación ya se encuentra " + asignacion.getEstado());
        }

        asignacion.setEstado(EstadoAsignacion.FINALIZADA);
        asignacion.setHoraFin(LocalDateTime.now());
        log.info("Asignación ID {} finalizada para operario {}", id, asignacion.getUsuario().getCorreo());
        return mapToDTO(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> obtenerActivas() {
        return asignacionRepository.findByEstado(EstadoAsignacion.ACTIVA).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AsignacionResponseDTO obtenerActivaPorOperario(Long usuarioId) {
        ensureUsuarioExists(usuarioId);
        AsignacionOperario asignacion = asignacionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoAsignacion.ACTIVA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró una asignación activa para el operario con ID: " + usuarioId));
        return mapToDTO(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> obtenerActivasPorAtraccion(Long atraccionId) {
        if (!atraccionRepository.existsById(atraccionId)) {
            throw new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId);
        }
        return asignacionRepository.findByAtraccionIdAndEstado(atraccionId, EstadoAsignacion.ACTIVA).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> obtenerHistorialPorOperario(Long usuarioId) {
        ensureUsuarioExists(usuarioId);
        return asignacionRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    // --- Métodos Privados Auxiliares ---

    private Usuario findAndValidateOperario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operario no encontrado con ID: " + usuarioId));

        if (usuario.getRol() == null || usuario.getRol().getNombre() != NombreRol.OPERARIO) {
            throw new ConflictoException("El usuario con ID " + usuario.getId() + " no tiene el rol de OPERARIO");
        }

        if (!usuario.estaActivo()) {
            throw new ConflictoException("El operario se encuentra inactivo");
        }

        return usuario;
    }

    private Atraccion findAndValidateAtraccionActiva(Long atraccionId) {
        Atraccion atraccion = atraccionRepository.findById(atraccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));

        if (atraccion.getEstado() != EstadoAtraccion.ACTIVO) {
            throw new ConflictoException("La atracción '" + atraccion.getNombre() + "' no se encuentra activa");
        }
        
        return atraccion;
    }

    private void ensureUsuarioExists(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado con ID: " + usuarioId);
        }
    }

    private AsignacionOperario buildAsignacionActiva(Usuario usuario, Atraccion atraccion, String observaciones) {
        return AsignacionOperario.builder()
                .usuario(usuario)
                .atraccion(atraccion)
                .fechaAsignacion(LocalDate.now())
                .horaInicio(LocalDateTime.now())
                .estado(EstadoAsignacion.ACTIVA)
                .observaciones(observaciones)
                .build();
    }

    private AsignacionResponseDTO mapToDTO(AsignacionOperario asignacion) {
        return new AsignacionResponseDTO(
                asignacion.getId(),
                asignacion.getUsuario().getId(),
                asignacion.getUsuario().getNombre(),
                asignacion.getUsuario().getCorreo(),
                asignacion.getAtraccion().getId(),
                asignacion.getAtraccion().getNombre(),
                asignacion.getFechaAsignacion(),
                asignacion.getHoraInicio(),
                asignacion.getHoraFin(),
                asignacion.getEstado(),
                asignacion.getObservaciones()
        );
    }
}
