package com.software.territoriodeninos.util;

import com.software.territoriodeninos.exception.ReglaNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilidad para parsear parámetros de fecha en endpoints REST aceptando tanto fechas simples (YYYY-MM-DD)
 * como marcas ISO LocalDateTime u OffsetDateTime.
 */
public final class FechaParametroUtil {

    private FechaParametroUtil() {
    }

    /**
     * Convierte un parámetro de fecha inicial en {@link LocalDateTime}.
     * Si se envía solo la fecha (YYYY-MM-DD), asume el inicio del día (00:00:00).
     */
    public static LocalDateTime parseInicio(String valor, String nombreParametro) {
        return parse(valor, nombreParametro, false);
    }

    /**
     * Convierte un parámetro de fecha final en {@link LocalDateTime}.
     * Si se envía solo la fecha (YYYY-MM-DD), asume el fin del día (23:59:59.999999999).
     */
    public static LocalDateTime parseFin(String valor, String nombreParametro) {
        return parse(valor, nombreParametro, true);
    }

    private static LocalDateTime parse(String valor, String nombreParametro, boolean finDeDia) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String v = valor.trim();

        if (v.length() == 10) {
            try {
                LocalDate fecha = LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
                return finDeDia ? fecha.atTime(LocalTime.MAX) : fecha.atStartOfDay();
            } catch (DateTimeParseException ignored) {
                throw new ReglaNegocioException(
                        "El parámetro '" + nombreParametro + "' con valor '" + v
                                + "' no es válido. Use formato YYYY-MM-DD o YYYY-MM-DDTHH:mm:ss.");
            }
        }

        try {
            return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(v).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw new ReglaNegocioException(
                        "El parámetro '" + nombreParametro + "' con valor '" + v
                                + "' no es válido. Use formato YYYY-MM-DD o YYYY-MM-DDTHH:mm:ss.");
            }
        }
    }
}
