package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.exception.TurnoConflictoException;
import com.software.territoriodeninos.service.TurnoCajaService;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoCajaControllerTest - Pruebas de integración Web con MockMvc")
class TurnoCajaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TurnoCajaService turnoCajaService;

    @InjectMocks
    private TurnoCajaController turnoCajaController;

    private TurnoCajaResponseDTO mockTurnoAbiertoResponse;
    private TurnoCajaResponseDTO mockTurnoCerradoResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(turnoCajaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockTurnoAbiertoResponse = new TurnoCajaResponseDTO(
                1L,
                10L,
                new BigDecimal("50000.00"),
                LocalDateTime.now(),
                null,
                BigDecimal.ZERO,
                null,
                null,
                EstadoTurnoCaja.ABIERTO,
                "Apertura regular",
                "Cajero Pedro"
        );

        mockTurnoCerradoResponse = new TurnoCajaResponseDTO(
                1L,
                10L,
                new BigDecimal("50000.00"),
                LocalDateTime.now().minusHours(4),
                LocalDateTime.now(),
                new BigDecimal("100000.00"),
                new BigDecimal("150000.00"),
                BigDecimal.ZERO,
                EstadoTurnoCaja.CERRADO,
                "Cierre exacto",
                "Cajero Pedro"
        );
    }

    @Nested
    @DisplayName("POST /api/turnos/abrir")
    class AbrirTurnoTests {

        @Test
        @DisplayName("201 Created - Apertura exitosa de turno con formato DTO estándar")
        void testAbrirTurno_Exito_Created() throws Exception {
            AperturaCajaDTO request = new AperturaCajaDTO(new BigDecimal("50000.00"), "Apertura regular");
            when(turnoCajaService.abrirTurno(any(AperturaCajaDTO.class), any())).thenReturn(mockTurnoAbiertoResponse);

            mockMvc.perform(post("/api/turnos/abrir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.idTurno", is(1)))
                    .andExpect(jsonPath("$.usuarioId", is(10)))
                    .andExpect(jsonPath("$.montoBase", is(50000.00)))
                    .andExpect(jsonPath("$.montoInicial", is(50000.00)))
                    .andExpect(jsonPath("$.estado", is("ABIERTO")))
                    .andExpect(jsonPath("$.observaciones", is("Apertura regular")));
        }

        @Test
        @DisplayName("201 Created - Compatibilidad con frontend Angular (payload montoInicial e idCaja)")
        void testAbrirTurno_CompatibilidadFrontend_Created() throws Exception {
            String jsonFrontend = "{\"montoInicial\": 75000.00, \"idCaja\": 2, \"observaciones\": \"Apertura desde POS\"}";
            TurnoCajaResponseDTO responseDto = new TurnoCajaResponseDTO(
                    2L, 10L, new BigDecimal("75000.00"), LocalDateTime.now(), null,
                    BigDecimal.ZERO, null, null, EstadoTurnoCaja.ABIERTO, "Apertura desde POS", "Cajero Pedro"
            );
            when(turnoCajaService.abrirTurno(any(AperturaCajaDTO.class), any())).thenReturn(responseDto);

            mockMvc.perform(post("/api/turnos/abrir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonFrontend))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idTurno", is(2)))
                    .andExpect(jsonPath("$.montoInicial", is(75000.00)))
                    .andExpect(jsonPath("$.cajeroNombre", is("Cajero Pedro")))
                    .andExpect(jsonPath("$.estado", is("ABIERTO")));
        }

        @Test
        @DisplayName("400 Bad Request - montoBase nulo")
        void testAbrirTurno_MontoBaseNulo_BadRequest() throws Exception {
            String jsonSinMonto = "{\"montoBase\": null, \"observaciones\": \"Sin monto\"}";

            mockMvc.perform(post("/api/turnos/abrir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonSinMonto))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.montoBase").exists());
        }

        @Test
        @DisplayName("400 Bad Request - montoBase negativo")
        void testAbrirTurno_MontoBaseNegativo_BadRequest() throws Exception {
            AperturaCajaDTO request = new AperturaCajaDTO(new BigDecimal("-100.00"), "Monto negativo");

            mockMvc.perform(post("/api/turnos/abrir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.montoBase").exists());
        }

        @Test
        @DisplayName("409 Conflict - Ya existe un turno ABIERTO para el cajero")
        void testAbrirTurno_TurnoYaAbierto_Conflict() throws Exception {
            AperturaCajaDTO request = new AperturaCajaDTO(new BigDecimal("50000.00"), "Intento duplicado");
            when(turnoCajaService.abrirTurno(any(AperturaCajaDTO.class), any()))
                    .thenThrow(new TurnoConflictoException("El usuario ya tiene un turno de caja abierto en simultáneo."));

            mockMvc.perform(post("/api/turnos/abrir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("El usuario ya tiene un turno de caja abierto en simultáneo.")));
        }

        @Test
        @DisplayName("403 Forbidden - Usuario sin rol autorizado")
        void testAbrirTurno_RolNoAutorizado_Forbidden() throws Exception {
            AperturaCajaDTO request = new AperturaCajaDTO(new BigDecimal("50000.00"), "Sin rol");
            when(turnoCajaService.abrirTurno(any(AperturaCajaDTO.class), any()))
                    .thenThrow(new AccessDeniedException("El usuario no tiene permisos de cajero (requiere rol TAQUILLERO o ADMINISTRADOR)"));

            mockMvc.perform(post("/api/turnos/abrir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("GET /api/turnos/activo")
    class ObtenerTurnoActivoTests {

        @Test
        @DisplayName("200 OK - Turno activo encontrado")
        void testObtenerTurnoActivo_Existe_Ok() throws Exception {
            when(turnoCajaService.obtenerTurnoActivo(any())).thenReturn(Optional.of(mockTurnoAbiertoResponse));

            mockMvc.perform(get("/api/turnos/activo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.usuarioId", is(10)))
                    .andExpect(jsonPath("$.estado", is("ABIERTO")));
        }

        @Test
        @DisplayName("204 No Content - No hay turno activo")
        void testObtenerTurnoActivo_NoExiste_NoContent() throws Exception {
            when(turnoCajaService.obtenerTurnoActivo(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/turnos/activo"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("403 Forbidden - Usuario sin rol autorizado")
        void testObtenerTurnoActivo_RolNoAutorizado_Forbidden() throws Exception {
            when(turnoCajaService.obtenerTurnoActivo(any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(get("/api/turnos/activo"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("PUT y POST /api/turnos/cerrar")
    class CerrarTurnoTests {

        @Test
        @DisplayName("200 OK - Cierre exitoso vía PUT (Notion CA-02 y CA-04)")
        void testCerrarTurno_Put_Exitoso() throws Exception {
            CierreCajaDTO request = new CierreCajaDTO(new BigDecimal("150000.00"), "Cierre sin novedades");
            when(turnoCajaService.cerrarTurno(any(CierreCajaDTO.class), any())).thenReturn(mockTurnoCerradoResponse);

            mockMvc.perform(put("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idTurno", is(1)))
                    .andExpect(jsonPath("$.estado", is("CERRADO")))
                    .andExpect(jsonPath("$.montoEfectivoReportado", is(150000.00)))
                    .andExpect(jsonPath("$.montoFinal", is(150000.00)))
                    .andExpect(jsonPath("$.totalEsperado", is(150000.00)))
                    .andExpect(jsonPath("$.diferencia", is(0)));
        }

        @Test
        @DisplayName("200 OK - Compatibilidad con cliente Web vía POST /api/turnos/cerrar")
        void testCerrarTurno_Post_CompatibilidadFrontend() throws Exception {
            String jsonFrontend = "{\"montoFinal\": 150000.00, \"observaciones\": \"Cierre desde frontend\"}";
            when(turnoCajaService.cerrarTurno(any(CierreCajaDTO.class), any())).thenReturn(mockTurnoCerradoResponse);

            mockMvc.perform(post("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonFrontend))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado", is("CERRADO")));
        }

        @Test
        @DisplayName("400 Bad Request - ReglaNegocioException por descuadre sin justificación (Notion CA-04)")
        void testCerrarTurno_DescuadreSinObservaciones_BadRequest() throws Exception {
            CierreCajaDTO request = new CierreCajaDTO(new BigDecimal("140000.00"), null);
            when(turnoCajaService.cerrarTurno(any(CierreCajaDTO.class), any()))
                    .thenThrow(new ReglaNegocioException("Existe una diferencia de caja (faltante de 10000.00). Es obligatorio ingresar una justificación en las observaciones."));

            mockMvc.perform(put("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is(400)))
                    .andExpect(jsonPath("$.detalles", is("Existe una diferencia de caja (faltante de 10000.00). Es obligatorio ingresar una justificación en las observaciones.")));
        }

        @Test
        @DisplayName("400 Bad Request - montoEfectivoReportado nulo")
        void testCerrarTurno_MontoNulo_BadRequest() throws Exception {
            String jsonSinMonto = "{\"montoEfectivoReportado\": null}";

            mockMvc.perform(put("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonSinMonto))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.montoEfectivoReportado").exists());
        }

        @Test
        @DisplayName("400 Bad Request - montoEfectivoReportado negativo")
        void testCerrarTurno_MontoNegativo_BadRequest() throws Exception {
            CierreCajaDTO request = new CierreCajaDTO(new BigDecimal("-50.00"), "Negativo");

            mockMvc.perform(put("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.montoEfectivoReportado").exists());
        }

        @Test
        @DisplayName("404 Not Found - No existe turno activo para cerrar")
        void testCerrarTurno_SinTurnoActivo_NotFound() throws Exception {
            CierreCajaDTO request = new CierreCajaDTO(new BigDecimal("150000.00"), "Cierre");
            when(turnoCajaService.cerrarTurno(any(CierreCajaDTO.class), any()))
                    .thenThrow(new RecursoNoEncontradoException("No se encontró ningún turno de caja abierto para el usuario en sesión."));

            mockMvc.perform(put("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Usuario no autorizado al cerrar")
        void testCerrarTurno_NoAutorizado_Forbidden() throws Exception {
            CierreCajaDTO request = new CierreCajaDTO(new BigDecimal("150000.00"), "Cierre");
            when(turnoCajaService.cerrarTurno(any(CierreCajaDTO.class), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(put("/api/turnos/cerrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("GET /api/turnos")
    class ObtenerHistorialTurnosTests {

        @Test
        @DisplayName("200 OK - Obtener historial completo sin filtro de fecha")
        void testObtenerHistorialTurnos_SinFecha_Ok() throws Exception {
            when(turnoCajaService.obtenerHistorialTurnos(null)).thenReturn(List.of(mockTurnoAbiertoResponse, mockTurnoCerradoResponse));

            mockMvc.perform(get("/api/turnos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].idUsuario", is(10)))
                    .andExpect(jsonPath("$[0].nombreCajero", is("Cajero Pedro")))
                    .andExpect(jsonPath("$[0].totalVentas", is(0)));
        }

        @Test
        @DisplayName("200 OK - Obtener historial filtrado por fecha con alias de frontend")
        void testObtenerHistorialTurnos_ConFecha_Ok() throws Exception {
            when(turnoCajaService.obtenerHistorialTurnos("2026-10-22")).thenReturn(List.of(mockTurnoCerradoResponse));

            mockMvc.perform(get("/api/turnos").param("fecha", "2026-10-22"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(1)))
                    .andExpect(jsonPath("$[0].idTurno", is(1)))
                    .andExpect(jsonPath("$[0].idUsuario", is(10)))
                    .andExpect(jsonPath("$[0].nombreCajero", is("Cajero Pedro")))
                    .andExpect(jsonPath("$[0].totalVentas", is(100000.00)))
                    .andExpect(jsonPath("$[0].totalEsperado", is(150000.00)));
        }
    }
}
