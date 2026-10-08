package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
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
public class TarifaTiempoServiceImpl implements TarifaTiempoService {

    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final AtraccionRepository atraccionRepository;

    @Override
    public TarifaTiempoResponseDTO crear(TarifaTiempoRequestDTO request) {
        Atraccion atraccion = findAtraccion(request.atraccionId());
        validarParametros(request.precio(), request.duracionMinutos());

        if (tarifaTiempoRepository.existsByAtraccionIdAndDuracionMinutos(request.atraccionId(), request.duracionMinutos())) {
            throw new ConflictoException("Ya existe una tarifa para la atracción '" + atraccion.getNombre() +
                    "' con duración de " + request.duracionMinutos() + " minutos");
        }

        TarifaTiempo tarifa = TarifaTiempo.builder()
                .atraccion(atraccion)
                .duracionMinutos(request.duracionMinutos())
                .precio(request.precio())
                .activo(request.activo() != null ? request.activo() : true)
                .build();

        TarifaTiempo guardada = tarifaTiempoRepository.save(tarifa);
        log.info("Tarifa de tiempo creada para atracción {} de {} min: ${}",
                atraccion.getNombre(), guardada.getDuracionMinutos(), guardada.getPrecio());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaTiempoResponseDTO> obtenerPorAtraccion(Long atraccionId) {
        if (!atraccionRepository.existsById(atraccionId)) {
            throw new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId);
        }
        return tarifaTiempoRepository.findByAtraccionId(atraccionId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TarifaTiempoResponseDTO obtenerPorId(Long id) {
        return mapToDTO(findTarifa(id));
    }

    @Override
    public TarifaTiempoResponseDTO actualizar(Long id, TarifaTiempoRequestDTO request) {
        TarifaTiempo tarifa = findTarifa(id);

        if (request.precio() != null) {
            validarPrecio(request.precio());
            tarifa.setPrecio(request.precio());
        }

        if (request.duracionMinutos() != null) {
            validarDuracion(request.duracionMinutos());
            if (!tarifa.getDuracionMinutos().equals(request.duracionMinutos()) &&
                    tarifaTiempoRepository.existsByAtraccionIdAndDuracionMinutos(tarifa.getAtraccion().getId(), request.duracionMinutos())) {
                throw new ConflictoException("Ya existe una tarifa para la atracción con duración de " + request.duracionMinutos() + " minutos");
            }
            tarifa.setDuracionMinutos(request.duracionMinutos());
        }

        if (request.activo() != null) {
            tarifa.setActivo(request.activo());
        }

        log.info("Tarifa de tiempo con ID {} actualizada", id);
        return mapToDTO(tarifa);
    }

    @Override
    public void eliminar(Long id) {
        tarifaTiempoRepository.delete(findTarifa(id));
        log.info("Tarifa de tiempo con ID {} eliminada", id);
    }

    private Atraccion findAtraccion(Long id) {
        return atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));
    }

    private TarifaTiempo findTarifa(Long id) {
        return tarifaTiempoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por tiempo no encontrada con ID: " + id));
    }

    private void validarParametros(BigDecimal precio, Integer duracionMinutos) {
        validarPrecio(precio);
        validarDuracion(duracionMinutos);
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
    }

    private void validarDuracion(Integer duracionMinutos) {
        if (duracionMinutos == null || duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0 minutos");
        }
    }

    private TarifaTiempoResponseDTO mapToDTO(TarifaTiempo tarifa) {
        return new TarifaTiempoResponseDTO(
                tarifa.getId(),
                tarifa.getAtraccion().getId(),
                tarifa.getAtraccion().getNombre(),
                tarifa.getDuracionMinutos(),
                tarifa.getPrecio(),
                tarifa.getActivo(),
                tarifa.getFechaCreacion()
        );
    }
}
