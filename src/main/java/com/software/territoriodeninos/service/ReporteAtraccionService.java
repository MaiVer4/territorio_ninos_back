package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ReporteAtraccionEstadoDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionRequestDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionResponseDTO;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ReporteAtraccionService {
    ReporteAtraccionResponseDTO crear(ReporteAtraccionRequestDTO request, Authentication auth);
    List<ReporteAtraccionResponseDTO> listar(Long atraccionId, TipoReporteAtraccion tipoReporte, EstadoReporteAtraccion estado);
    ReporteAtraccionResponseDTO obtenerPorId(Long id);
    ReporteAtraccionResponseDTO actualizarEstado(Long id, ReporteAtraccionEstadoDTO request, Authentication auth);
}
