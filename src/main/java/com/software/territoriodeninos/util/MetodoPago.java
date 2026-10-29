package com.software.territoriodeninos.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.software.territoriodeninos.exception.ReglaNegocioException;

public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
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
            case "CARD", "TARJETA" -> TARJETA;
            case "TARJETA_DEBITO", "DEBITO" -> TARJETA_DEBITO;
            case "TARJETA_CREDITO", "CREDITO" -> TARJETA_CREDITO;
            case "TRANSFER", "TRANSFERENCIA" -> TRANSFERENCIA;
            case "POS", "DATAFONO" -> DATAFONO;
            case "NEQUI" -> NEQUI;
            case "DAVIPLATA" -> DAVIPLATA;
            case "PSE" -> PSE;
            case "MIXTO" -> MIXTO;
            default -> {
                try {
                    yield MetodoPago.valueOf(clean);
                } catch (IllegalArgumentException e) {
                    throw new ReglaNegocioException("Método de pago no válido: '" + value +
                            "'. Métodos permitidos: EFECTIVO, TARJETA, TARJETA_DEBITO, TARJETA_CREDITO, TRANSFERENCIA, DATAFONO, NEQUI, DAVIPLATA, PSE, MIXTO");
                }
            }
        };
    }
}
