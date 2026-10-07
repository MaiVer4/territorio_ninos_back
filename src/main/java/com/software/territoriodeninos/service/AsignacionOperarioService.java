package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;

import java.util.List;

public interface AsignacionOperarioService {
    AsignacionResponseDTO asignar(AsignacionRequestDTO request);
    AsignacionResponseDTO reasignar(ReasignacionRequestDTO request);
    AsignacionResponseDTO finalizar(Long id);
    List<AsignacionResponseDTO> obtenerActivas();
    AsignacionResponseDTO obtenerActivaPorOperario(Long usuarioId);
    List<AsignacionResponseDTO> obtenerActivasPorAtraccion(Long atraccionId);
    List<AsignacionResponseDTO> obtenerHistorialPorOperario(Long usuarioId);
}
