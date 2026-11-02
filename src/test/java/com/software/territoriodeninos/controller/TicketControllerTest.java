package com.software.territoriodeninos.controller;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketControllerTest - Pruebas unitarias Web con MockMvc para tickets (HU-20.03, HU-21.03)")
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private TicketPrintService ticketPrintService;

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
}
