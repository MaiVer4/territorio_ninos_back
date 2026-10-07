package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;

import java.util.List;

public interface TarifaEventoService {
    TarifaEventoResponseDTO crear(TarifaEventoRequestDTO request);
    List<TarifaEventoResponseDTO> obtenerPorAtraccion(Long atraccionId);
    TarifaEventoResponseDTO obtenerPorId(Long id);
    TarifaEventoResponseDTO actualizar(Long id, TarifaEventoRequestDTO request);
    void eliminar(Long id);
}
