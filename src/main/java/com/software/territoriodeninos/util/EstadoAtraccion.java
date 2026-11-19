package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoAtraccion {
    ACTIVO,
    INACTIVO,
    MANTENIMIENTO,
    FUERA_DE_SERVICIO;

    @JsonCreator
    public static EstadoAtraccion fromString(String value) {
        if (value == null) {
            return ACTIVO;
        }
        String clean = value.trim().toUpperCase();
        if ("ACTIVA".equals(clean)) {
            return ACTIVO;
        }
        if ("INACTIVA".equals(clean)) {
            return INACTIVO;
        }
        if ("FUERADESERVICIO".equals(clean) || "FUERA DE SERVICIO".equals(clean) || "FUERA-DE-SERVICIO".equals(clean)) {
            return FUERA_DE_SERVICIO;
        }
        return EstadoAtraccion.valueOf(clean);
    }
}
