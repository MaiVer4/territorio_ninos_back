package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoReporteAtraccion {
    ABIERTO,
    EN_PROCESO,
    RESUELTO;

    @JsonCreator
    public static EstadoReporteAtraccion fromString(String value) {
        if (value == null || value.isBlank()) {
            return ABIERTO;
        }
        String clean = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if ("CERRADO".equals(clean)) {
            return RESUELTO;
        }
        return EstadoReporteAtraccion.valueOf(clean);
    }
}
