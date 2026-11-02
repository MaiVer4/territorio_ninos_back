package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.software.territoriodeninos.exception.ReglaNegocioException;

public enum EstadoTicket {
    PAGADO,
    EN_USO,
    PAUSADO,
    CONSUMIDO,
    VENCIDO,
    ANULADO;

    @JsonCreator
    public static EstadoTicket fromString(String value) {
        if (value == null || value.isBlank()) {
            return PAGADO;
        }
        try {
            return EstadoTicket.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException(
                    "Estado de ticket inválido: '" + value + "'. Valores permitidos: PAGADO, EN_USO, PAUSADO, CONSUMIDO, VENCIDO, ANULADO"
            );
        }
    }
}
