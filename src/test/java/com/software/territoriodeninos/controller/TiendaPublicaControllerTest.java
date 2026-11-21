package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.tienda.*;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.service.TiendaService;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.MetodoPago;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TiendaPublicaControllerTest - Pruebas Web para el portal público de compra en línea")
class TiendaPublicaControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TiendaService tiendaService;

    @InjectMocks
    private TiendaPublicaController tiendaPublicaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tiendaPublicaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/tienda/catalogo - Retorna catálogo público de atracciones")
    void obtenerCatalogo_Retorna200() throws Exception {
        CatalogoAtraccionOnlineDTO atr = new CatalogoAtraccionOnlineDTO(
                1L, "Piscina de Pelotas", "Diversión segura", "img.jpg",
                TipoAtraccion.TEMPORIZADA, EstadoAtraccion.ACTIVO, 20, 15, 3, 12, true, List.of(), List.of()
        );
        when(tiendaService.obtenerCatalogoPublico()).thenReturn(List.of(atr));

        mockMvc.perform(get("/api/tienda/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$[0].disponibleParaCompra", is(true)));
    }

    @Test
    @DisplayName("POST /api/tienda/comprar - Procesa compra online exitosa")
    void comprarBoletos_Exito_Retorna201() throws Exception {
        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, null, null, "Lucas", 6, 1);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(item), null
        );

        UUID uuid = UUID.randomUUID();
        TicketOnlineDTO ticketDTO = new TicketOnlineDTO(
                uuid, 1L, "Piscina de Pelotas", "Lucas", 6,
                "TIEMPO", "15 min", BigDecimal.valueOf(15000),
                EstadoTicket.PAGADO, LocalDateTime.now(), uuid.toString()
        );

        CompraOnlineResponseDTO resp = new CompraOnlineResponseDTO(
                50L, "ORD-12345-ABCD", "1098765432", "Carlos Mendoza",
                "carlos@gmail.com", BigDecimal.valueOf(15000), BigDecimal.ZERO,
                BigDecimal.valueOf(15000), MetodoPago.NEQUI, LocalDateTime.now(), List.of(ticketDTO)
        );

        when(tiendaService.procesarCompraOnline(any(CompraOnlineRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/tienda/comprar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoOrden", is("ORD-12345-ABCD")))
                .andExpect(jsonPath("$.tickets", hasSize(1)))
                .andExpect(jsonPath("$.tickets[0].menorNombre", is("Lucas")));
    }

    @Test
    @DisplayName("POST /api/tienda/comprar - Falla si el honeypot viene diligenciado")
    void comprarBoletos_HoneypotDetectado_Retorna400() throws Exception {
        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, null, null, "Lucas", 6, 1);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(item), "http://spam-bot.com"
        );

        when(tiendaService.procesarCompraOnline(any(CompraOnlineRequestDTO.class)))
                .thenThrow(new ReglaNegocioException("La solicitud no pudo ser procesada."));

        mockMvc.perform(post("/api/tienda/comprar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", is("La solicitud no pudo ser procesada.")));
    }

    @Test
    @DisplayName("GET /api/tienda/mis-boletos - Consulta boletos activos por documento")
    void consultarBoletos_Exito_Retorna200() throws Exception {
        UUID uuid = UUID.randomUUID();
        TicketOnlineDTO ticketDTO = new TicketOnlineDTO(
                uuid, 1L, "Piscina de Pelotas", "Lucas", 6,
                "TIEMPO", "15 min", BigDecimal.valueOf(15000),
                EstadoTicket.PAGADO, LocalDateTime.now(), uuid.toString()
        );

        when(tiendaService.consultarBoletosActivos(eq("1098765432"), any()))
                .thenReturn(List.of(ticketDTO));

        mockMvc.perform(get("/api/tienda/mis-boletos")
                        .param("documento", "1098765432")
                        .param("correo", "carlos@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuidCodigo", is(uuid.toString())));
    }
}
