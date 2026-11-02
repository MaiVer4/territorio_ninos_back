package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.VentaProcesarRequestDTO;
import com.software.territoriodeninos.dto.VentaResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface VentaService {

    /**
     * Procesa y registra de forma transaccional una venta en taquilla:
     * - Asigna estado PAGADO, fecha y hora de emisión exacta.
     * - Valida cliente, menores, aforo y tarifas oficiales.
     * - Vincula al turno de caja activo actualizando el balance del sistema.
     * - Congela los datos de la transacción para impedir cualquier edición posterior.
     * - Si falla cualquier ítem o regla de negocio, realiza rollback integral.
     *
     * @param request Datos de la venta a procesar
     * @param auth Datos del usuario autenticado (cajero en sesión)
     * @return VentaResponseDTO con el ID generado y desglose financiero
     */
    VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request, Authentication auth);

    default VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request) {
        return procesarVenta(request, null);
    }

    VentaResponseDTO obtenerVentaPorId(Long id);

    List<VentaResponseDTO> obtenerVentasPorTurno(Long turnoCajaId);

    default List<VentaResponseDTO> listarVentasPorTurno(Long turnoId, Authentication auth) {
        return obtenerVentasPorTurno(turnoId);
    }
}
