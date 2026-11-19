package com.software.territoriodeninos.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Traduce la hora estimada de ingreso a texto visible en POS / tirilla.
 */
public final class IngresoTextoFormatter {

    public static final String AHORA = "AHORA";
    public static final String PROXIMO_TURNO = "PRÓXIMO TURNO";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private IngresoTextoFormatter() {
    }

    public static String texto(LocalDateTime horaIngresoEstimada, TipoAtraccion tipo) {
        if (horaIngresoEstimada == null) {
            return AHORA;
        }
        if (TipoAtraccion.EVENTOS_CAIDAS.equals(tipo)) {
            return PROXIMO_TURNO;
        }
        return horaIngresoEstimada.format(HORA) + " (aprox.)";
    }
}
