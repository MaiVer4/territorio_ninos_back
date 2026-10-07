package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoAsignacion {
    ACTIVA,
    FINALIZADA,
    REASIGNADA;

    @JsonCreator
    public static EstadoAsignacion fromString(String value) {
        if (value == null) {
            return ACTIVA;
        }
        return EstadoAsignacion.valueOf(value.trim().toUpperCase());
    }
}
