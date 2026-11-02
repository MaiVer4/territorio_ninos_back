package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TicketConsultaDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.EstadoTicket;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceTest - Pruebas unitarias de búsqueda indexada y consulta de tickets (HU-23.02 / HU-23.03)")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TarifaTiempoRepository tarifaTiempoRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Cliente cliente;
    private Menor menor;
    private Atraccion atraccion;
    private Venta venta;
    private DetalleVenta detalleVenta;
    private Ticket ticket;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        uuid = UUID.randomUUID();

        cliente = Cliente.builder()
                .id(1L)
                .numeroDocumento("1098765432")
                .nombres("Laura")
                .apellidos("Castro")
                .build();

        menor = Menor.builder()
                .id(10L)
                .numeroDocumento("1122334455")
                .nombreCompleto("Mateo Gómez")
                .cliente(cliente)
                .build();

        atraccion = Atraccion.builder()
                .id(20L)
                .nombre("Laberinto Mágico")
                .build();

        venta = Venta.builder()
                .id(100L)
                .cliente(cliente)
                .montoTotal(new BigDecimal("25000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.PAGADO)
                .fechaVenta(LocalDateTime.now())
                .congelada(true)
                .build();

        detalleVenta = DetalleVenta.builder()
                .id(205L)
                .venta(venta)
                .atraccion(atraccion)
                .menor(menor)
                .tipoTarifa(TipoTarifa.TIEMPO)
                .tarifaTiempoId(50L)
                .cantidad(1)
                .precioUnitario(new BigDecimal("25000.00"))
                .subtotal(new BigDecimal("25000.00"))
                .build();

        ticket = Ticket.builder()
                .uuidCodigo(uuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("buscarTickets - Criterio con formato UUID recupera el ticket y resuelve minutos de tarifa de tiempo (CA-02, CA-03)")
    void buscarTickets_criterioEsUuidValido_retornaTicketConsultaDTO() {
        TarifaTiempo tarifaTiempo = TarifaTiempo.builder()
                .id(50L)
                .duracionMinutos(45)
                .build();

        when(ticketRepository.findByUuidCodigo(uuid)).thenReturn(Optional.of(ticket));
        when(tarifaTiempoRepository.findAllById(Set.of(50L))).thenReturn(List.of(tarifaTiempo));

        List<TicketConsultaDTO> resultados = ticketService.buscarTickets(uuid.toString());

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        TicketConsultaDTO dto = resultados.get(0);
        assertEquals(uuid, dto.uuidCodigo());
        assertEquals(uuid, dto.id());
        assertEquals(EstadoTicket.PAGADO, dto.estadoTicket());
        assertEquals(EstadoTicket.PAGADO, dto.estadoActual());
        assertEquals(45, dto.minutosContratados());
        assertEquals(20L, dto.atraccionId());
        assertEquals("Laberinto Mágico", dto.atraccionNombre());
        assertEquals(10L, dto.menorId());
        assertEquals("Mateo Gómez", dto.menorNombre());
        assertEquals("1122334455", dto.menorDocumento());
        assertEquals("1098765432", dto.clienteDocumento());
        assertEquals("Laura Castro", dto.clienteNombre());
        assertEquals(100L, dto.ventaId());
        assertEquals(205L, dto.detalleVentaId());

        verify(ticketRepository).findByUuidCodigo(uuid);
        verify(tarifaTiempoRepository).findAllById(Set.of(50L));
    }

    @Test
    @DisplayName("buscarTickets - Criterio es cédula de acudiente recupera los tickets asociados")
    void buscarTickets_criterioEsCedulaAcudiente_retornaListaTickets() {
        TarifaTiempo tarifaTiempo = TarifaTiempo.builder()
                .id(50L)
                .duracionMinutos(30)
                .build();

        when(ticketRepository.buscarPorDocumentoAcudiente("1098765432")).thenReturn(List.of(ticket));
        when(tarifaTiempoRepository.findAllById(Set.of(50L))).thenReturn(List.of(tarifaTiempo));

        List<TicketConsultaDTO> resultados = ticketService.buscarTickets("1098765432");

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(uuid, resultados.get(0).uuidCodigo());
        assertEquals(30, resultados.get(0).minutosContratados());

        verify(ticketRepository).buscarPorDocumentoAcudiente("1098765432");
    }

    @Test
    @DisplayName("buscarTickets - Criterio es tarifa MINUTO_BASE asigna cantidad de minutos del detalle")
    void buscarTickets_tarifaMinutoBase_asignaCantidadComoMinutos() {
        detalleVenta.setTipoTarifa(TipoTarifa.MINUTO_BASE);
        detalleVenta.setTarifaTiempoId(null);
        detalleVenta.setCantidad(20);

        when(ticketRepository.buscarPorDocumentoAcudiente("1098765432")).thenReturn(List.of(ticket));

        List<TicketConsultaDTO> resultados = ticketService.buscarTickets("1098765432");

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(20, resultados.get(0).minutosContratados());
        verifyNoInteractions(tarifaTiempoRepository);
    }

    @Test
    @DisplayName("buscarTickets - Lanza ReglaNegocioException si el criterio es nulo o vacío")
    void buscarTickets_criterioVacioONulo_lanzaReglaNegocioException() {
        assertThrows(ReglaNegocioException.class, () -> ticketService.buscarTickets(null));
        assertThrows(ReglaNegocioException.class, () -> ticketService.buscarTickets(""));
        assertThrows(ReglaNegocioException.class, () -> ticketService.buscarTickets("   "));
        verifyNoInteractions(ticketRepository);
    }

    @Test
    @DisplayName("buscarTickets - Retorna lista vacía si no existen tickets para el criterio")
    void buscarTickets_noEncontrado_retornaListaVacia() {
        when(ticketRepository.buscarPorDocumentoAcudiente("999999999")).thenReturn(List.of());

        List<TicketConsultaDTO> resultados = ticketService.buscarTickets("999999999");

        assertNotNull(resultados);
        assertTrue(resultados.isEmpty());
    }

    @Test
    @DisplayName("obtenerTicketPorUuid - Recupera ticket existente por UUID")
    void obtenerTicketPorUuid_existente_retornaTicketConsultaDTO() {
        when(ticketRepository.findByUuidCodigo(uuid)).thenReturn(Optional.of(ticket));

        TicketConsultaDTO dto = ticketService.obtenerTicketPorUuid(uuid);

        assertNotNull(dto);
        assertEquals(uuid, dto.uuidCodigo());
        assertEquals(EstadoTicket.PAGADO, dto.estadoTicket());
    }

    @Test
    @DisplayName("obtenerTicketPorUuid - Lanza RecursoNoEncontradoException si el ticket no existe")
    void obtenerTicketPorUuid_noExiste_lanzaRecursoNoEncontradoException() {
        UUID randomUuid = UUID.randomUUID();
        when(ticketRepository.findByUuidCodigo(randomUuid)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> ticketService.obtenerTicketPorUuid(randomUuid));
    }

    @Test
    @DisplayName("obtenerTicketPorUuid - Lanza ReglaNegocioException si el UUID es nulo")
    void obtenerTicketPorUuid_uuidNulo_lanzaReglaNegocioException() {
        assertThrows(ReglaNegocioException.class, () -> ticketService.obtenerTicketPorUuid(null));
    }
}
