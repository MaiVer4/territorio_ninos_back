package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TicketRepositoryTest - Pruebas de integración con base de datos para Ticket y unicidad de UUID (Notion: CA-02, CA-03)")
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MenorRepository menorRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    @Autowired
    private AtraccionRepository atraccionRepository;

    private Cliente cliente;
    private Menor menor;
    private TurnoCaja turnoCaja;
    private Atraccion atraccion;
    private Venta venta;
    private DetalleVenta detalleVenta;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .numeroDocumento("CC" + System.currentTimeMillis())
                .nombres("Andrea")
                .apellidos("Martínez")
                .telefono("3119876543")
                .correo("andrea" + System.currentTimeMillis() + "@test.com")
                .build();
        cliente = clienteRepository.save(cliente);

        menor = Menor.builder()
                .numeroDocumento("RC" + System.currentTimeMillis())
                .nombreCompleto("Sofía Martínez")
                .fechaNacimiento(LocalDate.now().minusYears(5))
                .cliente(cliente)
                .build();
        menor = menorRepository.save(menor);

        turnoCaja = TurnoCaja.builder()
                .usuarioId(1L)
                .montoBase(new BigDecimal("150000.00"))
                .ventasEfectivoSistema(BigDecimal.ZERO)
                .estado(EstadoTurnoCaja.ABIERTO)
                .fechaApertura(LocalDateTime.now())
                .build();
        turnoCaja = turnoCajaRepository.save(turnoCaja);

        atraccion = Atraccion.builder()
                .nombre("Mundo Saltarín " + System.currentTimeMillis())
                .descripcion("Camas elásticas")
                .capacidadMaxima(20)
                .tarifaMinuto(new BigDecimal("600.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();
        atraccion = atraccionRepository.save(atraccion);

        venta = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("30000.00"))
                .subtotal(new BigDecimal("30000.00"))
                .descuento(BigDecimal.ZERO)
                .impuestos(BigDecimal.ZERO)
                .montoRecibido(new BigDecimal("50000.00"))
                .cambio(new BigDecimal("20000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.PAGADO)
                .fechaVenta(LocalDateTime.now())
                .congelada(true)
                .build();

        detalleVenta = DetalleVenta.builder()
                .atraccion(atraccion)
                .menor(menor)
                .tipoTarifa(TipoTarifa.MINUTO_BASE)
                .cantidad(2)
                .precioUnitario(new BigDecimal("15000.00"))
                .subtotal(new BigDecimal("30000.00"))
                .build();

        venta.addDetalle(detalleVenta);
        venta = ventaRepository.save(venta);
    }

    @Test
    @DisplayName("guardarYBuscarPorUuidCodigo - Persiste ticket y recupera por UUID con grafo de detalle y menor cargados")
    void guardarYBuscarPorUuidCodigo_persisteYRecuperaCorrectamente() {
        UUID uuid = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .uuidCodigo(uuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();

        Ticket ticketGuardado = ticketRepository.save(ticket);
        assertNotNull(ticketGuardado);

        Optional<Ticket> optTicket = ticketRepository.findByUuidCodigo(uuid);
        assertTrue(optTicket.isPresent());

        Ticket encontrado = optTicket.get();
        assertEquals(uuid, encontrado.getUuidCodigo());
        assertEquals(EstadoTicket.PAGADO, encontrado.getEstadoTicket());
        assertEquals(detalleVenta.getId(), encontrado.getDetalleVenta().getId());
        assertEquals("Sofía Martínez", encontrado.getMenor().getNombreCompleto());
        assertEquals(atraccion.getId(), encontrado.getDetalleVenta().getAtraccion().getId());
    }

    @Test
    @DisplayName("existsByUuidCodigo - Retorna true si el UUID ya existe en BD y false si no existe")
    void existsByUuidCodigo_verificaExistenciaCorrecta() {
        UUID uuid = UUID.randomUUID();
        assertFalse(ticketRepository.existsByUuidCodigo(uuid));

        Ticket ticket = Ticket.builder()
                .uuidCodigo(uuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();
        ticketRepository.save(ticket);

        assertTrue(ticketRepository.existsByUuidCodigo(uuid));
        assertFalse(ticketRepository.existsByUuidCodigo(UUID.randomUUID()));
    }

    @Test
    @DisplayName("findByDetalleVentaVentaIdOrderByFechaEmisionAsc - Recupera todos los tickets de una venta")
    void findByDetalleVentaVentaId_recuperaTicketsDeLaVenta() {
        Ticket t1 = Ticket.builder()
                .uuidCodigo(UUID.randomUUID())
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now().minusMinutes(2))
                .build();

        Ticket t2 = Ticket.builder()
                .uuidCodigo(UUID.randomUUID())
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now().minusMinutes(1))
                .build();

        ticketRepository.saveAll(List.of(t1, t2));

        List<Ticket> ticketsVenta = ticketRepository.findByDetalleVentaVentaIdOrderByFechaEmisionAsc(venta.getId());
        assertEquals(2, ticketsVenta.size());
        assertEquals(t1.getUuidCodigo(), ticketsVenta.get(0).getUuidCodigo());
        assertEquals(t2.getUuidCodigo(), ticketsVenta.get(1).getUuidCodigo());
    }

    @Test
    @DisplayName("restriccionUnica_uuidDuplicado_lanzaExcepcionDeIntegridad")
    void restriccionUnica_uuidDuplicado_lanzaExcepcion() {
        UUID uuidDuplicado = UUID.randomUUID();

        Ticket t1 = Ticket.builder()
                .uuidCodigo(uuidDuplicado)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();
        ticketRepository.saveAndFlush(t1);

        Ticket t2 = Ticket.builder()
                .uuidCodigo(uuidDuplicado)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            ticketRepository.saveAndFlush(t2);
        });
    }

    @Test
    @DisplayName("buscarPorDocumentoAcudiente - Búsqueda indexada por cédula de acudiente con respuesta menor a 500ms (HU-23.02 / CA-01)")
    void buscarPorDocumentoAcudiente_recuperaTicketsYRespondeEnMenosDe500ms() {
        UUID uuid = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .uuidCodigo(uuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();
        ticketRepository.saveAndFlush(ticket);

        long inicio = System.currentTimeMillis();
        List<Ticket> tickets = ticketRepository.buscarPorDocumentoAcudiente(cliente.getNumeroDocumento());
        long duracionMs = System.currentTimeMillis() - inicio;

        assertTrue(duracionMs < 500, "La búsqueda indexada por cédula debe responder en menos de 500ms (demoró " + duracionMs + "ms)");
        assertFalse(tickets.isEmpty());
        assertEquals(uuid, tickets.get(0).getUuidCodigo());
        assertNotNull(tickets.get(0).getDetalleVenta().getVenta().getCliente());
        assertEquals(cliente.getNumeroDocumento(), tickets.get(0).getDetalleVenta().getVenta().getCliente().getNumeroDocumento());
    }

    @Test
    @DisplayName("findByUuidCodigo - Búsqueda indexada por clave primaria UUID con respuesta menor a 500ms (HU-23.02 / CA-01)")
    void findByUuidCodigo_respondeEnMenosDe500ms() {
        UUID uuid = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .uuidCodigo(uuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();
        ticketRepository.saveAndFlush(ticket);

        long inicio = System.currentTimeMillis();
        Optional<Ticket> optTicket = ticketRepository.findByUuidCodigo(uuid);
        long duracionMs = System.currentTimeMillis() - inicio;

        assertTrue(duracionMs < 500, "La búsqueda indexada por UUID debe responder en menos de 500ms (demoró " + duracionMs + "ms)");
        assertTrue(optTicket.isPresent());
        assertEquals(uuid, optTicket.get().getUuidCodigo());
    }
}
