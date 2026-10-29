package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoVenta {
    COMPLETADA,
    PENDIENTE,
    ANULADA,
    CANCELADA;

    @JsonCreator
    public static EstadoVenta fromString(String value) {
        if (value == null || value.isBlank()) {
            return COMPLETADA;
        }
        String clean = value.trim().toUpperCase();
        if ("PAGADA".equals(clean) || "EXITOSA".equals(clean)) {
            return COMPLETADA;
        }
        return EstadoVenta.valueOf(clean);
    }
}
