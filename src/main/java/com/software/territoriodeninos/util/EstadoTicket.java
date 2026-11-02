package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoTicket {
    PAGADO,
    EN_USO,
    CONSUMIDO,
    ANULADO;

    @JsonCreator
    public static EstadoTicket fromString(String value) {
        if (value == null || value.isBlank()) {
            return PAGADO;
        }
        return EstadoTicket.valueOf(value.trim().toUpperCase());
    }
}
