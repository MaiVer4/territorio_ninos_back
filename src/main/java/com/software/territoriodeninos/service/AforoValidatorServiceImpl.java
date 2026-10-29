package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.util.EstadoAtraccion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AforoValidatorServiceImpl implements AforoValidatorService {

    private final AtraccionRepository atraccionRepository;
    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final TarifaEventoRepository tarifaEventoRepository;
    private final com.software.territoriodeninos.repository.DetalleVentaRepository detalleVentaRepository;

    @Override
    public int validarCapacidadAtraccion(Long atraccionId, int cantidadTickets) {
        if (atraccionId == null) {
            throw new ReglaNegocioException("El ID de la atracción es obligatorio para validar aforo");
        }
        if (cantidadTickets <= 0) {
            throw new ReglaNegocioException("La cantidad de tickets a validar debe ser mayor a 0");
        }

        Atraccion atraccion = atraccionRepository.findById(atraccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));

        if (!EstadoAtraccion.ACTIVO.equals(atraccion.getEstado())) {
            throw new ReglaNegocioException("La atracción '" + atraccion.getNombre() +
                    "' no se encuentra activa (estado: " + atraccion.getEstado() + ") y no admite ventas");
        }

        int capacidadMaxima = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        int duracionVentanaMinutos = (atraccion.getDuracionMinutosBase() != null && atraccion.getDuracionMinutosBase() > 0)
                ? atraccion.getDuracionMinutosBase()
                : 15;
        java.time.LocalDateTime desde = java.time.LocalDateTime.now().minusMinutes(duracionVentanaMinutos);
        int ocupacionActual = 0;
        if (detalleVentaRepository != null) {
            Integer count = detalleVentaRepository.countBoletasActivasPorAtraccion(atraccionId, desde);
            ocupacionActual = count != null ? count : 0;
        }
        int aforoDisponible = Math.max(0, capacidadMaxima - ocupacionActual);

        if (cantidadTickets > aforoDisponible) {
            throw new ReglaNegocioException("Aforo máximo excedido para la atracción '" + atraccion.getNombre() +
                    "'. Se solicitaron " + cantidadTickets + " ticket(s) pero solo hay " + aforoDisponible +
                    " cupo(s) disponible(s) (Capacidad máxima: " + capacidadMaxima + ", Ocupación actual: " + ocupacionActual + ")");
        }

        return aforoDisponible - cantidadTickets;
    }

    @Override
    public BigDecimal validarConsistenciaTarifa(ItemVentaValidacionDTO item) {
        if (item == null) {
            throw new ReglaNegocioException("El ítem de venta no puede ser nulo");
        }
        if (item.atraccionId() == null) {
            throw new ReglaNegocioException("El ID de la atracción es obligatorio");
        }
        if (item.precioUnitario() == null) {
            throw new ReglaNegocioException("El precio unitario enviado es obligatorio para validar consistencia");
        }

        boolean tieneTiempo = item.tarifaTiempoId() != null;
        boolean tieneEvento = item.tarifaEventoId() != null;

        if (!tieneTiempo && !tieneEvento) {
            throw new ReglaNegocioException("Debe especificar una tarifa por tiempo o por evento para la atracción ID: " + item.atraccionId());
        }
        if (tieneTiempo && tieneEvento) {
            throw new ReglaNegocioException("Un ítem de venta no puede tener simultáneamente tarifa por tiempo y por evento");
        }

        int cantidad = (item.cantidad() != null && item.cantidad() > 0) ? item.cantidad() : 1;
        BigDecimal precioOficial;

        if (tieneTiempo) {
            TarifaTiempo tarifa = tarifaTiempoRepository.findById(item.tarifaTiempoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por tiempo no encontrada con ID: " + item.tarifaTiempoId()));

            if (!Boolean.TRUE.equals(tarifa.getActivo())) {
                throw new ReglaNegocioException("La tarifa por tiempo seleccionada (ID " + item.tarifaTiempoId() + ") se encuentra inactiva");
            }
            if (!tarifa.getAtraccion().getId().equals(item.atraccionId())) {
                throw new ReglaNegocioException("La tarifa por tiempo ID " + item.tarifaTiempoId() +
                        " no pertenece a la atracción ID " + item.atraccionId() +
                        " ('" + tarifa.getAtraccion().getNombre() + "')");
            }
            if (item.duracionMinutos() != null && !item.duracionMinutos().equals(tarifa.getDuracionMinutos())) {
                throw new ReglaNegocioException("La duración enviada (" + item.duracionMinutos() +
                        " min) no coincide con la tarifa oficial registrada (" + tarifa.getDuracionMinutos() + " min)");
            }

            precioOficial = tarifa.getPrecio();
            validarPrecioYSubtotal(item, precioOficial, cantidad, "tiempo", item.tarifaTiempoId());
        } else {
            TarifaEvento tarifa = tarifaEventoRepository.findById(item.tarifaEventoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa por evento no encontrada con ID: " + item.tarifaEventoId()));

            if (!Boolean.TRUE.equals(tarifa.getActivo())) {
                throw new ReglaNegocioException("La tarifa por evento seleccionada (ID " + item.tarifaEventoId() + ") se encuentra inactiva");
            }
            if (!tarifa.getAtraccion().getId().equals(item.atraccionId())) {
                throw new ReglaNegocioException("La tarifa por evento ID " + item.tarifaEventoId() +
                        " no pertenece a la atracción ID " + item.atraccionId() +
                        " ('" + tarifa.getAtraccion().getNombre() + "')");
            }
            if (item.intentos() != null && !item.intentos().equals(tarifa.getIntentos())) {
                throw new ReglaNegocioException("El número de intentos enviado (" + item.intentos() +
                        ") no coincide con la tarifa oficial registrada (" + tarifa.getIntentos() + ")");
            }

            precioOficial = tarifa.getPrecio();
            validarPrecioYSubtotal(item, precioOficial, cantidad, "evento", item.tarifaEventoId());
        }

        return precioOficial;
    }

    @Override
    public ResultadoValidacionVentaDTO validarItemsVenta(List<ItemVentaValidacionDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new ReglaNegocioException("La lista de ítems a validar no puede estar vacía");
        }

        // 1. Agrupar la suma total de tickets solicitados por cada atracción
        Map<Long, Integer> ticketsPorAtraccion = new LinkedHashMap<>();
        for (ItemVentaValidacionDTO item : items) {
            if (item.atraccionId() == null) {
                throw new ReglaNegocioException("Todos los ítems deben tener un atraccionId válido");
            }
            int cant = (item.cantidad() != null && item.cantidad() > 0) ? item.cantidad() : 1;
            ticketsPorAtraccion.merge(item.atraccionId(), cant, Integer::sum);
        }

        // 2. Validar que la suma total de tickets no sobrepase la capacidad máxima de cada atracción
        Map<Long, Integer> aforoRestantePorAtraccion = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : ticketsPorAtraccion.entrySet()) {
            int restante = validarCapacidadAtraccion(entry.getKey(), entry.getValue());
            aforoRestantePorAtraccion.put(entry.getKey(), restante);
        }

        // 3. Validar consistencia de tarifas y acumular total oficial
        BigDecimal totalOficial = BigDecimal.ZERO;
        int totalTickets = 0;

        for (ItemVentaValidacionDTO item : items) {
            BigDecimal precioOficial = validarConsistenciaTarifa(item);
            int cant = (item.cantidad() != null && item.cantidad() > 0) ? item.cantidad() : 1;
            totalTickets += cant;
            totalOficial = totalOficial.add(precioOficial.multiply(BigDecimal.valueOf(cant)));
        }

        return new ResultadoValidacionVentaDTO(true, totalOficial, totalTickets, aforoRestantePorAtraccion);
    }

    private void validarPrecioYSubtotal(ItemVentaValidacionDTO item, BigDecimal precioOficial, int cantidad, String tipoTarifa, Long tarifaId) {
        if (item.precioUnitario().compareTo(precioOficial) != 0) {
            log.warn("Alerta de seguridad: Intento de adulteración de precio en tarifa {} ID {}. Precio enviado: {}, Precio oficial BD: {}",
                    tipoTarifa, tarifaId, item.precioUnitario(), precioOficial);
            throw new ReglaNegocioException("Inconsistencia en el precio de la tarifa de " + tipoTarifa + " (ID " + tarifaId +
                    "): el precio enviado ($" + item.precioUnitario() + ") no coincide con el registrado en el sistema ($" + precioOficial + ")");
        }

        if (item.subtotal() != null) {
            BigDecimal subtotalCalculado = precioOficial.multiply(BigDecimal.valueOf(cantidad));
            if (item.subtotal().compareTo(subtotalCalculado) != 0) {
                log.warn("Alerta de seguridad: Intento de adulteración de subtotal en tarifa {} ID {}. Subtotal enviado: {}, Subtotal esperado: {}",
                        tipoTarifa, tarifaId, item.subtotal(), subtotalCalculado);
                throw new ReglaNegocioException("Inconsistencia en el subtotal de la línea para tarifa ID " + tarifaId +
                        ": subtotal enviado ($" + item.subtotal() + ") no coincide con el cálculo oficial ($" + subtotalCalculado + ")");
            }
        }
    }
}
