package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;
import com.software.territoriodeninos.dto.ItemCotizacionDTO;
import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.entity.ConfiguracionParque;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.ConfiguracionParqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VentaCalculoServiceImpl implements VentaCalculoService {

    public static final int ESCALA_MONETARIA = 2;
    public static final RoundingMode MODO_REDONDEO = RoundingMode.HALF_UP;
    private static final BigDecimal CIEN = new BigDecimal("100.00");

    private final ConfiguracionParqueRepository configuracionParqueRepository;
    private final AforoValidatorService aforoValidatorService;

    @Override
    public CotizacionResponseDTO cotizarVenta(CotizacionRequestDTO request) {
        if (request == null) {
            throw new ReglaNegocioException("La solicitud de cotización no puede ser nula");
        }

        boolean hasItems = request.items() != null && !request.items().isEmpty();
        boolean hasSubtotales = request.subtotales() != null && !request.subtotales().isEmpty();

        if (!hasItems && !hasSubtotales) {
            throw new ReglaNegocioException("Debe proporcionar al menos una lista de ítems o subtotales para cotizar");
        }

        List<BigDecimal> subtotalesCalculados = new ArrayList<>();
        List<ItemCotizacionDTO> itemsCalculados = hasItems ? new ArrayList<>() : null;

        if (hasItems) {
            List<ItemVentaValidacionDTO> itemsParaValidacionAforo = new ArrayList<>();

            for (ItemCotizacionDTO item : request.items()) {
                if (item == null) {
                    throw new ReglaNegocioException("Los ítems de cotización no pueden ser nulos");
                }
                if (item.cantidad() == null || item.cantidad() <= 0) {
                    throw new ReglaNegocioException("La cantidad de cada ítem debe ser mayor o igual a 1");
                }
                if (item.precioUnitario() == null || item.precioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ReglaNegocioException("El precio unitario no puede ser nulo ni negativo");
                }
                if (item.subtotal() != null && item.subtotal().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ReglaNegocioException("El subtotal del ítem no puede ser negativo");
                }

                BigDecimal precioUnitarioRedondeado = item.precioUnitario().setScale(ESCALA_MONETARIA, MODO_REDONDEO);
                BigDecimal subtotalLinea = precioUnitarioRedondeado
                        .multiply(BigDecimal.valueOf(item.cantidad()))
                        .setScale(ESCALA_MONETARIA, MODO_REDONDEO);

                if (item.subtotal() != null) {
                    BigDecimal subtotalEnviado = item.subtotal().setScale(ESCALA_MONETARIA, MODO_REDONDEO);
                    if (subtotalEnviado.compareTo(subtotalLinea) != 0) {
                        throw new ReglaNegocioException("Inconsistencia en el subtotal del ítem '" +
                                (item.descripcion() != null ? item.descripcion() : "sin descripción") +
                                "': enviado ($" + subtotalEnviado + ") no coincide con el cálculo ($" + subtotalLinea + ")");
                    }
                }

                if (item.tarifaTiempoId() != null || item.tarifaEventoId() != null) {
                    if (item.atraccionId() == null) {
                        throw new ReglaNegocioException("Debe especificar el ID de la atracción para la tarifa seleccionada");
                    }
                    itemsParaValidacionAforo.add(new ItemVentaValidacionDTO(
                            item.atraccionId(),
                            item.tarifaTiempoId(),
                            item.tarifaEventoId(),
                            item.cantidad(),
                            item.precioUnitario(),
                            item.subtotal(),
                            null,
                            null
                    ));
                }

                subtotalesCalculados.add(subtotalLinea);
                itemsCalculados.add(new ItemCotizacionDTO(
                        item.atraccionId(),
                        item.tarifaTiempoId(),
                        item.tarifaEventoId(),
                        item.descripcion(),
                        item.cantidad(),
                        precioUnitarioRedondeado,
                        subtotalLinea
                ));
            }

            if (!itemsParaValidacionAforo.isEmpty()) {
                aforoValidatorService.validarItemsVenta(itemsParaValidacionAforo);
            }
        } else {
            for (BigDecimal subtotal : request.subtotales()) {
                if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ReglaNegocioException("Los valores de subtotales no pueden ser nulos ni negativos");
                }
                subtotalesCalculados.add(subtotal.setScale(ESCALA_MONETARIA, MODO_REDONDEO));
            }
        }

        BigDecimal subtotalGeneral = subtotalesCalculados.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA_MONETARIA, MODO_REDONDEO);

        BigDecimal descuento = request.descuento() != null ? request.descuento() : BigDecimal.ZERO;
        descuento = descuento.setScale(ESCALA_MONETARIA, MODO_REDONDEO);

        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El descuento no puede ser negativo");
        }
        if (descuento.compareTo(subtotalGeneral) > 0) {
            throw new ReglaNegocioException("El descuento no puede ser superior al subtotal de la venta");
        }

        BigDecimal baseGravable = subtotalGeneral.subtract(descuento)
                .setScale(ESCALA_MONETARIA, MODO_REDONDEO);

        ConfiguracionParque config = configuracionParqueRepository.findFirstByOrderByIdAsc().orElse(null);
        boolean aplicaImpuesto = config != null && Boolean.TRUE.equals(config.getAplicaImpuesto());
        BigDecimal porcentajeImpuesto = config != null && config.getPorcentajeImpuesto() != null
                ? config.getPorcentajeImpuesto().setScale(ESCALA_MONETARIA, MODO_REDONDEO)
                : BigDecimal.ZERO.setScale(ESCALA_MONETARIA, MODO_REDONDEO);

        if (porcentajeImpuesto.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El porcentaje de impuesto configurado no puede ser negativo");
        }

        BigDecimal impuestos = BigDecimal.ZERO.setScale(ESCALA_MONETARIA, MODO_REDONDEO);
        if (aplicaImpuesto && porcentajeImpuesto.compareTo(BigDecimal.ZERO) > 0) {
            impuestos = baseGravable.multiply(porcentajeImpuesto)
                    .divide(CIEN, ESCALA_MONETARIA, MODO_REDONDEO);
        }

        BigDecimal totalNeto = baseGravable.add(impuestos)
                .setScale(ESCALA_MONETARIA, MODO_REDONDEO);

        log.debug("Cotización calculada: subtotal={}, descuento={}, baseGravable={}, impuesto%={}, impuestos={}, total={}",
                subtotalGeneral, descuento, baseGravable, porcentajeImpuesto, impuestos, totalNeto);

        return new CotizacionResponseDTO(
                subtotalesCalculados,
                itemsCalculados,
                subtotalGeneral,
                descuento,
                baseGravable,
                porcentajeImpuesto,
                impuestos,
                totalNeto
        );
    }
}
