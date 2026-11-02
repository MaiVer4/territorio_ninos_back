package com.software.territoriodeninos.util;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.TicketPrintDataDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formateador de plantillas térmicas estructuradas bajo el estándar ESC/POS (HU-21.01, HU-21.02).
 * Inyecta datos institucionales desde ConfiguracionParque, datos del ticket y menor,
 * pie legal de hasta 2000 caracteres, y añade comandos de centrado (ESC a 1) y
 * corte automático de papel GS V 66 0 (Notion: CA-01, CA-02).
 */
@Component
public class TicketPrintFormatter {

    // Comandos de control ESC/POS
    public static final String ESC_INIT = "\u001B@";               // ESC @ : Inicializar impresora
    public static final String ESC_ALIGN_LEFT = "\u001Ba\u0000";   // ESC a 0 : Alineación izquierda
    public static final String ESC_ALIGN_CENTER = "\u001Ba\u0001"; // ESC a 1 : Centrado de encabezados (CA-02)
    public static final String ESC_BOLD_ON = "\u001BE\u0001";      // ESC E 1 : Negrita activada
    public static final String ESC_BOLD_OFF = "\u001BE\u0000";     // ESC E 0 : Negrita desactivada
    public static final String GS_AUTO_CUT = "\u001DV\u0042\u0000";// GS V 66 0 (0x1D 0x56 0x42 0x00) : Corte automático parcial (CA-02)

    public static final int MAX_PIE_LEGAL_LENGTH = 2000;
    public static final int COLS_80MM = 48;
    public static final int COLS_58MM = 32;

    private static final String LF = "\n";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Locale LOCALE_CO = new Locale.Builder().setLanguage("es").setRegion("CO").build();

    /**
     * Construye el payload térmico ESC/POS estructurado para el ticket y la configuración dada.
     *
     * @param ticket Datos resueltos de la boleta y el menor.
     * @param config Configuración institucional del parque (NIT, razón social, dirección, pie legal).
     * @return Cadena con comandos binarios ESC/POS y texto plano formateado.
     */
    public String formatear(TicketPrintDataDTO ticket, ConfiguracionParqueDTO config) {
        if (ticket == null) {
            throw new IllegalArgumentException("Los datos del ticket para impresión no pueden ser nulos");
        }

        int anchoCols = (config != null && config.getAnchoPapelMm() != null && config.getAnchoPapelMm() <= 58)
                ? COLS_58MM
                : COLS_80MM;

        StringBuilder sb = new StringBuilder();

        // 1. Inicializar impresora
        sb.append(ESC_INIT);

        // 2. Encabezado Institucional Centrado (ESC a 1)
        sb.append(ESC_ALIGN_CENTER);
        sb.append(ESC_BOLD_ON);

        String nombreParque = resolverNombreParque(config);
        sb.append(nombreParque.toUpperCase()).append(LF);
        sb.append(ESC_BOLD_OFF);

        if (config != null) {
            if (StringUtils.hasText(config.getRazonSocial())) {
                sb.append(config.getRazonSocial().trim()).append(LF);
            }
            if (StringUtils.hasText(config.getNit())) {
                sb.append("NIT: ").append(config.getNit().trim()).append(LF);
            }
            if (StringUtils.hasText(config.getDireccion())) {
                sb.append(config.getDireccion().trim()).append(LF);
            }
            if (StringUtils.hasText(config.getCiudad())) {
                sb.append(config.getCiudad().trim()).append(LF);
            }
            if (StringUtils.hasText(config.getTelefono())) {
                sb.append("Tel: ").append(config.getTelefono().trim()).append(LF);
            }
        }

        // 3. Separador doble
        sb.append(generarLinea('=', anchoCols)).append(LF);

        // 4. Título del Comprobante / Boleta
        sb.append(ESC_BOLD_ON);
        sb.append("BOLETO DE ENTRADA").append(LF);
        sb.append(ESC_BOLD_OFF);
        sb.append(generarLinea('-', anchoCols)).append(LF);

        // 5. Datos operativos del Ticket (Alineación izquierda ESC a 0)
        sb.append(ESC_ALIGN_LEFT);

        if (ticket.fechaEmision() != null) {
            sb.append("Fecha/Hora : ").append(ticket.fechaEmision().format(DATE_FORMATTER)).append(LF);
        }

        sb.append("Atracción  : ").append(ESC_BOLD_ON)
                .append(ticket.nombreAtraccion() != null ? ticket.nombreAtraccion() : "N/A")
                .append(ESC_BOLD_OFF).append(LF);

        sb.append("Modalidad  : ").append(ticket.tiempoOModalidad() != null ? ticket.tiempoOModalidad() : "N/A").append(LF);

        sb.append("Menor      : ").append(ESC_BOLD_ON)
                .append(StringUtils.hasText(ticket.nombreMenor()) ? ticket.nombreMenor() : "Sin menor asignado")
                .append(ESC_BOLD_OFF).append(LF);

        if (ticket.precioUnitario() != null) {
            sb.append("Valor Pag. : ").append(formatearMoneda(ticket.precioUnitario())).append(" COP").append(LF);
        }

        sb.append(generarLinea('-', anchoCols)).append(LF);

        // 6. UUID de Validación / Código de Control (Centrado)
        sb.append(ESC_ALIGN_CENTER);
        sb.append(ESC_BOLD_ON);
        sb.append("CÓDIGO ÚNICO DE CONTROL").append(LF);
        sb.append(ESC_BOLD_OFF);
        sb.append(ticket.uuidTicket() != null ? ticket.uuidTicket().toString() : "N/A").append(LF);
        sb.append(generarLinea('=', anchoCols)).append(LF);

        // 7. Pie Legal de hasta 2000 caracteres (Alineación izquierda con salto inteligente de palabras)
        String pieLegal = resolverPieLegal(config);
        if (StringUtils.hasText(pieLegal)) {
            sb.append(ESC_ALIGN_LEFT);
            sb.append(ESC_BOLD_ON).append("TÉRMINOS Y CONDICIONES:").append(ESC_BOLD_OFF).append(LF);

            String pieProcesado = pieLegal.trim();
            if (pieProcesado.length() > MAX_PIE_LEGAL_LENGTH) {
                pieProcesado = pieProcesado.substring(0, MAX_PIE_LEGAL_LENGTH);
            }

            for (String linea : envolverTexto(pieProcesado, anchoCols)) {
                sb.append(linea).append(LF);
            }
            sb.append(generarLinea('-', anchoCols)).append(LF);
        }

        // 8. Mensaje de Despedida Institucional (Centrado)
        String mensajePie = (config != null && StringUtils.hasText(config.getMensajePie()))
                ? config.getMensajePie().trim()
                : "¡Gracias por su visita!";
        sb.append(ESC_ALIGN_CENTER);
        sb.append(mensajePie).append(LF);

        // 9. Avance de papel para dejar margen antes del cabezal de corte
        sb.append(LF).append(LF).append(LF);

        // 10. Comando de Corte Automático de Papel (GS V 66 0) (CA-02)
        sb.append(GS_AUTO_CUT);

        return sb.toString();
    }

