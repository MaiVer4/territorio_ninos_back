package com.software.territoriodeninos.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utilidades para el parseo y manejo flexible de fechas y rangos temporales en reportes.
 */
public final class FechaUtils {

    private FechaUtils() {}

    /**
     * Parsea una cadena de fecha de manera flexible soportando:
     * - ISO Date: "YYYY-MM-DD" (se ajusta a inicio o fin del día según el flag finDelDia)
     * - ISO Date Time local: "YYYY-MM-DDTHH:mm:ss" o "YYYY-MM-DDTHH:mm:ss.SSS"
     * - ISO Date Time con offset/zona: "YYYY-MM-DDTHH:mm:ssZ" o con desfase (+05:00)
     * - Fecha con espacio: "YYYY-MM-DD HH:mm:ss"
     *
     * @param fechaStr Cadena recibida (puede ser nula o en blanco)
     * @param finDelDia Si es true y solo se recibe la fecha sin hora, se ajusta a las 23:59:59.999999999
     * @return LocalDateTime resultante o null si no se proporcionó o no es válida
     */
    public static LocalDateTime parsearFechaFlexible(String fechaStr, boolean finDelDia) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return null;
        }
        String s = fechaStr.trim();

        // 1. Formato ISO Local Date Time (ej: 2026-11-14T10:30:00)
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        // 2. Formato con Zona/Offset ISO (ej: 2026-11-14T10:30:00Z)
        try {
            return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {}

        // 3. Formato ISO Date Time genérico
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {}

        // 4. Formato clásico con espacio "yyyy-MM-dd HH:mm:ss"
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {}

        // 5. Formato solo fecha "yyyy-MM-dd"
        try {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return finDelDia ? d.atTime(LocalTime.MAX) : d.atStartOfDay();
        } catch (Exception ignored) {}

        // 6. Subcadena si empieza con YYYY-MM-DD (longitud >= 10)
        if (s.length() >= 10) {
            try {
                LocalDate d = LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                return finDelDia ? d.atTime(LocalTime.MAX) : d.atStartOfDay();
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Resuelve el rango temporal en base al período solicitado y/o fechas explícitas.
     */
    public static RangoFechas resolverRangoFechas(String periodo, String fechaInicio, String fechaFin) {
        LocalDateTime fin = parsearFechaFlexible(fechaFin, true);
        LocalDateTime inicio = parsearFechaFlexible(fechaInicio, false);
        return resolverRangoFechas(periodo, inicio, fin);
    }

    /**
     * Resuelve el rango temporal en base al período solicitado y/o fechas explícitas en LocalDateTime.
     */
    public static RangoFechas resolverRangoFechas(String periodo, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        LocalDateTime fin = fechaFin != null ? fechaFin : LocalDateTime.now().with(LocalTime.MAX);
        LocalDateTime inicio = fechaInicio;

        if (inicio == null) {
            String p = periodo != null ? periodo.toUpperCase().trim() : "HOY";
            inicio = switch (p) {
                case "HOY", "DIARIO" -> LocalDate.now().atStartOfDay();
                case "ULTIMOS_7_DIAS", "SEMANAL" -> LocalDate.now().minusDays(6).atStartOfDay();
                case "ESTE_MES" -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
                case "MENSUAL" -> fin.minusDays(30);
                case "PERSONALIZADO" -> fin.minusDays(30);
                default -> LocalDate.now().atStartOfDay();
            };
        }

        return new RangoFechas(inicio, fin);
    }

    public record RangoFechas(LocalDateTime inicio, LocalDateTime fin) {}
}
