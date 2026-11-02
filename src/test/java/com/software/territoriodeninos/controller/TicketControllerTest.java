package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ReimpresionRequestDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.QrCodeService;
import com.software.territoriodeninos.service.TicketPrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketControllerTest - Pruebas unitarias Web con MockMvc para tickets (HU-20.03, HU-21.03)")
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private TicketPrintService ticketPrintService;

    @Mock
    private com.software.territoriodeninos.service.TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private UUID ticketUuid;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ticketUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid}/qr - Retorna 200 OK con Content-Type image/png y bytes de la imagen QR (HU-20.03)")
    void obtenerQrTicket_exito_retornaPng200() throws Exception {
        byte[] fakePngBytes = new byte[]{ (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 1, 2, 3 };
        when(qrCodeService.generarQrTicketPng(ticketUuid)).thenReturn(fakePngBytes);

        mockMvc.perform(get("/api/tickets/{uuid}/qr", ticketUuid))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ticket-" + ticketUuid + ".png\""))
                .andExpect(content().bytes(fakePngBytes));

        verify(qrCodeService).generarQrTicketPng(ticketUuid);
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid}/qr - Retorna 404 Not Found si el ticket no existe en BD")
    void obtenerQrTicket_ticketNoEncontrado_retorna404() throws Exception {
        when(qrCodeService.generarQrTicketPng(ticketUuid))
                .thenThrow(new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + ticketUuid));

        mockMvc.perform(get("/api/tickets/{uuid}/qr", ticketUuid))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("Recurso no encontrado")))
                .andExpect(jsonPath("$.detalles", containsString(ticketUuid.toString())));

        verify(qrCodeService).generarQrTicketPng(ticketUuid);
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid}/qr - Retorna 400 Bad Request si el UUID tiene formato inválido")
    void obtenerQrTicket_uuidInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/tickets/{uuid}/qr", "uuid-no-valido-1234"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("Tipo de parámetro inválido")));
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid}/print-payload - Retorna 200 OK con Content-Type text/plain y texto ESC/POS estructurado (HU-21.03)")
    void obtenerPrintPayload_exito_retorna200() throws Exception {
        String mockEscPosPayload = "\u001B@\u001Ba\u0001TERRITORIO DE NIÑOS\nBOLETO\n\u001DV\u0042\u0000";
        when(ticketPrintService.generarPayloadImpresion(ticketUuid)).thenReturn(mockEscPosPayload);

        mockMvc.perform(get("/api/tickets/{uuid}/print-payload", ticketUuid))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/plain")))
                .andExpect(content().string(mockEscPosPayload));

        verify(ticketPrintService).generarPayloadImpresion(ticketUuid);
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid}/print-payload - Retorna 404 Not Found si el ticket no existe")
    void obtenerPrintPayload_ticketNoEncontrado_retorna404() throws Exception {
        when(ticketPrintService.generarPayloadImpresion(ticketUuid))
                .thenThrow(new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + ticketUuid));

        mockMvc.perform(get("/api/tickets/{uuid}/print-payload", ticketUuid))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("Recurso no encontrado")));

        verify(ticketPrintService).generarPayloadImpresion(ticketUuid);
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid}/print-payload - Retorna 400 Bad Request si el UUID es inválido")
    void obtenerPrintPayload_uuidInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/tickets/{uuid}/print-payload", "invalido-1234"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("Tipo de parámetro inválido")));
    }

    @Test
    @DisplayName("POST /api/tickets/{uuid}/reimprimir - Retorna 200 OK con payload ESC/POS (HU-22.05)")
    void reimprimirTicket_exito_retorna200() throws Exception {
        String mockPayload = "\u001B@** REIMPRESIÓN **\n\u001DV\u0042\u0000";
        when(ticketPrintService.reimprimirTicket(eq(ticketUuid), any(ReimpresionRequestDTO.class), any()))
                .thenReturn(mockPayload);

        String jsonRequest = "{\"motivo\": \"Falla en corte de papel\"}";

        mockMvc.perform(post("/api/tickets/{uuid}/reimprimir", ticketUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/plain")))
                .andExpect(content().string(mockPayload));

        verify(ticketPrintService).reimprimirTicket(eq(ticketUuid), any(ReimpresionRequestDTO.class), any());
    }

    @Test
    @DisplayName("POST /api/tickets/{uuid}/reimprimir - Retorna 409 Conflict si el ticket está CONSUMIDO (HU-22.04 CA-04)")
    void reimprimirTicket_ticketConsumido_retorna409() throws Exception {
        when(ticketPrintService.reimprimirTicket(eq(ticketUuid), any(ReimpresionRequestDTO.class), any()))
                .thenThrow(new ConflictoException("No es posible reimprimir el ticket porque ya se encuentra en estado CONSUMIDO."));

        String jsonRequest = "{\"motivo\": \"Falla de papel\"}";

        mockMvc.perform(post("/api/tickets/{uuid}/reimprimir", ticketUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.detalles", containsString("ya se encuentra en estado CONSUMIDO")));
    }

    @Test
    @DisplayName("POST /api/tickets/{uuid}/reimprimir - Retorna 400 Bad Request si el motivo está vacío (HU-22.03)")
    void reimprimirTicket_motivoEnBlanco_retorna400() throws Exception {
        String jsonRequest = "{\"motivo\": \"   \"}";

        mockMvc.perform(post("/api/tickets/{uuid}/reimprimir", ticketUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Error de validación")))
                .andExpect(jsonPath("$.errors.motivo", containsString("obligatorio")));
    }

    @Test
    @DisplayName("POST /api/tickets/{uuid}/reimprimir - Retorna 404 Not Found si el ticket no existe")
    void reimprimirTicket_ticketNoEncontrado_retorna404() throws Exception {
        when(ticketPrintService.reimprimirTicket(eq(ticketUuid), any(ReimpresionRequestDTO.class), any()))
                .thenThrow(new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + ticketUuid));

        String jsonRequest = "{\"motivo\": \"Ticket borrado\"}";

        mockMvc.perform(post("/api/tickets/{uuid}/reimprimir", ticketUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.codigo", is(404)));
    }

    @Test
    @DisplayName("GET /api/tickets/buscar?criterio={cedula} - Retorna 200 OK con lista de TicketConsultaDTO (HU-23.03)")
    void buscarTickets_porCedula_retorna200ConLista() throws Exception {
        com.software.territoriodeninos.dto.TicketConsultaDTO mockDto = new com.software.territoriodeninos.dto.TicketConsultaDTO(
                ticketUuid,
                com.software.territoriodeninos.util.EstadoTicket.PAGADO,
                30,
                java.time.LocalDateTime.now(),
                10L,
                "Mateo Gómez",
                "1099887766",
                1L,
                "Laberinto Mágico",
                com.software.territoriodeninos.util.TipoTarifa.TIEMPO,
                100L,
                205L,
                5L,
                "1098765432",
                "Laura Castro"
        );

        when(ticketService.buscarTickets("1098765432")).thenReturn(java.util.List.of(mockDto));

        mockMvc.perform(get("/api/tickets/buscar").param("criterio", "1098765432"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].uuidCodigo", is(ticketUuid.toString())))
                .andExpect(jsonPath("$[0].id", is(ticketUuid.toString())))
                .andExpect(jsonPath("$[0].estadoTicket", is("PAGADO")))
                .andExpect(jsonPath("$[0].estadoActual", is("PAGADO")))
                .andExpect(jsonPath("$[0].minutosContratados", is(30)))
                .andExpect(jsonPath("$[0].menorNombre", is("Mateo Gómez")))
                .andExpect(jsonPath("$[0].atraccionNombre", is("Laberinto Mágico")))
                .andExpect(jsonPath("$[0].clienteDocumento", is("1098765432")));

        verify(ticketService).buscarTickets("1098765432");
    }

    @Test
    @DisplayName("GET /api/tickets/buscar?criterio={uuid} - Retorna 200 OK al buscar por UUID")
    void buscarTickets_porUuid_retorna200() throws Exception {
        com.software.territoriodeninos.dto.TicketConsultaDTO mockDto = new com.software.territoriodeninos.dto.TicketConsultaDTO(
                ticketUuid,
                com.software.territoriodeninos.util.EstadoTicket.PAGADO,
                15,
                java.time.LocalDateTime.now(),
                null, null, null,
                1L, "Saltarín",
                com.software.territoriodeninos.util.TipoTarifa.TIEMPO,
                100L, 205L, 5L, "1098765432", "Laura Castro"
        );

        when(ticketService.buscarTickets(ticketUuid.toString())).thenReturn(java.util.List.of(mockDto));

        mockMvc.perform(get("/api/tickets/buscar").param("criterio", ticketUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].uuidCodigo", is(ticketUuid.toString())));

        verify(ticketService).buscarTickets(ticketUuid.toString());
    }

    @Test
    @DisplayName("GET /api/tickets/buscar - Retorna 400 Bad Request si el criterio es nulo o vacío")
    void buscarTickets_sinCriterio_retorna400() throws Exception {
        when(ticketService.buscarTickets(null))
                .thenThrow(new com.software.territoriodeninos.exception.ReglaNegocioException("El criterio de búsqueda es obligatorio"));

        mockMvc.perform(get("/api/tickets/buscar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("Regla de negocio no cumplida")));
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid} - Retorna 200 OK con TicketConsultaDTO")
    void obtenerTicketPorUuid_exito_retorna200() throws Exception {
        com.software.territoriodeninos.dto.TicketConsultaDTO mockDto = new com.software.territoriodeninos.dto.TicketConsultaDTO(
                ticketUuid,
                com.software.territoriodeninos.util.EstadoTicket.PAGADO,
                45,
                java.time.LocalDateTime.now(),
                null, null, null,
                1L, "Carrusel",
                com.software.territoriodeninos.util.TipoTarifa.TIEMPO,
                100L, 205L, 5L, "1098765432", "Laura Castro"
        );

        when(ticketService.obtenerTicketPorUuid(ticketUuid)).thenReturn(mockDto);

        mockMvc.perform(get("/api/tickets/{uuid}", ticketUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ticketUuid.toString())))
                .andExpect(jsonPath("$.estadoActual", is("PAGADO")))
                .andExpect(jsonPath("$.minutosContratados", is(45)));

        verify(ticketService).obtenerTicketPorUuid(ticketUuid);
    }

    @Test
    @DisplayName("GET /api/tickets/{uuid} - Retorna 404 Not Found si no existe")
    void obtenerTicketPorUuid_noExiste_retorna404() throws Exception {
        when(ticketService.obtenerTicketPorUuid(ticketUuid))
                .thenThrow(new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + ticketUuid));

        mockMvc.perform(get("/api/tickets/{uuid}", ticketUuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)));

        verify(ticketService).obtenerTicketPorUuid(ticketUuid);
    }
}
