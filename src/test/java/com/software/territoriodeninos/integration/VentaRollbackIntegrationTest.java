package com.software.territoriodeninos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("VentaRollbackIntegrationTest - Pruebas de integración de rollback integral ante fallos (Notion: CA-01 y CA-04)")
class VentaRollbackIntegrationTest {

    @Autowired
    private VentaController ventaController;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private TicketRepository ticketRepository;

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
                "cajero@territoriodeninos.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc = MockMvcBuilders.standaloneSetup(ventaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // 1. Cliente y Menor
        cliente = Cliente.builder()
                .numeroDocumento("CC" + System.currentTimeMillis())
                .nombres("Andrés")
                .apellidos("Mejía")
                .telefono("3112223344")
                .correo("andres" + System.currentTimeMillis() + "@test.com")
                .build();

        menor = Menor.builder()
                .numeroDocumento("RC" + System.currentTimeMillis())
                .nombreCompleto("Mateo Mejía")
                .fechaNacimiento(LocalDate.now().minusYears(5))
                .build();
        cliente.addMenor(menor);
        cliente = clienteRepository.save(cliente);
        menor = cliente.getMenores().get(0);

        // 2. Turno de Caja Abierto
        turnoCaja = TurnoCaja.builder()
                .usuarioId(1L)
                .montoBase(new BigDecimal("100000.00"))
                .ventasEfectivoSistema(new BigDecimal("20000.00"))
                .fechaApertura(LocalDateTime.now())
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        turnoCaja = turnoCajaRepository.save(turnoCaja);

        // 3. Atracción Activa y Tarifa por Tiempo
        atraccion = Atraccion.builder()
                .nombre("Atracción Rollback Test " + System.currentTimeMillis())
                .tarifaMinuto(new BigDecimal("1000.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .tipo(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(2)
                .build();
        atraccion = atraccionRepository.save(atraccion);

        tarifaTiempo = TarifaTiempo.builder()
                .atraccion(atraccion)
                .duracionMinutos(15)
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .build();
        tarifaTiempo = tarifaTiempoRepository.save(tarifaTiempo);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Éxito (201 Created): Venta completa persiste Venta, DetalleVenta, Tickets y actualiza balance del turno (CA-02, CA-04)")
    void testProcesarVenta_ExitoCompleto() throws Exception {
        long ventasAntes = ventaRepository.count();
        long detallesAntes = detalleVentaRepository.count();
        long ticketsAntes = ticketRepository.count();

        DetalleVentaRequestDTO item1 = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                1,
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00")
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                cliente.getId(),
                turnoCaja.getId(),
                MetodoPago.EFECTIVO,
                new BigDecimal("20000.00"),
                BigDecimal.ZERO,
                "Venta exitosa",
                List.of(item1)
        );

        mockMvc.perform(post("/api/ventas/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.ventaId", notNullValue()))
                .andExpect(jsonPath("$.estadoVenta", is("PAGADO")))
                .andExpect(jsonPath("$.congelada", is(true)))
                .andExpect(jsonPath("$.montoTotal", is(15000.00)))
                .andExpect(jsonPath("$.montoRecibido", is(20000.00)))
                .andExpect(jsonPath("$.cambio", is(5000.00)))
                .andExpect(jsonPath("$.tickets", hasSize(1)))
                .andExpect(jsonPath("$.tickets[0].uuidCodigo", notNullValue()))
                .andExpect(jsonPath("$.tickets[0].id", notNullValue()))
                .andExpect(jsonPath("$.tickets[0].estadoTicket", is("PAGADO")))
                .andExpect(jsonPath("$.tickets[0].atraccionNombre", is(atraccion.getNombre())));

        assertEquals(ventasAntes + 1, ventaRepository.count());
        assertEquals(detallesAntes + 1, detalleVentaRepository.count());
        assertEquals(ticketsAntes + 1, ticketRepository.count(), "Debe haberse persistido 1 ticket individual");

        TurnoCaja turnoActualizado = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow();
        assertEquals(new BigDecimal("35000.00"), turnoActualizado.getVentasEfectivoSistema());
    }

    @Test
    @DisplayName("Éxito (201 Created): Compra de múltiples unidades genera tickets individuales con UUIDs únicos (CA-01, CA-04)")
    void testProcesarVenta_MultiplesUnidades_GeneraTicketsIndividualesConUUIDs() throws Exception {
        long ticketsAntes = ticketRepository.count();

        DetalleVentaRequestDTO itemMultiple = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                2,
                new BigDecimal("15000.00"),
                new BigDecimal("30000.00")
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                cliente.getId(),
                turnoCaja.getId(),
                MetodoPago.EFECTIVO,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                "Compra de 2 unidades",
                List.of(itemMultiple)
        );

        mockMvc.perform(post("/api/ventas/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tickets", hasSize(2)))
                .andExpect(jsonPath("$.tickets[0].uuidCodigo", notNullValue()))
                .andExpect(jsonPath("$.tickets[1].uuidCodigo", notNullValue()))
                .andExpect(jsonPath("$.tickets[0].estadoTicket", is("PAGADO")))
                .andExpect(jsonPath("$.tickets[1].estadoTicket", is("PAGADO")));

        assertEquals(ticketsAntes + 2, ticketRepository.count(), "Deben persistirse exactamente 2 tickets individuales en base de datos");
    }

