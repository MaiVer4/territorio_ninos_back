package com.software.territoriodeninos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.software.territoriodeninos.controller.TicketController;
import com.software.territoriodeninos.controller.VentaController;
import com.software.territoriodeninos.dto.DetalleVentaRequestDTO;
import com.software.territoriodeninos.dto.VentaProcesarRequestDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TicketCicloVidaIntegrationTest - Pruebas completas del ciclo de emisión, consulta y reimpresión (HU-23.02, HU-23.03, HU-20.03)")
class TicketCicloVidaIntegrationTest {

    @Autowired
    private VentaController ventaController;

    @Autowired
    private TicketController ticketController;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MenorRepository menorRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    @Autowired
    private AtraccionRepository atraccionRepository;

    @Autowired
    private TarifaTiempoRepository tarifaTiempoRepository;

    @Autowired
    private ConfiguracionParqueRepository configuracionParqueRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Cliente cliente;
    private Menor menor;
    private TurnoCaja turnoCaja;
    private Atraccion atraccion;
    private TarifaTiempo tarifaTiempo;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "taquillero@territoriodeninos.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc = MockMvcBuilders.standaloneSetup(ventaController, ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        cliente = Cliente.builder()
                .numeroDocumento("CC" + System.currentTimeMillis())
                .nombres("Mauricio")
                .apellidos("Zapata")
                .telefono("3155556677")
                .correo("mauricio" + System.currentTimeMillis() + "@test.com")
                .build();
        cliente = clienteRepository.save(cliente);

        menor = Menor.builder()
                .numeroDocumento("TI" + System.currentTimeMillis())
                .nombreCompleto("Camilo Zapata")
                .fechaNacimiento(LocalDate.now().minusYears(7))
                .cliente(cliente)
                .build();
        menor = menorRepository.save(menor);

        turnoCaja = TurnoCaja.builder()
                .usuarioId(1L)
                .montoBase(new BigDecimal("200000.00"))
                .ventasEfectivoSistema(BigDecimal.ZERO)
                .fechaApertura(LocalDateTime.now())
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        turnoCaja = turnoCajaRepository.save(turnoCaja);

