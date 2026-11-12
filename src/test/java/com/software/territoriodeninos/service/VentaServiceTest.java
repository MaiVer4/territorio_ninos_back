package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaServiceTest - Pruebas unitarias de procesamiento de ventas y congelamiento")
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MenorRepository menorRepository;

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AtraccionRepository atraccionRepository;

    @Mock
    private AforoValidatorService aforoValidatorService;

    @Mock
    private VentaCalculoService ventaCalculoService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UUIDGeneratorService uuidGeneratorService;

    @Mock
    private AtraccionService atraccionService;

    @Mock
    private AuditoriaOperacionService auditoriaOperacionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private Cliente clienteMock;
    private TurnoCaja turnoCajaMock;
    private Atraccion atraccionMock;
    private Menor menorMock;

    @BeforeEach
    void setUp() {
        clienteMock = Cliente.builder()
                .id(1L)
                .numeroDocumento("1098765432")
                .nombres("Laura")
                .apellidos("Castro")
                .build();

        menorMock = Menor.builder()
                .id(10L)
                .nombreCompleto("Lucas Castro")
                .cliente(clienteMock)
                .build();

        turnoCajaMock = TurnoCaja.builder()
                .id(5L)
                .usuarioId(2L)
                .montoBase(new BigDecimal("100000.00"))
                .ventasEfectivoSistema(new BigDecimal("50000.00"))
                .estado(EstadoTurnoCaja.ABIERTO)
                .fechaApertura(LocalDateTime.now())
                .build();

        atraccionMock = Atraccion.builder()
                .id(20L)
                .nombre("Piscina de Pelotas")
                .estado(EstadoAtraccion.ACTIVO)
                .tarifaMinuto(new BigDecimal("1000.00"))
                .capacidadMaxima(30)
                .build();
    }

    @Test
    @DisplayName("Debe procesar venta en efectivo, asignar PAGADO, fecha emisión, congelar y actualizar balance de caja (CA-02, CA-03)")
    void testProcesarVenta_ExitoEfectivo() {
        // Arrange
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                20L, 10L, TipoTarifa.TIEMPO, 100L, null, 2, new BigDecimal("15000.00"), new BigDecimal("30000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("40000.00"), BigDecimal.ZERO, "Venta normal", List.of(itemDTO)
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));
        when(menorRepository.findById(10L)).thenReturn(Optional.of(menorMock));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("30000.00")),
                List.of(),
                new BigDecimal("30000.00"),
                BigDecimal.ZERO,
                new BigDecimal("30000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("30000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);

        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(999L);
            return v;
        });

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        when(uuidGeneratorService.generarUuidsUnicos(2)).thenReturn(List.of(uuid1, uuid2));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VentaResponseDTO response = ventaService.procesarVenta(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(999L, response.id());
        assertEquals(999L, response.ventaId());
        assertEquals(EstadoVenta.PAGADO, response.estadoVenta());
        assertTrue(response.congelada());
        assertNotNull(response.fechaVenta());
        assertEquals(new BigDecimal("30000.00"), response.montoTotal());
        assertEquals(new BigDecimal("40000.00"), response.montoRecibido());
        assertEquals(new BigDecimal("10000.00"), response.cambio());
        assertEquals(1, response.detalles().size());

        // Verificar generación de tickets individuales criptoseguros (Notion: CA-01, CA-04)
        assertNotNull(response.tickets());
        assertEquals(2, response.tickets().size());
        assertEquals(uuid1, response.tickets().get(0).uuidCodigo());
        assertEquals(com.software.territoriodeninos.util.EstadoTicket.PAGADO, response.tickets().get(0).estadoTicket());
        assertEquals(20L, response.tickets().get(0).atraccionId());
        assertEquals("Piscina de Pelotas", response.tickets().get(0).atraccionNombre());
        assertEquals(10L, response.tickets().get(0).menorId());
        assertEquals("Lucas Castro", response.tickets().get(0).menorNombre());

        // Verificar que el balance del turno de caja se actualizó: 50000 + 30000 = 80000
        assertEquals(new BigDecimal("80000.00"), turnoCajaMock.getVentasEfectivoSistema());
        verify(turnoCajaRepository).save(turnoCajaMock);
        verify(ventaRepository).save(any(Venta.class));
        verify(ticketRepository).saveAll(anyList());
        verify(atraccionService).invalidarCacheCatalogo();
    }

    @Test
    @DisplayName("Debe generar automáticamente un ticket individual por cada unidad vendida (Notion: CA-01, CA-04)")
    void testProcesarVenta_GeneracionAutomaticaTicketsIndividualesPorCantidad() {
        // Arrange: 2 ítems, cant 3 en el primero y cant 2 en el segundo -> total 5 tickets
        DetalleVentaRequestDTO item1 = new DetalleVentaRequestDTO(
                20L, 10L, TipoTarifa.TIEMPO, 100L, null, 3, new BigDecimal("10000.00"), new BigDecimal("30000.00")
        );
        DetalleVentaRequestDTO item2 = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.TIEMPO, 100L, null, 2, new BigDecimal("10000.00"), new BigDecimal("20000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("60000.00"), BigDecimal.ZERO, "Compra múltiple", List.of(item1, item2)
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));
        when(menorRepository.findById(10L)).thenReturn(Optional.of(menorMock));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("30000.00"), new BigDecimal("20000.00")),
                List.of(),
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("50000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);

        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(555L);
            return v;
        });

        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID(), u3 = UUID.randomUUID();
        UUID u4 = UUID.randomUUID(), u5 = UUID.randomUUID();
        when(uuidGeneratorService.generarUuidsUnicos(3)).thenReturn(List.of(u1, u2, u3));
        when(uuidGeneratorService.generarUuidsUnicos(2)).thenReturn(List.of(u4, u5));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VentaResponseDTO response = ventaService.procesarVenta(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(555L, response.id());
        assertEquals(2, response.detalles().size());
        assertEquals(5, response.tickets().size(), "Deben generarse exactamente 5 tickets para 3 + 2 unidades vendidas");

        verify(uuidGeneratorService).generarUuidsUnicos(3);
        verify(uuidGeneratorService).generarUuidsUnicos(2);
        verify(ticketRepository).saveAll(argThat((List<Ticket> tickets) -> tickets.size() == 5));
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si el monto recibido es menor al monto total")
    void testProcesarVenta_MontoInsuficiente() {
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.TIEMPO, 100L, null, 1, new BigDecimal("25000.00"), new BigDecimal("25000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("25000.00")),
                List.of(),
                new BigDecimal("25000.00"),
                BigDecimal.ZERO,
                new BigDecimal("25000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("25000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ventaService.procesarVenta(request, authentication));
        assertTrue(ex.getMessage().contains("inferior al total neto"));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si el turno de caja especificado está CERRADO")
    void testProcesarVenta_TurnoCerrado() {
        turnoCajaMock.setEstado(EstadoTurnoCaja.CERRADO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));

        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.TIEMPO, 100L, null, 1, new BigDecimal("10000.00"), null
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ventaService.procesarVenta(request, authentication));
        assertTrue(ex.getMessage().contains("no se encuentra ABIERTO"));
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si el menor no pertenece al cliente")
    void testProcesarVenta_MenorNoPerteneceACliente() {
        Cliente otroCliente = Cliente.builder().id(99L).build();
        Menor menorAjeno = Menor.builder().id(88L).cliente(otroCliente).build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));
        when(menorRepository.findById(88L)).thenReturn(Optional.of(menorAjeno));

        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                20L, 88L, TipoTarifa.TIEMPO, 100L, null, 1, new BigDecimal("10000.00"), null
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ventaService.procesarVenta(request, authentication));
        assertTrue(ex.getMessage().contains("no está vinculado al cliente acudiente"));
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si la atracción está inactiva")
    void testProcesarVenta_AtraccionInactiva() {
        atraccionMock.setEstado(EstadoAtraccion.INACTIVO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));

        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.TIEMPO, 100L, null, 1, new BigDecimal("10000.00"), null
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ventaService.procesarVenta(request, authentication));
        assertTrue(ex.getMessage().contains("no se encuentra activa"));
    }

    @Test
    @DisplayName("Garantía de inmutabilidad: lanzar ReglaNegocioException si se intenta modificar venta congelada (CA-03)")
    void testInmutabilidadVentaCongelada() {
        Venta ventaCongelada = Venta.builder()
                .id(1L)
                .congelada(true)
                .estadoVenta(EstadoVenta.PAGADO)
                .build();

        DetalleVenta detalle = DetalleVenta.builder()
                .id(10L)
                .venta(ventaCongelada)
                .cantidad(2)
                .precioUnitario(new BigDecimal("10000.00"))
                .build();

        // 1. Intentar addDetalle en venta congelada existente
        assertThrows(ReglaNegocioException.class, () -> ventaCongelada.addDetalle(detalle));

        // 2. Intentar removeDetalle en venta congelada existente
        assertThrows(ReglaNegocioException.class, () -> ventaCongelada.removeDetalle(detalle));
    }

    @Test
    @DisplayName("Seguridad OWASP A01: Taquillero no puede registrar ventas en el turno de caja de otro cajero")
    void testProcesarVenta_TaquilleroEnTurnoAjeno_LanzaAccessDeniedException() {
        Usuario taquillero = Usuario.builder()
                .id(2L)
                .correo("taquillero@test.com")
                .rol(Rol.builder().id(2L).nombre(com.software.territoriodeninos.util.NombreRol.TAQUILLERO).build())
                .build();
        turnoCajaMock.setUsuarioId(99L); // Turno de otro usuario

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("taquillero@test.com");
        when(authentication.getName()).thenReturn("taquillero@test.com");
        when(usuarioRepository.findByCorreo("taquillero@test.com")).thenReturn(Optional.of(taquillero));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));

        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.TIEMPO, 100L, null, 1, new BigDecimal("10000.00"), null
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        assertThrows(AccessDeniedException.class, () -> ventaService.procesarVenta(request, authentication));
    }

    @Test
    @DisplayName("listarVentasPorTurno: Lanza RecursoNoEncontradoException si el turno no existe")
    void testListarVentasPorTurno_TurnoNoExiste_LanzaRecursoNoEncontradoException() {
        when(turnoCajaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> ventaService.listarVentasPorTurno(999L, authentication));
    }

    @Test
    @DisplayName("Seguridad OWASP A01: Taquillero no puede consultar ventas del turno de otro cajero")
    void testListarVentasPorTurno_TaquilleroConsultaTurnoAjeno_LanzaAccessDeniedException() {
        Usuario taquillero = Usuario.builder()
                .id(2L)
                .correo("taquillero@test.com")
                .rol(Rol.builder().id(2L).nombre(com.software.territoriodeninos.util.NombreRol.TAQUILLERO).build())
                .build();
        turnoCajaMock.setUsuarioId(99L);

        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("taquillero@test.com");
        when(authentication.getName()).thenReturn("taquillero@test.com");
        when(usuarioRepository.findByCorreo("taquillero@test.com")).thenReturn(Optional.of(taquillero));

        assertThrows(AccessDeniedException.class, () -> ventaService.listarVentasPorTurno(5L, authentication));
    }

    @Test
    @DisplayName("Aforo: Múltiples ítems de MINUTO_BASE acumulan y validan capacidad agrupada por atracción")
    void testProcesarVenta_MultiplesMinutoBase_ValidaCapacidadAcumulada() {
        DetalleVentaRequestDTO item1 = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.MINUTO_BASE, null, null, 2, new BigDecimal("1000.00"), new BigDecimal("2000.00")
        );
        DetalleVentaRequestDTO item2 = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.MINUTO_BASE, null, null, 3, new BigDecimal("1000.00"), new BigDecimal("3000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("10000.00"), BigDecimal.ZERO, null, List.of(item1, item2)
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("2000.00"), new BigDecimal("3000.00")),
                List.of(),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setId(777L);
            return v;
        });
        when(uuidGeneratorService.generarUuidsUnicos(anyInt())).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        ventaService.procesarVenta(request, authentication);

        // Se verifica que la capacidad se validó para la suma acumulada (2 + 3 = 5 tickets)
        verify(aforoValidatorService).validarCapacidadAtraccion(20L, 5);
        verify(atraccionService).invalidarCacheCatalogo();
    }

    @Test
    @DisplayName("V-06: Aforo multitarifa: Ítems mixtos (MINUTO_BASE + TIEMPO) acumulan aforo en la misma atracción")
    void testProcesarVenta_TarifasMixtas_ValidaCapacidadAcumulada() {
        DetalleVentaRequestDTO itemMinuto = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.MINUTO_BASE, null, null, 2, new BigDecimal("1000.00"), new BigDecimal("2000.00")
        );
        DetalleVentaRequestDTO itemTiempo = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.TIEMPO, 101L, null, 3, new BigDecimal("5000.00"), new BigDecimal("15000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemMinuto, itemTiempo)
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(turnoCajaRepository.findById(5L)).thenReturn(Optional.of(turnoCajaMock));
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(atraccionMock));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("2000.00"), new BigDecimal("15000.00")),
                List.of(),
                new BigDecimal("17000.00"),
                BigDecimal.ZERO,
                new BigDecimal("17000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("17000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setId(888L);
            return v;
        });
        when(uuidGeneratorService.generarUuidsUnicos(anyInt())).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        ventaService.procesarVenta(request, authentication);

        // Se verifica que la capacidad se validó para la suma total de boletas sin importar tarifa (2 + 3 = 5 tickets)
        verify(aforoValidatorService).validarCapacidadAtraccion(20L, 5);
    }

    @Test
    @DisplayName("V-07: Descuento manual por cajero no administrador es rechazado")
    void testProcesarVenta_DescuentoManualSinRolAdmin_LanzaReglaNegocioException() {
        DetalleVentaRequestDTO item = new DetalleVentaRequestDTO(
                20L, null, TipoTarifa.MINUTO_BASE, null, null, 1, new BigDecimal("1000.00"), new BigDecimal("1000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 5L, MetodoPago.EFECTIVO, new BigDecimal("1000.00"), new BigDecimal("500.00"), null, List.of(item)
        );

        // authentication con rol TAQUILLERO
        org.springframework.security.core.Authentication authTaquillero = mock(org.springframework.security.core.Authentication.class);
        doReturn(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TAQUILLERO")))
                .when(authTaquillero).getAuthorities();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                ventaService.procesarVenta(request, authTaquillero)
        );
        assertTrue(ex.getMessage().contains("Los cajeros no tienen autorización para aplicar descuentos manuales"));
    }
}
