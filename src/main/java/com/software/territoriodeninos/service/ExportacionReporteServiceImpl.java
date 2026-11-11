package com.software.territoriodeninos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.IncidenteDetalleDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportacionReporteServiceImpl implements ExportacionReporteService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Locale LOCALE_CO = Locale.of("es", "CO");

    private final ConciliacionTurnoService conciliacionTurnoService;
    private final AuditoriaOperacionService auditoriaOperacionService;
    private final ConfiguracionParqueService configuracionParqueService;

    @Override
    public byte[] exportarPdf(String tipoReporte, LocalDateTime inicio, LocalDateTime fin, Long usuarioId, Long atraccionId) {
        String tipo = tipoReporte != null ? tipoReporte.toUpperCase().trim() : "TURNOS";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 25, 25, 25, 25);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Encabezado institucional dinámico (HU-39.02)
            agregarEncabezadoInstitucional(document, tipo);

            switch (tipo) {
                case "ROTACION" -> generarTablaPdfRotacion(document, inicio, fin);
                case "INCIDENTES" -> generarTablaPdfIncidentes(document, inicio, fin, atraccionId);
                default -> generarTablaPdfTurnos(document, inicio, fin, usuarioId);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Error al exportar reporte PDF [{}]: {}", tipo, ex.getMessage(), ex);
            throw new ReglaNegocioException("Error en la maquetación y generación del reporte PDF.");
        }
    }

    @Override
    public byte[] exportarCsv(String tipoReporte, LocalDateTime inicio, LocalDateTime fin, Long usuarioId, Long atraccionId) {
        String tipo = tipoReporte != null ? tipoReporte.toUpperCase().trim() : "TURNOS";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Marca UTF-8 BOM para compatibilidad nativa con Microsoft Excel (HU-39.04)
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            switch (tipo) {
                case "ROTACION" -> generarCsvRotacion(pw, inicio, fin);
                case "INCIDENTES" -> generarCsvIncidentes(pw, inicio, fin, atraccionId);
                default -> generarCsvTurnos(pw, inicio, fin, usuarioId);
            }

            pw.flush();
            baos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Error al exportar reporte CSV [{}]: {}", tipo, ex.getMessage(), ex);
            throw new ReglaNegocioException("Error al generar el archivo CSV.");
        }
    }

    // ==========================================
    // METODOS DE MAQUETACION PDF
    // ==========================================

    private void agregarEncabezadoInstitucional(Document document, String tipoReporte) throws DocumentException {
        ConfiguracionParqueDTO config = null;
        try {
            config = configuracionParqueService.obtenerConfiguracion();
        } catch (Exception ex) {
            log.debug("Usando encabezado institucional por defecto debido a error al obtener configuración: {}", ex.getMessage());
        }

        String nombre = config != null && config.getNombreParque() != null ? config.getNombreParque() : "Territorio de Niños";
        String nit = config != null && config.getNit() != null ? "NIT: " + config.getNit() : "NIT: 901.458.789-0";
        String direccion = config != null && config.getDireccion() != null ? config.getDireccion() : "Calle 100 # 15-20";
        String telefono = config != null && config.getTelefono() != null ? "Tel: " + config.getTelefono() : "";

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(20, 45, 95));
        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font fontReporte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(40, 90, 160));

        Paragraph header = new Paragraph();
        header.add(new Chunk(nombre.toUpperCase() + "\n", fontTitulo));
        header.add(new Chunk(nit + " | " + direccion + " " + telefono + "\n", fontSub));
        header.add(new Chunk("REPORTE OFICIAL: " + tipoReporte + " - Emitido el: " + LocalDateTime.now().format(FECHA_FMT) + "\n\n", fontReporte));
        document.add(header);
    }

    private void generarTablaPdfTurnos(Document document, LocalDateTime inicio, LocalDateTime fin, Long usuarioId) throws DocumentException {
        List<ReporteTurnoDTO> turnos = conciliacionTurnoService.obtenerReporteTurnos(inicio, fin, usuarioId);

        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 2.2f, 2.2f, 2.2f, 1.8f, 1.8f, 1.8f, 1.8f, 1.6f});

        String[] headers = {"ID", "Cajero", "Apertura", "Cierre", "Base", "Ventas Efec.", "Esperado", "Reportado", "Estado"};
        agregarEncabezadosTabla(table, headers);

        BigDecimal totalEsperadoSum = BigDecimal.ZERO;
        BigDecimal totalReportadoSum = BigDecimal.ZERO;

        Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

        for (ReporteTurnoDTO t : turnos) {
            table.addCell(crearCelda(String.valueOf(t.turnoId()), fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(t.nombreCajero(), fontBody, Element.ALIGN_LEFT));
            table.addCell(crearCelda(t.fechaApertura() != null ? t.fechaApertura().format(FECHA_FMT) : "-", fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(t.fechaCierre() != null ? t.fechaCierre().format(FECHA_FMT) : "-", fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(formatearMoneda(t.baseInicial()), fontBody, Element.ALIGN_RIGHT));
            table.addCell(crearCelda(formatearMoneda(t.ventasEfectivoSistema()), fontBody, Element.ALIGN_RIGHT));
            table.addCell(crearCelda(formatearMoneda(t.totalEsperadoEfectivo()), fontBody, Element.ALIGN_RIGHT));
            table.addCell(crearCelda(formatearMoneda(t.montoFisicoDeclarado()), fontBody, Element.ALIGN_RIGHT));
            table.addCell(crearCelda(t.estadoConciliacion() != null ? t.estadoConciliacion().name() : "-", fontBody, Element.ALIGN_CENTER));

            if (t.totalEsperadoEfectivo() != null) totalEsperadoSum = totalEsperadoSum.add(t.totalEsperadoEfectivo());
            if (t.montoFisicoDeclarado() != null) totalReportadoSum = totalReportadoSum.add(t.montoFisicoDeclarado());
        }

        // Fila de totales monetarios (HU-39.03)
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
        PdfPCell celdaTotales = new PdfPCell(new Phrase("TOTALES:", fontBold));
        celdaTotales.setColspan(6);
        celdaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaTotales.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(celdaTotales);

        PdfPCell cEsp = new PdfPCell(new Phrase(formatearMoneda(totalEsperadoSum), fontBold));
        cEsp.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cEsp.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cEsp);

        PdfPCell cRep = new PdfPCell(new Phrase(formatearMoneda(totalReportadoSum), fontBold));
        cRep.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cRep.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cRep);

        PdfPCell cVacia = new PdfPCell(new Phrase("", fontBold));
        cVacia.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cVacia);

        document.add(table);
    }

    private void generarTablaPdfRotacion(Document document, LocalDateTime inicio, LocalDateTime fin) throws DocumentException {
        List<RotacionAtraccionDTO> rotacion = conciliacionTurnoService.obtenerRotacionAtracciones(inicio, fin, "MENSUAL", "USOS");

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 3.0f, 1.8f, 1.5f, 1.5f, 2.2f, 1.8f, 2.2f});

        String[] headers = {"ID", "Atracción", "Tipo", "Capacidad", "Usos", "Ingresos Brutos", "% Ocupación", "Hora Pico"};
        agregarEncabezadosTabla(table, headers);

        Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        long totalUsosSum = 0;
        BigDecimal totalIngresosSum = BigDecimal.ZERO;

        for (RotacionAtraccionDTO r : rotacion) {
            table.addCell(crearCelda(String.valueOf(r.atraccionId()), fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(r.nombreAtraccion(), fontBody, Element.ALIGN_LEFT));
            table.addCell(crearCelda(r.tipoAtraccion() != null ? r.tipoAtraccion().name() : "-", fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(String.valueOf(r.capacidadMaxima()), fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(String.valueOf(r.cantidadTotalUsos()), fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(formatearMoneda(r.ingresosBrutos()), fontBody, Element.ALIGN_RIGHT));
            table.addCell(crearCelda(r.porcentajeOcupacion() + " %", fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(r.horaMayorAfluencia(), fontBody, Element.ALIGN_CENTER));

            totalUsosSum += r.cantidadTotalUsos() != null ? r.cantidadTotalUsos() : 0;
            if (r.ingresosBrutos() != null) totalIngresosSum = totalIngresosSum.add(r.ingresosBrutos());
        }

        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
        PdfPCell cTot = new PdfPCell(new Phrase("TOTALES:", fontBold));
        cTot.setColspan(4);
        cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTot.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cTot);

        PdfPCell cUsos = new PdfPCell(new Phrase(String.valueOf(totalUsosSum), fontBold));
        cUsos.setHorizontalAlignment(Element.ALIGN_CENTER);
        cUsos.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cUsos);

        PdfPCell cIng = new PdfPCell(new Phrase(formatearMoneda(totalIngresosSum), fontBold));
        cIng.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cIng.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cIng);

        PdfPCell cVacia = new PdfPCell(new Phrase("", fontBold));
        cVacia.setColspan(2);
        cVacia.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cVacia);

        document.add(table);
    }

    private void generarTablaPdfIncidentes(Document document, LocalDateTime inicio, LocalDateTime fin, Long atraccionId) throws DocumentException {
        ResumenIncidentesAuditoriaDTO resumen = auditoriaOperacionService.consultarIncidentes(inicio, fin, atraccionId, null);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 2.5f, 2.0f, 3.5f, 2.2f, 2.2f, 1.6f});

        String[] headers = {"ID", "Atracción", "Tipo Parada", "Motivo", "Operario", "Inicio", "Duración (min)"};
        agregarEncabezadosTabla(table, headers);

        Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

        for (IncidenteDetalleDTO inc : resumen.incidentes()) {
            table.addCell(crearCelda(String.valueOf(inc.incidenteId()), fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(inc.atraccionNombre(), fontBody, Element.ALIGN_LEFT));
            table.addCell(crearCelda(inc.tipoPausa(), fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(inc.motivo(), fontBody, Element.ALIGN_LEFT));
            table.addCell(crearCelda(inc.operarioNombre(), fontBody, Element.ALIGN_LEFT));
            table.addCell(crearCelda(inc.fechaInicioPausa() != null ? inc.fechaInicioPausa().format(FECHA_FMT) : "-", fontBody, Element.ALIGN_CENTER));
            table.addCell(crearCelda(String.valueOf(inc.duracionMinutos()), fontBody, Element.ALIGN_RIGHT));
        }

        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
        PdfPCell cTot = new PdfPCell(new Phrase("INACTIVIDAD TOTAL (MIN):", fontBold));
        cTot.setColspan(6);
        cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTot.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cTot);

        PdfPCell cMin = new PdfPCell(new Phrase(String.valueOf(resumen.tiempoTotalInactividadMinutos()), fontBold));
        cMin.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cMin.setBackgroundColor(new Color(235, 240, 250));
        table.addCell(cMin);

        document.add(table);
    }

    private void agregarEncabezadosTabla(PdfPTable table, String[] headers) {
        Font fontHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Color bgColor = new Color(28, 55, 105);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHead));
            cell.setBackgroundColor(bgColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPCell crearCelda(String texto, Font font, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(4);
        return cell;
    }

    private String formatearMoneda(BigDecimal valor) {
        DecimalFormat fmt = new DecimalFormat("$ #,##0.00", new DecimalFormatSymbols(LOCALE_CO));
        return fmt.format(valor != null ? valor : BigDecimal.ZERO);
    }

    private String sanitizarCeldaCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace("\r", " ").replace("\n", " ").replace("\"", "\"\"");
        if (!limpio.isEmpty()) {
            char c = limpio.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@') {
                limpio = "'" + limpio;
            }
        }
        return limpio;
    }

    // ==========================================
    // METODOS DE EXPORTACION CSV
    // ==========================================

    private void generarCsvTurnos(PrintWriter pw, LocalDateTime inicio, LocalDateTime fin, Long usuarioId) {
        List<ReporteTurnoDTO> turnos = conciliacionTurnoService.obtenerReporteTurnos(inicio, fin, usuarioId);

        pw.println("ID_TURNO;CAJERO;FECHA_APERTURA;FECHA_CIERRE;BASE_INICIAL;VENTAS_EFECTIVO;VENTAS_ELECTRONICO;TOTAL_ESPERADO;MONTO_REPORTADO;DIFERENCIA;ESTADO_CONCILIACION");

        for (ReporteTurnoDTO t : turnos) {
            pw.printf(Locale.US, "%d;\"%s\";\"%s\";\"%s\";%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;\"%s\"%n",
                    t.turnoId(),
                    sanitizarCeldaCsv(t.nombreCajero()),
                    t.fechaApertura() != null ? t.fechaApertura().format(FECHA_FMT) : "",
                    t.fechaCierre() != null ? t.fechaCierre().format(FECHA_FMT) : "",
                    t.baseInicial(),
                    t.ventasEfectivoSistema(),
                    t.ventasElectronicoSistema(),
                    t.totalEsperadoEfectivo(),
                    t.montoFisicoDeclarado(),
                    t.diferencia(),
                    t.estadoConciliacion() != null ? t.estadoConciliacion().name() : ""
            );
        }
    }

    private void generarCsvRotacion(PrintWriter pw, LocalDateTime inicio, LocalDateTime fin) {
        List<RotacionAtraccionDTO> rotacion = conciliacionTurnoService.obtenerRotacionAtracciones(inicio, fin, "MENSUAL", "USOS");

        pw.println("ID_ATRACCION;NOMBRE;TIPO;CAPACIDAD_MAXIMA;TOTAL_USOS;INGRESOS_BRUTOS;TIEMPO_PROMEDIO_MIN;PORCENTAJE_OCUPACION;HORA_PICO");

        for (RotacionAtraccionDTO r : rotacion) {
            pw.printf(Locale.US, "%d;\"%s\";\"%s\";%d;%d;%.2f;%.2f;%.2f;\"%s\"%n",
                    r.atraccionId(),
                    sanitizarCeldaCsv(r.nombreAtraccion()),
                    r.tipoAtraccion() != null ? r.tipoAtraccion().name() : "",
                    r.capacidadMaxima(),
                    r.cantidadTotalUsos(),
                    r.ingresosBrutos(),
                    r.tiempoPromedioMinutos(),
                    r.porcentajeOcupacion(),
                    sanitizarCeldaCsv(r.horaMayorAfluencia())
            );
        }
    }

    private void generarCsvIncidentes(PrintWriter pw, LocalDateTime inicio, LocalDateTime fin, Long atraccionId) {
        ResumenIncidentesAuditoriaDTO resumen = auditoriaOperacionService.consultarIncidentes(inicio, fin, atraccionId, null);

        pw.println("ID_INCIDENTE;ATRACCION;TIPO_PAUSA;MOTIVO;OPERARIO;FECHA_INICIO;FECHA_FIN;DURACION_SEGUNDOS;DURACION_MINUTOS");

        for (IncidenteDetalleDTO inc : resumen.incidentes()) {
            pw.printf(Locale.US, "%d;\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";%d;%.2f%n",
                    inc.incidenteId(),
                    sanitizarCeldaCsv(inc.atraccionNombre()),
                    sanitizarCeldaCsv(inc.tipoPausa()),
                    sanitizarCeldaCsv(inc.motivo()),
                    sanitizarCeldaCsv(inc.operarioNombre()),
                    inc.fechaInicioPausa() != null ? inc.fechaInicioPausa().format(FECHA_FMT) : "",
                    inc.fechaFinPausa() != null ? inc.fechaFinPausa().format(FECHA_FMT) : "",
                    inc.duracionSegundos(),
                    inc.duracionMinutos()
            );
        }
    }
}
