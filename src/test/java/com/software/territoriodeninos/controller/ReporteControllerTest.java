package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.software.territoriodeninos.dto.DetalleTurnoAuditadoDTO;
import com.software.territoriodeninos.dto.ReporteTurnoDTO;
import com.software.territoriodeninos.dto.RotacionAtraccionDTO;
import com.software.territoriodeninos.service.ConciliacionTurnoService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteControllerTest - Pruebas de endpoints REST para reportes contables y rotación (HU-36.05, HU-36.06, HU-37.04)")
class ReporteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConciliacionTurnoService conciliacionTurnoService;

    @Mock
    private com.software.territoriodeninos.service.ExportacionReporteService exportacionReporteService;

    @Mock
    private com.software.territoriodeninos.service.EstadisticasReporteService estadisticasReporteService;

    @InjectMocks
    private ReporteController reporteController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(reporteController).build();
    }

    @Test
    @DisplayName("GET /api/reportes/turnos debe retornar lista de turnos conciliados (HU-36.05)")
    void testObtenerReporteTurnos() throws Exception {
        ReporteTurnoDTO dto = ReporteTurnoDTO.builder()
                .turnoId(1L)
                .usuarioId(2L)
                .nombreCajero("Pedro Cajero")
                .fechaApertura(LocalDateTime.now().minusHours(8))
                .fechaCierre(LocalDateTime.now())
                .baseInicial(new BigDecimal("50000.00"))
                .ventasEfectivoSistema(new BigDecimal("100000.00"))
                .ventasElectronicoSistema(new BigDecimal("50000.00"))
                .totalEsperadoEfectivo(new BigDecimal("150000.00"))
                .montoFisicoDeclarado(new BigDecimal("150000.00"))
                .diferencia(BigDecimal.ZERO)
                .estadoConciliacion(EstadoConciliacionTurno.CUADRADO)
                .estadoTurno(EstadoTurnoCaja.CERRADO)
                .totalFacturado(new BigDecimal("150000.00"))
                .totalTransacciones(10)
                .build();

        when(conciliacionTurnoService.obtenerReporteTurnos(any(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reportes/turnos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].turnoId", is(1)))
                .andExpect(jsonPath("$[0].nombreCajero", is("Pedro Cajero")))
                .andExpect(jsonPath("$[0].estadoConciliacion", is("CUADRADO")));
    }

    @Test
    @DisplayName("GET /api/reportes/turnos/{id}/detalle debe retornar el desglose discriminado (HU-36.06)")
    void testObtenerDetalleTurno() throws Exception {
        ReporteTurnoDTO turnoDto = ReporteTurnoDTO.builder()
                .turnoId(5L)
                .estadoConciliacion(EstadoConciliacionTurno.CUADRADO)
                .build();

        DetalleTurnoAuditadoDTO.VentaAuditadaDTO ventaDto = DetalleTurnoAuditadoDTO.VentaAuditadaDTO.builder()
                .ventaId(10L)
                .clienteNombre("Ana López")
                .metodoPago("EFECTIVO")
                .montoTotal(new BigDecimal("20000.00"))
                .lineas(List.of())
                .build();

        DetalleTurnoAuditadoDTO detalleDto = DetalleTurnoAuditadoDTO.builder()
                .turno(turnoDto)
                .ventas(List.of(ventaDto))
                .build();

        when(conciliacionTurnoService.obtenerDetalleTurno(5L)).thenReturn(detalleDto);

        mockMvc.perform(get("/api/reportes/turnos/5/detalle")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turno.turnoId", is(5)))
                .andExpect(jsonPath("$.ventas", hasSize(1)))
                .andExpect(jsonPath("$.ventas[0].clienteNombre", is("Ana López")));
    }

    @Test
    @DisplayName("GET /api/reportes/rotacion-atracciones debe retornar métricas de uso y demanda (HU-37.04)")
    void testObtenerRotacionAtracciones() throws Exception {
        RotacionAtraccionDTO rotacionDto = RotacionAtraccionDTO.builder()
                .atraccionId(1L)
                .nombreAtraccion("Trampolín Gigante")
                .tipoAtraccion(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(12)
                .cantidadTotalUsos(55L)
                .ingresosBrutos(new BigDecimal("350000.00"))
                .tiempoPromedioMinutos(25.0)
                .porcentajeOcupacion(72.5)
                .horaMayorAfluencia("17:00 - 18:00")
                .build();

        when(conciliacionTurnoService.obtenerRotacionAtracciones(any(), any(), any(), any()))
                .thenReturn(List.of(rotacionDto));

        mockMvc.perform(get("/api/reportes/rotacion-atracciones")
                        .param("periodo", "MENSUAL")
                        .param("criterioOrden", "USOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreAtraccion", is("Trampolín Gigante")))
                .andExpect(jsonPath("$[0].cantidadTotalUsos", is(55)))
                .andExpect(jsonPath("$[0].horaMayorAfluencia", is("17:00 - 18:00")));
    }

    @Test
    @DisplayName("GET /api/reportes/exportar/pdf debe retornar binario PDF (HU-39.05)")
    void testExportarPdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 mock".getBytes();
        when(exportacionReporteService.exportarPdf(any(), any(), any(), any(), any()))
                .thenReturn(pdfBytes);

        mockMvc.perform(get("/api/reportes/exportar/pdf")
                        .param("tipoReporte", "TURNOS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("GET /api/reportes/exportar/csv debe retornar CSV con UTF-8 BOM (HU-39.05)")
    void testExportarCsv() throws Exception {
        byte[] csvBytes = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', ';', 'b'};
        when(exportacionReporteService.exportarCsv(any(), any(), any(), any(), any()))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/reportes/exportar/csv")
                        .param("tipoReporte", "TURNOS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(content().bytes(csvBytes));
    }

    @Test
    @DisplayName("GET /api/reportes/graficos/horas-pico debe retornar dataset de afluencia (HU-40.04)")
    void testObtenerHorasPico() throws Exception {
        com.software.territoriodeninos.dto.GraficoTendenciaDTO grafico = com.software.territoriodeninos.dto.GraficoTendenciaDTO.builder()
                .titulo("Horas Pico")
                .totalAcumulado(100.0)
                .dataset(com.software.territoriodeninos.dto.DatasetGraficoDTO.builder().nombreSerie("Pico").puntos(List.of()).build())
                .build();

        when(estadisticasReporteService.obtenerHorasPico(any(), any())).thenReturn(grafico);

        mockMvc.perform(get("/api/reportes/graficos/horas-pico")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Horas Pico")))
                .andExpect(jsonPath("$.totalAcumulado", is(100.0)));
    }

    @Test
    @DisplayName("GET /api/reportes/graficos/metodos-pago debe retornar distribución de medios de pago (HU-40.04)")
    void testObtenerMetodosPago() throws Exception {
        com.software.territoriodeninos.dto.GraficoTendenciaDTO grafico = com.software.territoriodeninos.dto.GraficoTendenciaDTO.builder()
                .titulo("Distribución Métodos de Pago")
                .totalAcumulado(250000.0)
                .dataset(com.software.territoriodeninos.dto.DatasetGraficoDTO.builder().nombreSerie("Metodos").puntos(List.of()).build())
                .build();

        when(estadisticasReporteService.obtenerDistribucionMetodosPago(any(), any())).thenReturn(grafico);

        mockMvc.perform(get("/api/reportes/graficos/metodos-pago")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Distribución Métodos de Pago")))
                .andExpect(jsonPath("$.totalAcumulado", is(250000.0)));
    }

    @Test
    @DisplayName("GET /api/reportes/graficos/evolucion-ingresos debe retornar los 7 días de recaudación (HU-40.04)")
    void testObtenerEvolucionIngresos() throws Exception {
        com.software.territoriodeninos.dto.GraficoTendenciaDTO grafico = com.software.territoriodeninos.dto.GraficoTendenciaDTO.builder()
                .titulo("Evolución Ingresos")
                .totalAcumulado(500000.0)
                .dataset(com.software.territoriodeninos.dto.DatasetGraficoDTO.builder().nombreSerie("Evolucion").puntos(List.of()).build())
                .build();

        when(estadisticasReporteService.obtenerEvolucionIngresosUltimos7Dias()).thenReturn(grafico);

        mockMvc.perform(get("/api/reportes/graficos/evolucion-ingresos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Evolución Ingresos")));
    }
}
