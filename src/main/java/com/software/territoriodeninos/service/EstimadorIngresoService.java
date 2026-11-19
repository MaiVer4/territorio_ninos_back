package com.software.territoriodeninos.service;

import com.software.territoriodeninos.entity.Atraccion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estima la hora de ingreso de nuevas boletas cuando la atracción puede estar llena (cola FIFO).
 */
public interface EstimadorIngresoService {

    /** Minutos tras la hora estimada a partir de los cuales una boleta PAGADO no escaneada deja de reservar cupo. */
    int MINUTOS_EXPIRACION_RESERVA = 15;

    /**
     * Calcula la hora estimada de ingreso para cada una de las nuevas boletas en orden.
     *
     * @param atraccion         atracción destino
     * @param duracionesMinutos duración contratada de cada nueva boleta (en orden de emisión)
     * @param ahora             instante de referencia
     * @return lista del mismo tamaño; cada elemento es {@code null} si el ingreso es inmediato (AHORA)
     */
    List<LocalDateTime> estimar(Atraccion atraccion, List<Integer> duracionesMinutos, LocalDateTime ahora);

    /** Hora en que podría ingresar una boleta nueva adicional ({@code null} = AHORA). */
    LocalDateTime estimarProximoIngreso(Atraccion atraccion, LocalDateTime ahora);
}
