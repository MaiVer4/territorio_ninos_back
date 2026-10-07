package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TarifaEventoServiceImpl implements TarifaEventoService {

    private final TarifaEventoRepository tarifaEventoRepository;
    private final AtraccionRepository atraccionRepository;

    @Override
    public TarifaEventoResponseDTO crear(TarifaEventoRequestDTO request) {
        Atraccion atraccion = atraccionRepository.findById(request.atraccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + request.atraccionId()));

        if (request.precio() == null || request.precio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
        if (request.intentos() == null || request.intentos() < 1) {
            throw new IllegalArgumentException("El número de intentos debe ser al menos 1");
        }

        if (tarifaEventoRepository.existsByAtraccionIdAndIntentos(request.atraccionId(), request.intentos())) {
            throw new ConflictoException("Ya existe una tarifa de evento para la atracción '" + atraccion.getNombre() +
                    "' con " + request.intentos() + " intento(s)");
        }

        TarifaEvento tarifa = TarifaEvento.builder()
                .atraccion(atraccion)
                .intentos(request.intentos())
                .precio(request.precio())
                .activo(request.activo() != null ? request.activo() : true)
                .build();

        TarifaEvento guardada = tarifaEventoRepository.save(tarifa);
        log.info("Tarifa de evento creada para atracción {} con {} intentos: ${}",
                atraccion.getNombre(), guardada.getIntentos(), guardada.getPrecio());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaEventoResponseDTO> obtenerPorAtraccion(Long atraccionId) {
        if (!atraccionRepository.existsById(atraccionId)) {
            throw new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId);
        }
        return tarifaEventoRepository.findByAtraccionId(atraccionId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TarifaEventoResponseDTO obtenerPorId(Long id) {
        TarifaEvento tarifa = tarifaEventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por evento no encontrada con ID: " + id));
        return mapToDTO(tarifa);
    }

    @Override
    public TarifaEventoResponseDTO actualizar(Long id, TarifaEventoRequestDTO request) {
        TarifaEvento tarifa = tarifaEventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por evento no encontrada con ID: " + id));

        if (request.precio() != null) {
            if (request.precio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio debe ser mayor a 0");
            }
            tarifa.setPrecio(request.precio());
        }

        if (request.intentos() != null) {
            if (request.intentos() < 1) {
                throw new IllegalArgumentException("El número de intentos debe ser al menos 1");
            }
            if (!tarifa.getIntentos().equals(request.intentos()) &&
                    tarifaEventoRepository.existsByAtraccionIdAndIntentos(tarifa.getAtraccion().getId(), request.intentos())) {
                throw new ConflictoException("Ya existe una tarifa para la atracción con " + request.intentos() + " intento(s)");
            }
            tarifa.setIntentos(request.intentos());
        }

        if (request.activo() != null) {
            tarifa.setActivo(request.activo());
        }

        log.info("Tarifa de evento con ID {} actualizada", id);
        return mapToDTO(tarifa);
    }

    @Override
    public void eliminar(Long id) {
        TarifaEvento tarifa = tarifaEventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por evento no encontrada con ID: " + id));
        tarifaEventoRepository.delete(tarifa);
        log.info("Tarifa de evento con ID {} eliminada", id);
    }

    private TarifaEventoResponseDTO mapToDTO(TarifaEvento tarifa) {
        return new TarifaEventoResponseDTO(
                tarifa.getId(),
                tarifa.getAtraccion().getId(),
                tarifa.getAtraccion().getNombre(),
                tarifa.getIntentos(),
                tarifa.getPrecio(),
                tarifa.getActivo(),
                tarifa.getFechaCreacion()
        );
    }
}
