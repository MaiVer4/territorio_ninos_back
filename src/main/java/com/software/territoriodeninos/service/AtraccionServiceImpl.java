package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.util.EstadoAtraccion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AtraccionServiceImpl implements AtraccionService {

    private final AtraccionRepository atraccionRepository;

    @Override
    public AtraccionResponseDTO crear(AtraccionRequestDTO request) {
        if (atraccionRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictoException("Ya existe una atracción con el nombre: " + request.nombre());
        }

        Atraccion.AtraccionBuilder builder = Atraccion.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tarifaMinuto(request.tarifaMinuto());

        if (request.tipo() != null) {
            builder.tipo(request.tipo());
        }
        if (request.capacidadMaxima() != null) {
            builder.capacidadMaxima(request.capacidadMaxima());
        }
        if (request.duracionMinutosBase() != null) {
            builder.duracionMinutosBase(request.duracionMinutosBase());
        }
        if (request.toleranciaMinutos() != null) {
            builder.toleranciaMinutos(request.toleranciaMinutos());
        }
        if (request.caidasLimite() != null) {
            builder.caidasLimite(request.caidasLimite());
        }

        Atraccion guardada = atraccionRepository.save(builder.build());
        log.info("Atracción creada: {}", guardada.getNombre());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtraccionResponseDTO> obtenerTodas() {
        return atraccionRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtraccionResponseDTO> obtenerActivas() {
        return atraccionRepository.findByEstado(EstadoAtraccion.ACTIVO).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AtraccionResponseDTO obtenerPorId(Long id) {
        Atraccion atraccion = atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));
        return mapToDTO(atraccion);
    }

    @Override
    public AtraccionResponseDTO actualizar(Long id, AtraccionRequestDTO request) {
        Atraccion atraccion = atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));

        if (!atraccion.getNombre().equalsIgnoreCase(request.nombre()) &&
                atraccionRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictoException("Ya existe otra atracción con el nombre: " + request.nombre());
        }

        atraccion.setNombre(request.nombre());
        atraccion.setDescripcion(request.descripcion());
        atraccion.setTarifaMinuto(request.tarifaMinuto());

        if (request.tipo() != null) {
            atraccion.setTipo(request.tipo());
        }
        if (request.capacidadMaxima() != null) {
            atraccion.setCapacidadMaxima(request.capacidadMaxima());
        }
        if (request.duracionMinutosBase() != null) {
            atraccion.setDuracionMinutosBase(request.duracionMinutosBase());
        }
        if (request.toleranciaMinutos() != null) {
            atraccion.setToleranciaMinutos(request.toleranciaMinutos());
        }
        if (request.caidasLimite() != null) {
            atraccion.setCaidasLimite(request.caidasLimite());
        }

        log.info("Atracción actualizada: {}", atraccion.getNombre());
        return mapToDTO(atraccion);
    }

    @Override
    public AtraccionResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        Atraccion atraccion = atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));

        try {
            EstadoAtraccion estado = EstadoAtraccion.valueOf(nuevoEstado.toUpperCase());
            atraccion.setEstado(estado);
            log.info("Estado de atracción {} cambiado a {}", atraccion.getNombre(), estado);
        } catch (IllegalArgumentException e) {
            throw new ConflictoException("Estado de atracción inválido: " + nuevoEstado);
        }

        return mapToDTO(atraccion);
    }

    @Override
    public void eliminar(Long id) {
        Atraccion atraccion = atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));
        
        atraccion.setEstado(EstadoAtraccion.INACTIVO);
        log.info("Atracción {} marcada como inactiva", atraccion.getNombre());
    }

    private AtraccionResponseDTO mapToDTO(Atraccion atraccion) {
        return new AtraccionResponseDTO(
                atraccion.getId(),
                atraccion.getNombre(),
                atraccion.getDescripcion(),
                atraccion.getTarifaMinuto(),
                atraccion.getEstado() != null ? atraccion.getEstado().name() : "ACTIVO",
                atraccion.getFechaCreacion(),
                atraccion.getTipo() != null ? atraccion.getTipo().name() : "TEMPORIZADA",
                atraccion.getCapacidadMaxima(),
                atraccion.getDuracionMinutosBase(),
                atraccion.getToleranciaMinutos(),
                atraccion.getCaidasLimite()
        );
    }
}
