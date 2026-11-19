package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoReporteAtraccion {
    DANO,
    FALTA_SUPLEMENTOS,
    MANTENIMIENTO_PREVENTIVO,
    OTRO;

    @JsonCreator
    public static TipoReporteAtraccion fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTRO;
        }
        String clean = value.trim().toUpperCase()
                .replace("Ñ", "N")
                .replace(" ", "_")
                .replace("-", "_");
        if ("INSUMOS".equals(clean) || "SIN_SUPLEMENTOS".equals(clean) || "SUPLEMENTOS".equals(clean)) {
            return FALTA_SUPLEMENTOS;
        }
        if ("FALLA".equals(clean) || "AVERIA".equals(clean) || "DANIO".equals(clean)) {
            return DANO;
        }
        return TipoReporteAtraccion.valueOf(clean);
    }
}