    @Test
    @DisplayName("Rollback Integral (CA-01): Si el segundo ítem falla por tarifa adulterada, ningún ítem, ticket ni venta se persiste y el balance no cambia")
    void testRollbackIntegral_FalloSegundoItemPorTarifaAdulterada() throws Exception {
        long ventasAntes = ventaRepository.count();
        long detallesAntes = detalleVentaRepository.count();
        long ticketsAntes = ticketRepository.count();
        BigDecimal balanceAntes = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow().getVentasEfectivoSistema();

        // Ítem 1 es completamente válido
        DetalleVentaRequestDTO item1Valido = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                1,
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00")
        );

        // Ítem 2 tiene tarifa adulterada ($5,000 en vez de $15,000 de la BD)
        DetalleVentaRequestDTO item2Adulterado = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                1,
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00")
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                cliente.getId(),
                turnoCaja.getId(),
                MetodoPago.EFECTIVO,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                "Intento fraudulento",
                List.of(item1Valido, item2Adulterado)
        );

        mockMvc.perform(post("/api/ventas/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", containsString("Inconsistencia en el precio")));

        // Verificación de Rollback Total:
        // Ninguna venta creada
        assertEquals(ventasAntes, ventaRepository.count(), "No debe persistirse ninguna venta si falla algún ítem");
        // Ningún detalle creado (incluso el ítem 1 que era válido fue revertido)
        assertEquals(detallesAntes, detalleVentaRepository.count(), "El ítem 1 debe haber sufrido rollback integral");
        // Ningún ticket creado
        assertEquals(ticketsAntes, ticketRepository.count(), "No debe persistirse ningún ticket ante rollback");
        // El balance del turno de caja no fue tocado
        TurnoCaja turnoPost = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow();
        assertEquals(balanceAntes, turnoPost.getVentasEfectivoSistema(), "El balance del turno no debe modificarse ante fallo transaccional");
    }

    @Test
    @DisplayName("Rollback Integral (CA-01): Si la suma de tickets excede el aforo disponible, se realiza rollback integral")
    void testRollbackIntegral_FalloPorExcesoAforo() throws Exception {
        long ventasAntes = ventaRepository.count();
        long detallesAntes = detalleVentaRepository.count();
        long ticketsAntes = ticketRepository.count();
        BigDecimal balanceAntes = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow().getVentasEfectivoSistema();

        // Capacidad máxima de atraccion es 2. Solicitamos 5 tickets.
        DetalleVentaRequestDTO itemSobrecupo = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                5,
                new BigDecimal("15000.00"),
                new BigDecimal("75000.00")
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                cliente.getId(),
                turnoCaja.getId(),
                MetodoPago.EFECTIVO,
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                "Intento sobrecupo",
                List.of(itemSobrecupo)
        );

        mockMvc.perform(post("/api/ventas/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", containsString("Aforo máximo excedido")));

        assertEquals(ventasAntes, ventaRepository.count());
        assertEquals(detallesAntes, detalleVentaRepository.count());
        assertEquals(ticketsAntes, ticketRepository.count(), "No debe persistirse ningún ticket");
        TurnoCaja turnoPost = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow();
        assertEquals(balanceAntes, turnoPost.getVentasEfectivoSistema());
    }

    @Test
    @DisplayName("Rollback Integral (CA-01): Si el monto recibido es inferior al total neto, se rechaza y no se persiste nada")
    void testRollbackIntegral_FalloPorMontoInsuficiente() throws Exception {
        long ventasAntes = ventaRepository.count();
        long detallesAntes = detalleVentaRepository.count();
        long ticketsAntes = ticketRepository.count();
        BigDecimal balanceAntes = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow().getVentasEfectivoSistema();

        DetalleVentaRequestDTO item = new DetalleVentaRequestDTO(
                atraccion.getId(),
                menor.getId(),
                TipoTarifa.TIEMPO,
                tarifaTiempo.getId(),
                null,
                1,
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00")
        );

        // Venta de 15,000 pero cliente entrega solo 10,000
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                cliente.getId(),
                turnoCaja.getId(),
                MetodoPago.EFECTIVO,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                "Monto insuficiente",
                List.of(item)
        );

        mockMvc.perform(post("/api/ventas/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", containsString("inferior al total neto")));

        assertEquals(ventasAntes, ventaRepository.count());
        assertEquals(detallesAntes, detalleVentaRepository.count());
        assertEquals(ticketsAntes, ticketRepository.count(), "No debe persistirse ningún ticket");
        TurnoCaja turnoPost = turnoCajaRepository.findById(turnoCaja.getId()).orElseThrow();
        assertEquals(balanceAntes, turnoPost.getVentasEfectivoSistema());
    }
}
