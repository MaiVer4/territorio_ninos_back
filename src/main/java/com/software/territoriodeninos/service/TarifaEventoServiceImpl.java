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
        Atraccion atraccion = findAtraccion(request.atraccionId());
        validarParametros(request.precio(), request.intentos());

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
        return mapToDTO(findTarifa(id));
    }

    @Override
    public TarifaEventoResponseDTO actualizar(Long id, TarifaEventoRequestDTO request) {
        TarifaEvento tarifa = findTarifa(id);

        if (request.precio() != null) {
            validarPrecio(request.precio());
            tarifa.setPrecio(request.precio());
        }

        if (request.intentos() != null) {
            validarIntentos(request.intentos());
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
        tarifaEventoRepository.delete(findTarifa(id));
        log.info("Tarifa de evento con ID {} eliminada", id);
    }

    private Atraccion findAtraccion(Long id) {
        return atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));
    }

    private TarifaEvento findTarifa(Long id) {
        return tarifaEventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por evento no encontrada con ID: " + id));
    }

    private void validarParametros(BigDecimal precio, Integer intentos) {
        validarPrecio(precio);
        validarIntentos(intentos);
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
    }

    private void validarIntentos(Integer intentos) {
        if (intentos == null || intentos < 1) {
            throw new IllegalArgumentException("El número de intentos debe ser al menos 1");
        }
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
