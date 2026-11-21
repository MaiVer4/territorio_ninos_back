package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.tienda.CatalogoAtraccionOnlineDTO;
import com.software.territoriodeninos.dto.tienda.CompraOnlineRequestDTO;
import com.software.territoriodeninos.dto.tienda.CompraOnlineResponseDTO;
import com.software.territoriodeninos.dto.tienda.ItemCompraOnlineDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.*;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TiendaServiceImplTest - Pruebas unitarias de lógica de negocio para compras online")
class TiendaServiceImplTest {

    @Mock
    private AtraccionRepository atraccionRepository;
    @Mock
    private TarifaTiempoRepository tarifaTiempoRepository;
    @Mock
    private TarifaEventoRepository tarifaEventoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private MenorRepository menorRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private DetalleVentaRepository detalleVentaRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private AuditoriaTicketRepository auditoriaTicketRepository;
    @Mock
    private ConfiguracionParqueRepository configuracionParqueRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SesionJuegoRepository sesionJuegoRepository;
    @Mock
    private EstimadorIngresoService estimadorIngresoService;
    @Mock
    private AtraccionService atraccionService;

    @InjectMocks
    private TiendaServiceImpl tiendaService;

    private Atraccion atraccionActiva;
    private Cliente clienteExistente;
    private TarifaTiempo tarifaTiempo;

    @BeforeEach
    void setUp() {
        atraccionActiva = Atraccion.builder()
                .id(1L)
                .nombre("Piscina de Pelotas")
                .descripcion("Diversión segura")
                .tipo(TipoAtraccion.TEMPORIZADA)
                .estado(EstadoAtraccion.ACTIVO)
                .capacidadMaxima(20)
                .duracionMinutosBase(15)
                .tarifaMinuto(BigDecimal.valueOf(100))
                .edadMinima(3)
                .edadMaxima(10)
                .build();

        clienteExistente = Cliente.builder()
                .id(10L)
                .numeroDocumento("1098765432")
                .nombres("Carlos")
                .apellidos("Mendoza")
                .telefono("3101234567")
                .correo("carlos@gmail.com")
                .build();

        tarifaTiempo = TarifaTiempo.builder()
                .id(100L)
                .atraccion(atraccionActiva)
                .duracionMinutos(15)
                .precio(BigDecimal.valueOf(15000))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("obtenerCatalogoPublico - Mapea atracciones activas en batch sin N+1 e incluye porcentajeImpuesto")
    void obtenerCatalogoPublico_Exito() {
        ConfiguracionParque config = ConfiguracionParque.builder()
                .aplicaImpuesto(true)
                .porcentajeImpuesto(BigDecimal.valueOf(19))
                .build();
        when(atraccionRepository.findByEstadoInOrderByNombreAsc(List.of(EstadoAtraccion.ACTIVO)))
                .thenReturn(List.of(atraccionActiva));
        when(tarifaTiempoRepository.findByAtraccionIdInAndActivoTrueOrderByDuracionMinutosAsc(List.of(1L)))
                .thenReturn(List.of(tarifaTiempo));
        when(tarifaEventoRepository.findByAtraccionIdInAndActivoTrueOrderByIntentosAsc(List.of(1L)))
                .thenReturn(List.of());
        when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));

        List<CatalogoAtraccionOnlineDTO> catalogo = tiendaService.obtenerCatalogoPublico();

