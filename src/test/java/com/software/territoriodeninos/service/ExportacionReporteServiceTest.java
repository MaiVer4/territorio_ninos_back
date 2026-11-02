package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.util.EstadoConciliacionTurno;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.TipoAtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportacionReporteServiceTest - Pruebas de exportación a PDF y CSV con membrete institucional y BOM UTF-8 (HU-39.02 a HU-39.04)")
class ExportacionReporteServiceTest {

    @Mock
    private ConciliacionTurnoService conciliacionTurnoService;

    @Mock
    private AuditoriaOperacionService auditoriaOperacionService;

    @Mock
    private ConfiguracionParqueService configuracionParqueService;

    @InjectMocks
    private ExportacionReporteServiceImpl exportacionReporteService;

    private ReporteTurnoDTO turnoDTO;
    private RotacionAtraccionDTO rotacionDTO;
    private IncidenteDetalleDTO incidenteDTO;

    @BeforeEach
    void setUp() {
        ConfiguracionParqueDTO config = ConfiguracionParqueDTO.builder()
                .nombreParque("Parque de Diversiones Aventura")
                .nit("900.876.543-1")
                .direccion("Av. Infantil 123")
                .telefono("310-000-0000")
                .build();
        lenient().when(configuracionParqueService.obtenerConfiguracion()).thenReturn(config);

        turnoDTO = ReporteTurnoDTO.builder()
                .turnoId(1L)
                .usuarioId(2L)
                .nombreCajero("Pedro Cajero")
                .fechaApertura(LocalDateTime.now().minusHours(8))
                .fechaCierre(LocalDateTime.now())
                .baseInicial(new BigDecimal("50000.00"))
                .ventasEfectivoSistema(new BigDecimal("100000.00"))
                .ventasElectronicoSistema(new BigDecimal("40000.00"))
                .totalEsperadoEfectivo(new BigDecimal("150000.00"))
                .montoFisicoDeclarado(new BigDecimal("150000.00"))
                .diferencia(BigDecimal.ZERO)
                .estadoConciliacion(EstadoConciliacionTurno.CUADRADO)
                .estadoTurno(EstadoTurnoCaja.CERRADO)
                .totalFacturado(new BigDecimal("140000.00"))
                .totalTransacciones(10)
                .build();

        rotacionDTO = RotacionAtraccionDTO.builder()
                .atraccionId(5L)
                .nombreAtraccion("Barco Pirata")
                .tipoAtraccion(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(20)
                .cantidadTotalUsos(45L)
                .ingresosBrutos(new BigDecimal("225000.00"))
                .tiempoPromedioMinutos(15.0)
                .porcentajeOcupacion(65.0)
                .horaMayorAfluencia("16:00 - 17:00")
                .build();

        incidenteDTO = IncidenteDetalleDTO.builder()
                .incidenteId(1L)
                .atraccionId(5L)
                .atraccionNombre("Barco Pirata")
                .tipoPausa("OPERATIVA")
                .motivo("Mantenimiento preventivo")
                .operarioNombre("Juan Operario")
                .fechaInicioPausa(LocalDateTime.now().minusHours(1))
                .fechaFinPausa(LocalDateTime.now().minusMinutes(45))
                .duracionSegundos(900)
                .duracionMinutos(15.0)
                .build();
    }

    @Test
    @DisplayName("Debe generar PDF de turnos con encabezado institucional y totales (HU-39.02, HU-39.03)")
    void testExportarPdfTurnos() {
        when(conciliacionTurnoService.obtenerReporteTurnos(any(), any(), any()))
                .thenReturn(List.of(turnoDTO));

        byte[] pdf = exportacionReporteService.exportarPdf("TURNOS", null, null, null, null);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
        // Validar magic bytes de PDF: %PDF
        String magic = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertThat(magic).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Debe generar PDF de rotación por atracción (HU-39.03)")
    void testExportarPdfRotacion() {
        when(conciliacionTurnoService.obtenerRotacionAtracciones(any(), any(), any(), any()))
                .thenReturn(List.of(rotacionDTO));

        byte[] pdf = exportacionReporteService.exportarPdf("ROTACION", null, null, null, null);

        assertThat(pdf).isNotNull();
        String magic = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertThat(magic).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Debe generar CSV con marca BOM UTF-8 y encabezados correctos (HU-39.04)")
    void testExportarCsvTurnosConBom() {
        when(conciliacionTurnoService.obtenerReporteTurnos(any(), any(), any()))
                .thenReturn(List.of(turnoDTO));

        byte[] csv = exportacionReporteService.exportarCsv("TURNOS", null, null, null, null);

        assertThat(csv).isNotNull();
        // Validar bytes UTF-8 BOM: 0xEF, 0xBB, 0xBF
        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);

        String contenido = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(contenido).contains("ID_TURNO;CAJERO;FECHA_APERTURA");
        assertThat(contenido).contains("Pedro Cajero");
        assertThat(contenido).contains("CUADRADO");
    }
}
