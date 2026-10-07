package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoAtraccion {
    TEMPORIZADA,
    EVENTOS_CAIDAS;

    @JsonCreator
    public static TipoAtraccion fromString(String value) {
        if (value == null) {
            return TEMPORIZADA;
        }
        String clean = value.trim().toUpperCase();
        if ("TIME".equals(clean) || "TEMPORIZADA".equals(clean) || "TIEMPO".equals(clean) || "VIP".equals(clean)) {
            return TEMPORIZADA;
        }
        if ("FALLS".equals(clean) || "EVENTOS_CAIDAS".equals(clean) || "CAIDAS".equals(clean) || "CAÍDAS".equals(clean) || "EVENTO".equals(clean) || "EVENTOS".equals(clean)) {
            return EVENTOS_CAIDAS;
        }
        return TipoAtraccion.valueOf(clean);
    }
}
