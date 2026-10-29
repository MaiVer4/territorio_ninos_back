package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;

public interface VentaCalculoService {

    /**
     * Calcula la cotización previa de una venta aplicando redondeo comercial exacto (HALF_UP a 2 decimales),
     * validación estricta de no negatividad/nulos (Notion: CA-04) y liquidación de impuestos según
     * la parametrización de ConfiguracionParque.
     *
     * @param request Datos de la cotización (ítems detallados o subtotales directos, descuento)
     * @return CotizacionResponseDTO con el desglose de subtotales, base gravable, impuestos y total neto
     * @throws com.software.territoriodeninos.exception.ReglaNegocioException si hay valores nulos, negativos,
     *         descuento excesivo o tarifas inconsistentes/adulteradas
     */
    CotizacionResponseDTO cotizarVenta(CotizacionRequestDTO request);
}