        assertEquals(1, catalogo.size());
        assertEquals("Piscina de Pelotas", catalogo.get(0).nombre());
        assertTrue(catalogo.get(0).disponibleParaCompra());
        assertEquals(1, catalogo.get(0).tarifasTiempo().size());
        assertEquals(BigDecimal.valueOf(19), catalogo.get(0).porcentajeImpuesto());
        verify(tarifaTiempoRepository, never()).findByAtraccionIdAndActivoTrue(any());
        verify(tarifaEventoRepository, never()).findByAtraccionIdAndActivoTrue(any());
    }

    @Test
    @DisplayName("procesarCompraOnline - Rechaza compra si honeypot contiene datos")
    void procesarCompraOnline_HoneypotLleno_LanzaExcepcion() {
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(), "http://bot-spam.com"
        );

        assertThrows(ReglaNegocioException.class, () -> tiendaService.procesarCompraOnline(req));
        verifyNoInteractions(ventaRepository);
    }

    @Test
    @DisplayName("procesarCompraOnline - Valida que la edad del menor no sea menor a la mínima permitida")
    void procesarCompraOnline_EdadMenorInvalida_LanzaExcepcion() {
        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, 100L, null, "Bebe", 2, 1);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(item), null
        );

        when(clienteRepository.findByNumeroDocumento("1098765432")).thenReturn(Optional.of(clienteExistente));
        when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> tiendaService.procesarCompraOnline(req));
        assertTrue(ex.getMessage().contains("no cumple con la edad mínima"));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("procesarCompraOnline - Procesa compra exitosamente sin sobrescribir cliente existente, emite tickets y audita")
    void procesarCompraOnline_Exito() {
        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, 100L, null, "Lucas", 6, 2);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Otro Nombre", "Intruso", "3999999999",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(item), null
        );

        when(clienteRepository.findByNumeroDocumento("1098765432")).thenReturn(Optional.of(clienteExistente));
        when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempo));

        Menor menorMock = Menor.builder().id(20L).nombreCompleto("Lucas").edad(6).cliente(clienteExistente).build();
        when(menorRepository.findByClienteIdAndNombreCompletoIgnoreCase(10L, "Lucas")).thenReturn(Optional.of(menorMock));

        Venta ventaMock = Venta.builder()
                .id(1L)
                .cliente(clienteExistente)
                .canalVenta(CanalVenta.ONLINE)
                .codigoOrden("ORD-12345")
                .subtotal(BigDecimal.valueOf(30000))
                .montoTotal(BigDecimal.valueOf(30000))
                .fechaVenta(LocalDateTime.now())
                .build();
        when(ventaRepository.save(any(Venta.class))).thenReturn(ventaMock);

        DetalleVenta detalleMock = DetalleVenta.builder().id(5L).venta(ventaMock).atraccion(atraccionActiva).build();
        when(detalleVentaRepository.save(any(DetalleVenta.class))).thenReturn(detalleMock);

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompraOnlineResponseDTO response = tiendaService.procesarCompraOnline(req);

        assertNotNull(response);
        assertEquals("carlos@gmail.com", response.clienteCorreo());
        assertEquals("Carlos", clienteExistente.getNombres());
        assertEquals("3101234567", clienteExistente.getTelefono());
        verify(clienteRepository, never()).save(any(Cliente.class));
        assertEquals(2, response.tickets().size());
        assertEquals("Lucas", response.tickets().get(0).menorNombre());
        verify(emailService, times(1)).enviarBoletasCompraOnline(any(), any(), any(), any(), any(), any(), any(), any());
        verify(auditoriaTicketRepository, times(2)).save(any(AuditoriaTicket.class));
        verify(atraccionService, times(1)).invalidarCacheCatalogo();
    }

    @Test
    @DisplayName("procesarCompraOnline - Rechaza tarifa de tiempo que pertenece a otra atracción o está inactiva (P0 fraude)")
    void procesarCompraOnline_TarifaDeOtraAtraccionOInactiva_LanzaExcepcion() {
        Atraccion otraAtraccion = Atraccion.builder().id(99L).nombre("Otra").estado(EstadoAtraccion.ACTIVO).build();
        TarifaTiempo tarifaAjena = TarifaTiempo.builder()
                .id(200L)
                .atraccion(otraAtraccion)
                .duracionMinutos(5)
                .precio(BigDecimal.valueOf(1000))
                .activo(true)
                .build();

        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, 200L, null, "Lucas", 6, 1);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(item), null
        );

        when(clienteRepository.findByNumeroDocumento("1098765432")).thenReturn(Optional.of(clienteExistente));
        when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));
        when(menorRepository.findByClienteIdAndNombreCompletoIgnoreCase(10L, "Lucas"))
                .thenReturn(Optional.of(Menor.builder().id(20L).nombreCompleto("Lucas").edad(6).cliente(clienteExistente).build()));
        when(tarifaTiempoRepository.findById(200L)).thenReturn(Optional.of(tarifaAjena));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> tiendaService.procesarCompraOnline(req));
        assertTrue(ex.getMessage().contains("no pertenece a la atracción"));
        verifyNoInteractions(ventaRepository);
    }

    @Test
    @DisplayName("procesarCompraOnline - Rechaza método de pago presencial EFECTIVO en canal online")
    void procesarCompraOnline_MetodoPagoEfectivo_LanzaExcepcion() {
        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, 100L, null, "Lucas", 6, 1);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.EFECTIVO, List.of(item), null
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> tiendaService.procesarCompraOnline(req));
        assertTrue(ex.getMessage().contains("no está habilitado para compras online"));
        verifyNoInteractions(ventaRepository);
    }

    @Test
    @DisplayName("obtenerCatalogoPublico - Calcula ocupacionActual, cuposLibres y horaProximoIngreso para atracción llena")
    void obtenerCatalogoPublico_AtraccionLlena_IncluyeHoraEstimada() {
        atraccionActiva.setCapacidadMaxima(2);
        SesionJuego s1 = SesionJuego.builder().id(1L).atraccion(atraccionActiva).estadoSesion(EstadoSesionJuego.EN_CURSO).build();
        SesionJuego s2 = SesionJuego.builder().id(2L).atraccion(atraccionActiva).estadoSesion(EstadoSesionJuego.PAUSADA).build();
        LocalDateTime horaEstimada = LocalDateTime.of(2026, 11, 20, 16, 20);

        when(atraccionRepository.findByEstadoInOrderByNombreAsc(List.of(EstadoAtraccion.ACTIVO)))
                .thenReturn(List.of(atraccionActiva));
        when(tarifaTiempoRepository.findByAtraccionIdInAndActivoTrueOrderByDuracionMinutosAsc(List.of(1L)))
                .thenReturn(List.of(tarifaTiempo));
        when(tarifaEventoRepository.findByAtraccionIdInAndActivoTrueOrderByIntentosAsc(List.of(1L)))
                .thenReturn(List.of());
        when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(sesionJuegoRepository.findByAtraccionIdInAndEstadoSesionIn(any(), any())).thenReturn(List.of(s1, s2));
        when(estimadorIngresoService.estimarProximoIngresoBatch(any(), any(), any()))
                .thenReturn(java.util.Map.of(1L, horaEstimada));

        List<CatalogoAtraccionOnlineDTO> catalogo = tiendaService.obtenerCatalogoPublico();

        assertEquals(1, catalogo.size());
        assertEquals(2, catalogo.get(0).ocupacionActual());
        assertEquals(0, catalogo.get(0).cuposLibres());
        assertEquals(horaEstimada, catalogo.get(0).horaProximoIngreso());
        assertEquals("16:20 (aprox.)", catalogo.get(0).ingresoTexto());
    }

    @Test
    @DisplayName("procesarCompraOnline - Persiste horas estimadas escalonadas en Ticket y las envía al correo con totales")
    void procesarCompraOnline_ConAforoLleno_AsignaHorasEscalonadas() {
        ItemCompraOnlineDTO item = new ItemCompraOnlineDTO(1L, 100L, null, "Lucas", 6, 2);
        CompraOnlineRequestDTO req = new CompraOnlineRequestDTO(
                "1098765432", "Carlos", "Mendoza", "3101234567",
                "carlos@gmail.com", MetodoPago.NEQUI, List.of(item), null
        );

        LocalDateTime h1 = LocalDateTime.of(2026, 11, 20, 15, 10);
        LocalDateTime h2 = LocalDateTime.of(2026, 11, 20, 15, 25);

        when(clienteRepository.findByNumeroDocumento("1098765432")).thenReturn(Optional.of(clienteExistente));
        when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempo));
        when(menorRepository.findByClienteIdAndNombreCompletoIgnoreCase(10L, "Lucas"))
                .thenReturn(Optional.of(Menor.builder().id(20L).nombreCompleto("Lucas").edad(6).cliente(clienteExistente).build()));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });
        when(detalleVentaRepository.save(any(DetalleVenta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estimadorIngresoService.estimar(eq(atraccionActiva), eq(List.of(15, 15)), any()))
                .thenReturn(List.of(h1, h2));

        List<Ticket> guardados = new java.util.ArrayList<>();
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            guardados.add(t);
            return t;
        });

        CompraOnlineResponseDTO response = tiendaService.procesarCompraOnline(req);

        assertEquals(2, guardados.size());
        assertEquals(h1, guardados.get(0).getHoraIngresoEstimada());
        assertEquals(h2, guardados.get(1).getHoraIngresoEstimada());
        assertEquals("15:10 (aprox.)", response.tickets().get(0).ingresoTexto());
        assertEquals("15:25 (aprox.)", response.tickets().get(1).ingresoTexto());

        verify(emailService).enviarBoletasCompraOnline(
                eq("carlos@gmail.com"),
                eq("Carlos Mendoza"),
                any(),
                eq(BigDecimal.valueOf(30000).setScale(2)),
                eq(BigDecimal.ZERO.setScale(2)),
                eq(BigDecimal.valueOf(30000).setScale(2)),
                eq(MetodoPago.NEQUI),
                eq(response.tickets())
        );
    }

    @Test
    @DisplayName("consultarBoletosActivos - Exige segundo factor (correo o código de orden) y resuelve modalidad real")
    void consultarBoletosActivos_IncluyeHoraIngresoEstimada() {
        assertThrows(ReglaNegocioException.class,
                () -> tiendaService.consultarBoletosActivos("1098765432", null, null));

        LocalDateTime horaEstimada = LocalDateTime.of(2026, 11, 20, 17, 45);
        Ticket ticket = Ticket.builder()
                .uuidCodigo(UUID.randomUUID())
                .detalleVenta(DetalleVenta.builder()
                        .atraccion(atraccionActiva)
                        .tipoTarifa(TipoTarifa.TIEMPO)
                        .tarifaTiempoId(100L)
                        .precioUnitario(BigDecimal.valueOf(15000))
                        .build())
                .menor(Menor.builder().nombreCompleto("Sofía").edad(5).build())
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .horaIngresoEstimada(horaEstimada)
                .build();

        when(ticketRepository.buscarBoletosActivosPorDocumentoYCorreo(
                eq("1098765432"), eq("carlos@gmail.com"), any(), any()))
                .thenReturn(List.of(ticket));
        when(tarifaTiempoRepository.findAllById(any())).thenReturn(List.of(tarifaTiempo));

        List<com.software.territoriodeninos.dto.tienda.TicketOnlineDTO> res =
                tiendaService.consultarBoletosActivos("1098765432", "carlos@gmail.com", null);

        assertEquals(1, res.size());
        assertEquals("15 min", res.get(0).modalidadTexto());
        assertEquals(horaEstimada, res.get(0).horaIngresoEstimada());
        assertEquals("17:45 (aprox.)", res.get(0).ingresoTexto());
    }
}
