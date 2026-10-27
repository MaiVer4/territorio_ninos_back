package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;

import java.math.BigDecimal;
import java.util.List;

public interface AforoValidatorService {

    /**
     * Comprueba que la cantidad de tickets solicitados para una atracción no supere su capacidad disponible.
     *
     * @param atraccionId ID de la atracción
     * @param cantidadTickets Cantidad de tickets a vender
     * @return Cupos restantes proyectados tras la venta
     * @throws com.software.territoriodeninos.exception.ReglaNegocioException si se sobrepasa la capacidad o la atracción no está activa
     * @throws com.software.territoriodeninos.exception.RecursoNoEncontradoException si la atracción no existe
     */
    int validarCapacidadAtraccion(Long atraccionId, int cantidadTickets);

    /**
     * Valida que la tarifa enviada desde el cliente exista, esté activa, corresponda a la atracción
     * y que el precio no haya sido adulterado.
     *
     * @param item Ítem de venta con datos de tarifa y precio
     * @return El precio oficial autoritativo de la tarifa en base de datos
     * @throws com.software.territoriodeninos.exception.ReglaNegocioException si la tarifa es inconsistente o adulterada
     * @throws com.software.territoriodeninos.exception.RecursoNoEncontradoException si la tarifa no existe
     */
    BigDecimal validarConsistenciaTarifa(ItemVentaValidacionDTO item);

    /**
     * Valida de forma integral una lista de ítems de venta: agrupa tickets por atracción para
     * verificar que la suma total no exceda la capacidad máxima y comprueba cada tarifa contra la BD.
     *
     * @param items Lista de ítems en venta
     * @return Resultado consolidado con el total oficial y aforo restante por atracción
     */
    ResultadoValidacionVentaDTO validarItemsVenta(List<ItemVentaValidacionDTO> items);
}
