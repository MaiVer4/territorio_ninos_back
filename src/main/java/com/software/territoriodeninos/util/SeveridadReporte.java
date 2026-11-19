package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SeveridadReporte {
    BAJA,
    MEDIA,
    ALTA,
    CRITICA;

    @JsonCreator
    public static SeveridadReporte fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIA;
        }
        return SeveridadReporte.valueOf(value.trim().toUpperCase().replace("Í", "I"));
    }
}
