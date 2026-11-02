package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.exception.*;
import com.software.territoriodeninos.service.OperacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperacionControllerTest - Pruebas MockMvc para escaneo QR y control de aforo (HU-25.02, HU-34.03)")
class OperacionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OperacionService operacionService;

    @InjectMocks
    private OperacionController operacionController;

    private UUID sampleUuid;
    private ValidacionTicketResponseDTO sampleResponse;
    private AforoActualDTO sampleAforo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(operacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleUuid = UUID.randomUUID();

        sampleResponse = new ValidacionTicketResponseDTO(
                true,
                "Ticket validado exitosamente para ingreso a Piscina de Pelotas",
                sampleUuid,
                "PAGADO",
                1L,
                "Piscina de Pelotas",
                "TEMPORIZADA",
                10L,
                "Sofia Restrepo",
                5,
                "Valentina Restrepo",
                "1094988776",
                "3127894561",
                "TIEMPO",
                30,
                null,
                15,
                5,
                3,
                LocalDateTime.now().minusMinutes(10)
        );

        sampleAforo = new AforoActualDTO(
                1L,
                "Piscina de Pelotas",
                20,
                15,
                5,
                75.0,
                false,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 200 OK cuando el ticket es válido (HU-25.02)")
    void escanearTicket_exito_retorna200() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valido", is(true)))
                .andExpect(jsonPath("$.uuidTicket", is(sampleUuid.toString())))
                .andExpect(jsonPath("$.atraccionId", is(1)))
                .andExpect(jsonPath("$.atraccionNombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$.menorNombre", is("Sofia Restrepo")))
                .andExpect(jsonPath("$.menorEdad", is(5)))
                .andExpect(jsonPath("$.minutosAutorizados", is(30)));

        verify(operacionService).escanearTicket(any(EscaneoQrRequestDTO.class), any(), any());
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 400 Bad Request cuando faltan campos obligatorios")
    void escanearTicket_camposFaltantes_retorna400() throws Exception {
        // Enviar JSON sin UUID ni atraccionId
        String jsonInvalido = "{\"timestamp\": \"2026-11-02T15:30:00\"}";

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Error de validación")));
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 403 Forbidden si el operario no tiene asignada la atracción (HU-24.04)")
    void escanearTicket_estacionNoAutorizada_retorna403() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 2L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenThrow(new EstacionNoAutorizadaException("No está autorizado para operar en atracción distinta a su estación"));

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is(403)))
                .andExpect(jsonPath("$.mensaje", is("Estación física no autorizada")));
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 404 Not Found si el ticket o atracción no existe")
    void escanearTicket_noEncontrado_retorna404() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenThrow(new RecursoNoEncontradoException("Ticket no encontrado con el código UUID: " + sampleUuid));

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("Recurso no encontrado")));
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 409 Conflict si el aforo está al 100% (HU-34.02)")
    void escanearTicket_aforoLleno_retorna409() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenThrow(new AforoCompletoException("La atracción ha alcanzado el 100% de su capacidad máxima"));

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", is("Aforo completo en la atracción")));
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 409 Conflict si el ticket está CONSUMIDO o no está PAGADO (HU-26.03)")
    void escanearTicket_ticketConsumido_retorna409() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenThrow(new TicketInvalidoOperacionException("El ticket no se encuentra en estado PAGADO", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", is("Estado inválido del ticket")));
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 422 Unprocessable Entity si la atracción no coincide (HU-26.04)")
    void escanearTicket_atraccionDistinta_retorna422() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenThrow(new TicketInvalidoOperacionException("El ticket fue adquirido para otra atracción", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo", is(422)))
                .andExpect(jsonPath("$.mensaje", is("Boleto incompatible o no procesable")));
    }

    @Test
    @DisplayName("POST /api/operacion/escanear - Retorna 422 Unprocessable Entity si el menor viola el rango de edad (HU-26.05)")
    void escanearTicket_edadInvalida_retorna422() throws Exception {
        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        when(operacionService.escanearTicket(any(EscaneoQrRequestDTO.class), any(), any()))
                .thenThrow(new TicketInvalidoOperacionException("El menor no cumple con la edad mínima requerida", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(post("/api/operacion/escanear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo", is(422)))
                .andExpect(jsonPath("$.mensaje", is("Boleto incompatible o no procesable")));
    }

    @Test
    @DisplayName("GET /api/operacion/atraccion/{id}/aforo-actual - Retorna 200 OK con métricas de aforo (HU-34.03)")
    void obtenerAforoActual_exito_retorna200() throws Exception {
        when(operacionService.obtenerAforoActual(1L)).thenReturn(sampleAforo);

        mockMvc.perform(get("/api/operacion/atraccion/{id}/aforo-actual", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.atraccionId", is(1)))
                .andExpect(jsonPath("$.atraccionNombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$.capacidadMaxima", is(20)))
                .andExpect(jsonPath("$.ocupacionActiva", is(15)))
                .andExpect(jsonPath("$.cuposLibres", is(5)))
                .andExpect(jsonPath("$.porcentajeOcupacion", is(75.0)))
                .andExpect(jsonPath("$.aforoCompleto", is(false)));

        verify(operacionService).obtenerAforoActual(1L);
    }

    @Test
    @DisplayName("GET /api/operacion/atraccion/{id}/aforo-actual - Retorna 404 si atracción no existe")
    void obtenerAforoActual_atraccionNoExiste_retorna404() throws Exception {
        when(operacionService.obtenerAforoActual(999L))
                .thenThrow(new RecursoNoEncontradoException("Atracción no encontrada con ID: 999"));

        mockMvc.perform(get("/api/operacion/atraccion/{id}/aforo-actual", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("Recurso no encontrado")));

        verify(operacionService).obtenerAforoActual(999L);
    }

    // =========================================================
    // Pruebas Fase 2: Inicio, Cola Activa, Pausas y Reanudación
    // =========================================================

    @Test
    @DisplayName("POST /api/operacion/iniciar-sesion - Retorna 200 OK al iniciar sesión de juego (HU-27.05)")
    void iniciarSesion_exito_retorna200() throws Exception {
        InicioSesionRequestDTO request = new InicioSesionRequestDTO(sampleUuid, 1L, LocalDateTime.now());

        SesionJuegoResponseDTO sesionDTO = new SesionJuegoResponseDTO(
                100L,
                sampleUuid,
                1L,
                "Piscina de Pelotas",
                10L,
                "Sofia Restrepo",
                5,
                "Valentina Restrepo",
                "3127894561",
                "1094988776",
                "Carlos Operario",
                "carlos@territoriodeninos.com",
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15),
                900,
                900,
                null,
                null,
                "EN_CURSO",
                0,
                null,
                null,
                false,
                false
        );

        when(operacionService.iniciarSesion(any(InicioSesionRequestDTO.class), any()))
                .thenReturn(sesionDTO);

        mockMvc.perform(post("/api/operacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sesionId", is(100)))
                .andExpect(jsonPath("$.ticketUuid", is(sampleUuid.toString())))
                .andExpect(jsonPath("$.estadoSesion", is("EN_CURSO")))
                .andExpect(jsonPath("$.duracionSegundos", is(900)))
                .andExpect(jsonPath("$.segundosRestantes", is(900)));

        verify(operacionService).iniciarSesion(any(InicioSesionRequestDTO.class), any());
    }

    @Test
    @DisplayName("GET /api/operacion/cola/{atraccionId} - Retorna 200 OK con cola activa ordenada (HU-28.02)")
    void obtenerCola_exito_retorna200() throws Exception {
        ColaAtraccionResponseDTO colaDTO = new ColaAtraccionResponseDTO(
                1L,
                "Piscina de Pelotas",
                20,
                1,
                19,
                java.util.List.of(),
                LocalDateTime.now()
        );

        when(operacionService.obtenerCola(1L)).thenReturn(colaDTO);

        mockMvc.perform(get("/api/operacion/cola/{atraccionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.atraccionId", is(1)))
                .andExpect(jsonPath("$.atraccionNombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$.capacidadMaxima", is(20)))
                .andExpect(jsonPath("$.ocupacionActiva", is(1)))
                .andExpect(jsonPath("$.cuposDisponibles", is(19)));

        verify(operacionService).obtenerCola(1L);
    }

    @Test
    @DisplayName("POST /api/operacion/sesiones/{id}/pausar - Retorna 200 OK con sesión pausada (HU-29.03)")
    void pausarSesion_exito_retorna200() throws Exception {
        PausaRequestDTO request = new PausaRequestDTO("Menor requiere ir al baño");

        SesionJuegoResponseDTO sesionPausada = new SesionJuegoResponseDTO(
                100L,
                sampleUuid,
                1L,
                "Piscina de Pelotas",
                10L,
                "Sofia Restrepo",
                5,
                "Valentina Restrepo",
                "3127894561",
                "1094988776",
                "Carlos Operario",
                "carlos@territoriodeninos.com",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(10),
                900,
                600,
                600,
                LocalDateTime.now(),
                "PAUSADA",
                0,
                null,
                null,
                false,
                false
        );

        when(operacionService.pausarSesion(eq(100L), any(PausaRequestDTO.class), any()))
                .thenReturn(sesionPausada);

        mockMvc.perform(post("/api/operacion/sesiones/{id}/pausar", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId", is(100)))
                .andExpect(jsonPath("$.estadoSesion", is("PAUSADA")))
                .andExpect(jsonPath("$.tiempoRestantePausa", is(600)));

        verify(operacionService).pausarSesion(eq(100L), any(PausaRequestDTO.class), any());
    }

    @Test
    @DisplayName("POST /api/operacion/sesiones/{id}/pausar - Retorna 400 Bad Request si motivo está en blanco")
    void pausarSesion_motivoEnBlanco_retorna400() throws Exception {
        PausaRequestDTO requestInvalido = new PausaRequestDTO("   ");

        mockMvc.perform(post("/api/operacion/sesiones/{id}/pausar", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Error de validación")));
    }

    @Test
    @DisplayName("POST /api/operacion/sesiones/{id}/reanudar - Retorna 200 OK con sesión reanudada (HU-29.04)")
    void reanudarSesion_exito_retorna200() throws Exception {
        SesionJuegoResponseDTO sesionReanudada = new SesionJuegoResponseDTO(
                100L,
                sampleUuid,
                1L,
                "Piscina de Pelotas",
                10L,
                "Sofia Restrepo",
                5,
                "Valentina Restrepo",
                "3127894561",
                "1094988776",
                "Carlos Operario",
                "carlos@territoriodeninos.com",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusSeconds(600),
                900,
                600,
                null,
                null,
                "EN_CURSO",
                0,
                null,
                null,
                false,
                false
        );

        when(operacionService.reanudarSesion(eq(100L), any()))
                .thenReturn(sesionReanudada);

        mockMvc.perform(post("/api/operacion/sesiones/{id}/reanudar", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId", is(100)))
                .andExpect(jsonPath("$.estadoSesion", is("EN_CURSO")))
                .andExpect(jsonPath("$.segundosRestantes", is(600)));

        verify(operacionService).reanudarSesion(eq(100L), any());
    }

    @Test
    @DisplayName("POST /api/operacion/atraccion/{id}/pausa-general - Retorna 200 OK con cola pausada (HU-29.05)")
    void pausaGeneral_exito_retorna200() throws Exception {
        PausaGeneralRequestDTO request = new PausaGeneralRequestDTO("Falla técnica de mantenimiento");

        ColaAtraccionResponseDTO colaDTO = new ColaAtraccionResponseDTO(
                1L,
                "Piscina de Pelotas",
                20,
                2,
                18,
                java.util.List.of(),
                LocalDateTime.now()
        );

        when(operacionService.pausaGeneral(eq(1L), any(PausaGeneralRequestDTO.class), any()))
                .thenReturn(colaDTO);

        mockMvc.perform(post("/api/operacion/atraccion/{id}/pausa-general", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atraccionId", is(1)))
                .andExpect(jsonPath("$.capacidadMaxima", is(20)));

        verify(operacionService).pausaGeneral(eq(1L), any(PausaGeneralRequestDTO.class), any());
    }

    // =========================================================
    // Pruebas Fase 3: Egreso Físico, Descuento Intentos y Sincronización Epoch
    // =========================================================

    @Test
    @DisplayName("POST /api/operacion/sesiones/{id}/salida - Retorna 200 OK formalizando egreso y liberando aforo (HU-32.02)")
    void registrarSalida_exito_retorna200() throws Exception {
        SesionJuegoResponseDTO salidaDTO = new SesionJuegoResponseDTO(
                100L,
                sampleUuid,
                1L,
                "Piscina de Pelotas",
                10L,
                "Sofia Restrepo",
                5,
                "Valentina Restrepo",
                "3127894561",
                "1094988776",
                "Carlos Operario",
                "carlos@territoriodeninos.com",
                LocalDateTime.now().minusMinutes(15),
                LocalDateTime.now(),
                900,
                0,
                null,
                null,
                "FINALIZADA",
                0,
                null,
                null,
                false,
                true
        );

        when(operacionService.registrarSalida(eq(100L), any()))
                .thenReturn(salidaDTO);

        mockMvc.perform(post("/api/operacion/sesiones/{id}/salida", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId", is(100)))
                .andExpect(jsonPath("$.estadoSesion", is("FINALIZADA")));

        verify(operacionService).registrarSalida(eq(100L), any());
    }

    @Test
    @DisplayName("POST /api/operacion/sesiones/{id}/descontar-intento - Retorna 200 OK con intento descontado (HU-33.03)")
    void descontarIntento_exito_retorna200() throws Exception {
        RegistroIntentoEventoDTO request = new RegistroIntentoEventoDTO(1, 25, "Primera caída");

        SesionJuegoResponseDTO sesionDTO = new SesionJuegoResponseDTO(
                100L,
                sampleUuid,
                2L,
                "Toro Mecánico",
                10L,
                "Mateo Gómez",
                8,
                "Laura Castro",
                "3101234567",
                "1098765432",
                "Carlos Operario",
                "carlos@territoriodeninos.com",
                LocalDateTime.now().minusMinutes(2),
                null,
                0,
                0,
                null,
                null,
                "EN_CURSO",
                0,
                3,
                2,
                false,
                false
        );

        when(operacionService.descontarIntento(eq(100L), any(), any()))
                .thenReturn(sesionDTO);

        mockMvc.perform(post("/api/operacion/sesiones/{id}/descontar-intento", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId", is(100)))
                .andExpect(jsonPath("$.intentosRestantes", is(2)));

        verify(operacionService).descontarIntento(eq(100L), any(), any());
    }

    @Test
    @DisplayName("GET /api/operacion/sincronizar/{atraccionId} - Retorna 200 OK con epoch de servidor (HU-35.01)")
    void sincronizarReloj_exito_retorna200() throws Exception {
        ColaAtraccionResponseDTO colaDTO = new ColaAtraccionResponseDTO(
                1L,
                "Piscina de Pelotas",
                20,
                5,
                15,
                java.util.List.of(),
                LocalDateTime.now()
        );

        SincronizacionRelojResponseDTO syncDTO = new SincronizacionRelojResponseDTO(
                1L,
                "Piscina de Pelotas",
                System.currentTimeMillis(),
                LocalDateTime.now(),
                colaDTO
        );

        when(operacionService.sincronizarReloj(1L)).thenReturn(syncDTO);

        mockMvc.perform(get("/api/operacion/sincronizar/{atraccionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atraccionId", is(1)))
                .andExpect(jsonPath("$.atraccionNombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$.serverEpochMillis").isNumber())
                .andExpect(jsonPath("$.colaActualizada").exists());

        verify(operacionService).sincronizarReloj(1L);
    }
}
