package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;

import java.util.List;

public interface TarifaTiempoService {
    TarifaTiempoResponseDTO crear(TarifaTiempoRequestDTO request);
    List<TarifaTiempoResponseDTO> obtenerPorAtraccion(Long atraccionId);
    TarifaTiempoResponseDTO obtenerPorId(Long id);
    TarifaTiempoResponseDTO actualizar(Long id, TarifaTiempoRequestDTO request);
    void eliminar(Long id);
}
