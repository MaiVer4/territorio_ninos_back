package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoTarifa {
    TIEMPO,
    EVENTO,
    MINUTO_BASE;

    @JsonCreator
    public static TipoTarifa fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim().toUpperCase()
                .replace("Í", "I");
        return switch (clean) {
            case "TIEMPO", "TIME", "TEMPORIZADA", "POR_TIEMPO" -> TIEMPO;
            case "EVENTO", "EVENTOS", "EVENTOS_CAIDAS", "CAIDAS", "POR_EVENTO" -> EVENTO;
            case "MINUTO", "MINUTO_BASE", "BASE" -> MINUTO_BASE;
            default -> TipoTarifa.valueOf(clean);
        };
    }
}