        atraccion = Atraccion.builder()
                .nombre("Mundo Ninja " + System.currentTimeMillis())
                .tarifaMinuto(new BigDecimal("1000.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .tipo(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(10)
                .build();
        atraccion = atraccionRepository.save(atraccion);

        tarifaTiempo = TarifaTiempo.builder()
                .atraccion(atraccion)
                .duracionMinutos(30)
                .precio(new BigDecimal("20000.00"))
                .activo(true)
                .build();
        tarifaTiempo = tarifaTiempoRepository.save(tarifaTiempo);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Ciclo completo E2E: 1. Emisión de venta con boletos -> 2. Búsqueda por cédula/UUID (< 500ms) -> 3. Reimpresión de imagen QR")
    void testCicloCompleto_EmisionConsultaYReimpresion() throws Exception {
        // =========================================================================
        // FASE 1: EMISIÓN DE VENTA Y TICKET EN TAQUILLA (HU-19.04)
        // =========================================================================
        DetalleVentaRequestDTO item = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                1,
                new BigDecimal("20000.00"),
                new BigDecimal("20000.00")
        );

        VentaProcesarRequestDTO ventaRequest = new VentaProcesarRequestDTO(
                cliente.getId(),
                turnoCaja.getId(),
                MetodoPago.EFECTIVO,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                "Venta de ciclo completo",
                List.of(item)
        );

        MvcResult resultadoVenta = mockMvc.perform(post("/api/ventas/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.estadoVenta", is("PAGADO")))
                .andExpect(jsonPath("$.tickets", hasSize(1)))
                .andExpect(jsonPath("$.tickets[0].uuidCodigo", notNullValue()))
                .andReturn();

        JsonNode ventaJson = objectMapper.readTree(resultadoVenta.getResponse().getContentAsString());
        String ticketUuidStr = ventaJson.get("tickets").get(0).get("uuidCodigo").asText();
        UUID ticketUuid = UUID.fromString(ticketUuidStr);
        assertNotNull(ticketUuid);

        // =========================================================================
        // FASE 2: BÚSQUEDA INDEXADA POR CÉDULA DE ACUDIENTE Y UUID (< 500ms) (HU-23.02 / HU-23.03)
        // =========================================================================
        long inicioBusquedaCedula = System.currentTimeMillis();
        MvcResult resultadoBusqueda = mockMvc.perform(get("/api/tickets/buscar")
                        .param("criterio", cliente.getNumeroDocumento()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].uuidCodigo", is(ticketUuidStr)))
                .andExpect(jsonPath("$[0].id", is(ticketUuidStr)))
                .andExpect(jsonPath("$[0].estadoTicket", is("PAGADO")))
                .andExpect(jsonPath("$[0].estadoActual", is("PAGADO")))
                .andExpect(jsonPath("$[0].minutosContratados", is(30)))
                .andExpect(jsonPath("$[0].menorNombre", is("Camilo Zapata")))
                .andExpect(jsonPath("$[0].menorDocumento", is(menor.getNumeroDocumento())))
                .andExpect(jsonPath("$[0].atraccionNombre", is(atraccion.getNombre())))
                .andExpect(jsonPath("$[0].clienteDocumento", is(cliente.getNumeroDocumento())))
                .andExpect(jsonPath("$[0].clienteNombre", is("Mauricio Zapata")))
                .andExpect(jsonPath("$[0].horaEmision", notNullValue()))
                .andReturn();

        long duracionBusquedaMs = System.currentTimeMillis() - inicioBusquedaCedula;
        assertTrue(duracionBusquedaMs < 500, "La búsqueda de tickets por cédula debe responder en menos de 500ms (duró " + duracionBusquedaMs + "ms)");

        // Búsqueda directa por UUID mediante el mismo endpoint
        mockMvc.perform(get("/api/tickets/buscar")
                        .param("criterio", ticketUuidStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuidCodigo", is(ticketUuidStr)))
                .andExpect(jsonPath("$[0].minutosContratados", is(30)));

        // Consulta específica por path variable GET /api/tickets/{uuid}
        mockMvc.perform(get("/api/tickets/{uuid}", ticketUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ticketUuidStr)))
                .andExpect(jsonPath("$.estadoActual", is("PAGADO")))
                .andExpect(jsonPath("$.minutosContratados", is(30)));

        // =========================================================================
        // FASE 3: REIMPRESIÓN Y VERIFICACIÓN DEL CÓDIGO QR (HU-20.02 / HU-20.03)
        // =========================================================================
        MvcResult resultadoQr = mockMvc.perform(get("/api/tickets/{uuid}/qr", ticketUuid))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ticket-" + ticketUuid + ".png\""))
                .andReturn();

        byte[] qrBytes = resultadoQr.getResponse().getContentAsByteArray();
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0);

        // Validar decodificación de imagen PNG 200x200 con nivel 'M'
        BufferedImage imagenQr = ImageIO.read(new ByteArrayInputStream(qrBytes));
        assertNotNull(imagenQr);
        assertEquals(200, imagenQr.getWidth());
        assertEquals(200, imagenQr.getHeight());

        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(imagenQr);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader qrReader = new QRCodeReader();
        Result resultadoDecodificado = qrReader.decode(bitmap);

        assertNotNull(resultadoDecodificado);
        assertEquals(ticketUuidStr, resultadoDecodificado.getText(), "El QR decodificado debe coincidir con el UUID del ticket emitido");

        Object nivelM = resultadoDecodificado.getResultMetadata().get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
        assertNotNull(nivelM);
        assertTrue(nivelM.toString().contains("M"), "Nivel de corrección debe ser M (15% tolerancia)");
    }
}
