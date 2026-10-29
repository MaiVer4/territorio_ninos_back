package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    DATAFONO,
    NEQUI,
    DAVIPLATA,
    PSE,
    MIXTO;

    @JsonCreator
    public static MetodoPago fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
        return switch (clean) {
            case "CASH", "EFECTIVO" -> EFECTIVO;
            case "CARD", "TARJETA", "CREDITO", "DEBITO" -> TARJETA;
            case "TRANSFER", "TRANSFERENCIA" -> TRANSFERENCIA;
            case "POS", "DATAFONO" -> DATAFONO;
            default -> MetodoPago.valueOf(clean);
        };
    }
}