    private String resolverNombreParque(ConfiguracionParqueDTO config) {
        if (config == null) {
            return "Territorio de Niños";
        }
        if (StringUtils.hasText(config.getNombreComercial())) {
            return config.getNombreComercial().trim();
        }
        if (StringUtils.hasText(config.getNombreParque())) {
            return config.getNombreParque().trim();
        }
        return "Territorio de Niños";
    }

    private String resolverPieLegal(ConfiguracionParqueDTO config) {
        if (config == null) {
            return null;
        }
        if (StringUtils.hasText(config.getPieTicketLegal())) {
            return config.getPieTicketLegal();
        }
        if (StringUtils.hasText(config.getPoliticasSeguridad())) {
            return config.getPoliticasSeguridad();
        }
        return null;
    }

    private String generarLinea(char caracter, int longitud) {
        return String.valueOf(caracter).repeat(Math.max(0, longitud));
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) return "$ 0";
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_CO);
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return "$ " + nf.format(valor);
    }

    /**
     * Divide un texto en líneas que no excedan el ancho de columnas dado, respetando los límites de palabras.
     */
    private List<String> envolverTexto(String texto, int maxCols) {
        List<String> resultado = new ArrayList<>();
        if (!StringUtils.hasText(texto)) {
            return resultado;
        }

        String[] parrafos = texto.split("\\r?\\n");
        for (String parrafo : parrafos) {
            if (parrafo.isBlank()) {
                resultado.add("");
                continue;
            }

            String[] palabras = parrafo.split("\\s+");
            StringBuilder lineaActual = new StringBuilder();

            for (String palabra : palabras) {
                if (lineaActual.length() == 0) {
                    if (palabra.length() <= maxCols) {
                        lineaActual.append(palabra);
                    } else {
                        // Si la palabra es más larga que toda la línea, dividirla
                        int idx = 0;
                        while (idx < palabra.length()) {
                            int end = Math.min(idx + maxCols, palabra.length());
                            resultado.add(palabra.substring(idx, end));
                            idx = end;
                        }
                    }
                } else if (lineaActual.length() + 1 + palabra.length() <= maxCols) {
                    lineaActual.append(" ").append(palabra);
                } else {
                    resultado.add(lineaActual.toString());
                    lineaActual.setLength(0);
                    if (palabra.length() <= maxCols) {
                        lineaActual.append(palabra);
                    } else {
                        int idx = 0;
                        while (idx < palabra.length()) {
                            int end = Math.min(idx + maxCols, palabra.length());
                            resultado.add(palabra.substring(idx, end));
                            idx = end;
                        }
                    }
                }
            }

            if (lineaActual.length() > 0) {
                resultado.add(lineaActual.toString());
            }
        }

        return resultado;
    }
}
