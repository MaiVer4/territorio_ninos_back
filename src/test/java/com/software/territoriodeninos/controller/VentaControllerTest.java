package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.service.AforoValidatorService;
import com.software.territoriodeninos.service.AtraccionService;
import com.software.territoriodeninos.service.VentaCalculoService;
import com.software.territoriodeninos.service.VentaService;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import com.software.territoriodeninos.util.TipoTarifa;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaControllerTest - Pruebas de integración Web con MockMvc para /api/ventas")
class VentaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AtraccionService atraccionService;

    @Mock
    private AforoValidatorService aforoValidatorService;

    @Mock
    private VentaCalculoService ventaCalculoService;

    @Mock
    private VentaService ventaService;

    @InjectMocks
    private VentaController ventaController;

    private CatalogoAtraccionVentaDTO mockAtraccionCatalogo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ventaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TarifaTiempoResponseDTO tarifaTiempo = new TarifaTiempoResponseDTO(
                10L, 1L, "Piscina de Pelotas", 15, new BigDecimal("12000.00"), true, LocalDateTime.now()
        );

        TarifaEventoResponseDTO tarifaEvento = new TarifaEventoResponseDTO(
                20L, 1L, "Piscina de Pelotas", 3, new BigDecimal("8000.00"), true, LocalDateTime.now()
        );

        mockAtraccionCatalogo = new CatalogoAtraccionVentaDTO(
                1L,
                "Piscina de Pelotas",
                "Área con piscina y tobogán",
                "TEMPORIZADA",
                "ACTIVO",
                new BigDecimal("1000.00"),
                15,
                5,
                3,
                20,
                5,
                15,
                true,
                List.of(tarifaTiempo),
                List.of(tarifaEvento)
        );
    }

    @Test
    @DisplayName("GET /api/ventas/catalogo - Retorna 200 OK con header Cache-Control y catálogo completo")
    void testObtenerCatalogo_Exitoso() throws Exception {
        when(atraccionService.obtenerCatalogoVenta()).thenReturn(List.of(mockAtraccionCatalogo));

        mockMvc.perform(get("/api/ventas/catalogo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=5")))
                .andExpect(header().string("Cache-Control", containsString("must-revalidate")))
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$[0].tipo", is("TEMPORIZADA")))
                .andExpect(jsonPath("$[0].estado", is("ACTIVO")))
                .andExpect(jsonPath("$[0].capacidadMaxima", is(20)))
                .andExpect(jsonPath("$[0].ocupacionActual", is(5)))
                .andExpect(jsonPath("$[0].aforoDisponible", is(15)))
                .andExpect(jsonPath("$[0].tieneCupo", is(true)))
                .andExpect(jsonPath("$[0].ninosActivos", is(5)))
                .andExpect(jsonPath("$[0].tieneOperarioAsignado", is(true)))
                .andExpect(jsonPath("$[0].tarifasTiempo", hasSize(1)))
                .andExpect(jsonPath("$[0].tarifasTiempo[0].duracionMinutos", is(15)))
                .andExpect(jsonPath("$[0].tarifasEvento", hasSize(1)))
                .andExpect(jsonPath("$[0].tarifasEvento[0].intentos", is(3)));

        verify(atraccionService).obtenerCatalogoVenta();
    }

    @Test
    @DisplayName("GET /api/ventas/catalogo - Retorna lista vacía cuando no hay atracciones activas")
    void testObtenerCatalogo_Vacio() throws Exception {
        when(atraccionService.obtenerCatalogoVenta()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ventas/catalogo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=5")))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(atraccionService).obtenerCatalogoVenta();
    }

    @Test
    @DisplayName("POST /api/ventas/validar - Retorna 200 OK con resultado oficial cuando la venta es válida")
    void testValidarVenta_Exitoso() throws Exception {
        ItemVentaValidacionDTO item = new ItemVentaValidacionDTO(
                1L, 10L, null, 2, new BigDecimal("12000.00"), new BigDecimal("24000.00"), 15, null
        );

        ResultadoValidacionVentaDTO resultadoEsperado = new ResultadoValidacionVentaDTO(
                true, new BigDecimal("24000.00"), 2, Map.of(1L, 18)
        );

        when(aforoValidatorService.validarItemsVenta(anyList())).thenReturn(resultadoEsperado);

        mockMvc.perform(post("/api/ventas/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(item))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido", is(true)))
                .andExpect(jsonPath("$.totalOficial", is(24000.00)))
                .andExpect(jsonPath("$.totalTickets", is(2)))
                .andExpect(jsonPath("$.aforoRestantePorAtraccion['1']", is(18)));

        verify(aforoValidatorService).validarItemsVenta(anyList());
    }

    @Test
    @DisplayName("POST /api/ventas/validar - Retorna 400 Bad Request cuando se detecta adulteración o aforo excedido")
    void testValidarVenta_FalloReglaNegocio() throws Exception {
        ItemVentaValidacionDTO itemAdulterado = new ItemVentaValidacionDTO(
                1L, 10L, null, 1, new BigDecimal("1000.00"), null, 15, null
        );

        when(aforoValidatorService.validarItemsVenta(anyList()))
                .thenThrow(new ReglaNegocioException("Inconsistencia en el precio de la tarifa"));

        mockMvc.perform(post("/api/ventas/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(itemAdulterado))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("Regla de negocio no cumplida")))
                .andExpect(jsonPath("$.detalles", containsString("Inconsistencia en el precio")));

        verify(aforoValidatorService).validarItemsVenta(anyList());
    }

    @Test
    @DisplayName("POST /api/ventas/cotizar - Retorna 200 OK con desglose financiero exacto")
    void testCotizarVenta_Exitoso() throws Exception {
        ItemCotizacionDTO item = new ItemCotizacionDTO(
                1L, 10L, null, "Piscina de Pelotas - 15 min", 2, new BigDecimal("15000.00"), new BigDecimal("30000.00")
        );
        CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item), null, BigDecimal.ZERO);

        CotizacionResponseDTO responseDTO = new CotizacionResponseDTO(
                List.of(new BigDecimal("30000.00")),
                List.of(item),
                new BigDecimal("30000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("19.00"),
                new BigDecimal("5700.00"),
                new BigDecimal("35700.00")
        );

        when(ventaCalculoService.cotizarVenta(org.mockito.ArgumentMatchers.any(CotizacionRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/ventas/cotizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotales", hasSize(1)))
                .andExpect(jsonPath("$.subtotales[0]", is(30000.00)))
                .andExpect(jsonPath("$.subtotal", is(30000.00)))
                .andExpect(jsonPath("$.descuento", is(0.00)))
                .andExpect(jsonPath("$.baseGravable", is(30000.00)))
                .andExpect(jsonPath("$.porcentajeImpuesto", is(19.00)))
                .andExpect(jsonPath("$.impuestos", is(5700.00)))
                .andExpect(jsonPath("$.totalNeto", is(35700.00)))
                .andExpect(jsonPath("$.total", is(35700.00)));

        verify(ventaCalculoService).cotizarVenta(org.mockito.ArgumentMatchers.any(CotizacionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/ventas/cotizar - Retorna 400 Bad Request cuando el servicio lanza ReglaNegocioException")
    void testCotizarVenta_FalloReglaNegocio() throws Exception {
        ItemCotizacionDTO item = new ItemCotizacionDTO(
                1L, 10L, null, "Piscina de Pelotas", 1, new BigDecimal("10000.00"), null
        );
        CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item), null, new BigDecimal("50000.00"));

        when(ventaCalculoService.cotizarVenta(org.mockito.ArgumentMatchers.any(CotizacionRequestDTO.class)))
                .thenThrow(new ReglaNegocioException("El descuento no puede ser superior al subtotal de la venta"));

        mockMvc.perform(post("/api/ventas/cotizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("Regla de negocio no cumplida")))
                .andExpect(jsonPath("$.detalles", containsString("El descuento no puede ser superior al subtotal")));

        verify(ventaCalculoService).cotizarVenta(org.mockito.ArgumentMatchers.any(CotizacionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/ventas - Retorna 201 Created cuando la venta se procesa exitosamente")
    void testProcesarVenta_Exitoso() throws Exception {
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                1L, 10L, TipoTarifa.TIEMPO, 1L, null, 1, new BigDecimal("15000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                50L, 2L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        VentaResponseDTO responseDTO = new VentaResponseDTO(
                100L, 50L, "Carlos Gómez", "123456", 2L,
                new BigDecimal("15000.00"), new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20000.00"), new BigDecimal("5000.00"),
                MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA, null, "Sin notas", List.of()
        );

        when(ventaService.procesarVenta(org.mockito.ArgumentMatchers.any(VentaProcesarRequestDTO.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.clienteId", is(50)))
                .andExpect(jsonPath("$.montoTotal", is(15000.00)))
                .andExpect(jsonPath("$.montoRecibido", is(20000.00)))
                .andExpect(jsonPath("$.cambio", is(5000.00)))
                .andExpect(jsonPath("$.metodoPago", is("EFECTIVO")))
                .andExpect(jsonPath("$.estadoVenta", is("COMPLETADA")));

        verify(ventaService).procesarVenta(org.mockito.ArgumentMatchers.any(VentaProcesarRequestDTO.class), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("GET /api/ventas/{id} - Retorna 200 OK con los detalles de la venta")
    void testObtenerVenta_Exitoso() throws Exception {
        VentaResponseDTO responseDTO = new VentaResponseDTO(
                100L, 50L, "Carlos Gómez", "123456", 2L,
                new BigDecimal("15000.00"), new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20000.00"), new BigDecimal("5000.00"),
                MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA, null, null, List.of()
        );

        when(ventaService.obtenerVentaPorId(100L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/ventas/100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.clienteNombre", is("Carlos Gómez")));

        verify(ventaService).obtenerVentaPorId(100L);
    }

    @Test
    @DisplayName("GET /api/ventas/turno/{turnoId} - Retorna 200 OK con lista de ventas")
    void testListarVentasPorTurno_Exitoso() throws Exception {
        VentaResponseDTO v1 = new VentaResponseDTO(
                101L, 50L, "Carlos Gómez", "123456", 2L,
                new BigDecimal("15000.00"), new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("15000.00"), BigDecimal.ZERO,
                MetodoPago.TARJETA_DEBITO, EstadoVenta.COMPLETADA, null, null, List.of()
        );

        when(ventaService.listarVentasPorTurno(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(v1));

        mockMvc.perform(get("/api/ventas/turno/2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(101)));

        verify(ventaService).listarVentasPorTurno(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any());
    }
}
