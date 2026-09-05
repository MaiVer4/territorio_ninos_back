package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import java.util.List;

public interface AtraccionService {
    AtraccionResponseDTO crear(AtraccionRequestDTO request);
    List<AtraccionResponseDTO> obtenerTodas();
    List<AtraccionResponseDTO> obtenerActivas();
    AtraccionResponseDTO obtenerPorId(Long id);
    AtraccionResponseDTO actualizar(Long id, AtraccionRequestDTO request);
    AtraccionResponseDTO cambiarEstado(Long id, String nuevoEstado);
    void eliminar(Long id);
}
