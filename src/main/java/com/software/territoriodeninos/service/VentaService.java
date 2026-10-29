package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.VentaProcesarRequestDTO;
import com.software.territoriodeninos.dto.VentaResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface VentaService {

    /**
     * Procesa, valida y liquida de forma transaccional una venta en taquilla,
     * persistiendo el comprobante e incrementando el saldo del turno de caja.
     */
    VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request, Authentication auth);

    /**
     * Consulta los detalles de una venta específica por su ID.
     */
    VentaResponseDTO obtenerVentaPorId(Long id);

    /**
     * Lista las ventas asociadas a un turno de caja específico.
     */
    List<VentaResponseDTO> listarVentasPorTurno(Long turnoId, Authentication auth);
}
