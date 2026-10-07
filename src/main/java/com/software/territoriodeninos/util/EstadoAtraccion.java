package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoAtraccion {
    ACTIVO,
    INACTIVO,
    MANTENIMIENTO;

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
        return EstadoAtraccion.valueOf(clean);
    }
}
