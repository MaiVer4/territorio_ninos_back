package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoVenta {
    PAGADO,
    COMPLETADA,
    PENDIENTE,
    ANULADA,
    CANCELADA;

    @JsonCreator
    public static EstadoVenta fromString(String value) {
        if (value == null || value.isBlank()) {
            return PAGADO;
        }
        String clean = value.trim().toUpperCase();
        if ("PAGADA".equals(clean) || "PAGADO".equals(clean) || "EXITOSA".equals(clean)) {
            return PAGADO;
        }
        if ("COMPLETADA".equals(clean) || "COMPLETADO".equals(clean)) {
            return COMPLETADA;
        }
        return EstadoVenta.valueOf(clean);
    }
}
