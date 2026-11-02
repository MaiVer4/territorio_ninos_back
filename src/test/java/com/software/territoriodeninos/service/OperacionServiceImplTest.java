package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.*;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperacionServiceImplTest - Pruebas del servicio de operación, asignación de estación y validación QR (HU-24 a HU-26, HU-34)")
class OperacionServiceImplTest {

    @Mock
    private AsignacionOperarioRepository asignacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AtraccionRepository atraccionRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private SesionJuegoRepository sesionJuegoRepository;
    @Mock
    private DetalleVentaRepository detalleVentaRepository;
    @Mock
    private LogEscaneoQrRepository logEscaneoQrRepository;
    @Mock
    private LogIncidentePausaRepository logIncidentePausaRepository;
    @Mock
    private TarifaTiempoRepository tarifaTiempoRepository;
    @Mock
    private TarifaEventoRepository tarifaEventoRepository;
    @Mock
    private OperacionWebSocketPublisher webSocketPublisher;
    @Mock
    private AuditoriaOperacionService auditoriaOperacionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OperacionServiceImpl operacionService;

    private Usuario operario;
    private Usuario admin;
    private Rol rolOperario;
    private Rol rolAdmin;
    private Atraccion atraccion;
    private Atraccion otraAtraccion;
    private AsignacionOperario asignacion;
    private Cliente cliente;
    private Menor menor;
    private Venta venta;
    private DetalleVenta detalleVenta;
    private Ticket ticket;
    private UUID ticketUuid;

    @BeforeEach
    void setUp() {
        ticketUuid = UUID.randomUUID();

        rolOperario = Rol.builder()
                .id(2L)
                .nombre(NombreRol.OPERARIO)
                .build();

        rolAdmin = Rol.builder()
                .id(1L)
                .nombre(NombreRol.ADMINISTRADOR)
                .build();

        operario = Usuario.builder()
                .id(5L)
                .nombre("Carlos Operario")
                .correo("carlos@territoriodeninos.com")
                .rol(rolOperario)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        admin = Usuario.builder()
                .id(1L)
                .nombre("Admin General")
                .correo("admin@territoriodeninos.com")
                .rol(rolAdmin)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        atraccion = Atraccion.builder()
                .id(10L)
                .nombre("Piscina de Pelotas")
                .descripcion("Atracción acuática de pelotas")
                .tipo(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(20)
                .duracionMinutosBase(15)
                .toleranciaMinutos(5)
                .caidasLimite(3)
                .edadMinima(3)
                .edadMaxima(10)
                .estado(EstadoAtraccion.ACTIVO)
                .build();

        otraAtraccion = Atraccion.builder()
                .id(20L)
                .nombre("Toro Mecánico")
                .descripcion("Atracción por evento")
                .tipo(TipoAtraccion.EVENTOS_CAIDAS)
                .capacidadMaxima(5)
                .duracionMinutosBase(5)
                .toleranciaMinutos(2)
                .caidasLimite(3)
                .edadMinima(6)
                .edadMaxima(14)
                .estado(EstadoAtraccion.ACTIVO)
                .build();

        asignacion = AsignacionOperario.builder()
                .id(100L)
                .usuario(operario)
                .atraccion(atraccion)
                .fechaAsignacion(LocalDate.now())
                .horaInicio(LocalDateTime.now().minusHours(2))
                .estado(EstadoAsignacion.ACTIVA)
                .build();

        cliente = Cliente.builder()
                .id(1L)
                .numeroDocumento("1094988776")
                .nombres("Valentina")
                .apellidos("Restrepo")
                .telefono("3127894561")
                .correo("valen@mail.com")
                .build();

        menor = Menor.builder()
                .id(15L)
                .numeroDocumento("1234567890")
                .nombreCompleto("Sofia Restrepo")
                .edad(5)
                .fechaNacimiento(LocalDate.now().minusYears(5))
                .cliente(cliente)
                .build();

        venta = Venta.builder()
                .id(50L)
                .cliente(cliente)
                .montoTotal(new BigDecimal("30000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.PAGADO)
                .build();

        detalleVenta = DetalleVenta.builder()
                .id(200L)
                .venta(venta)
                .atraccion(atraccion)
                .menor(menor)
                .tipoTarifa(TipoTarifa.TIEMPO)
                .tarifaTiempoId(1L)
                .subtotal(new BigDecimal("30000.00"))
                .build();

        ticket = Ticket.builder()
                .uuidCodigo(ticketUuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now().minusMinutes(15))
                .build();
    }

    // =========================================================
    // Pruebas HU-24.01 a HU-24.03: Obtener Estación Activa
    // =========================================================

    @Test
    @DisplayName("obtenerEstacionActiva - Retorna DTO con aforo y datos completos cuando operario tiene turno activo")
    void obtenerEstacionActiva_exito() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any()))
                .thenReturn(6);

        EstacionTrabajoDTO dto = operacionService.obtenerEstacionActiva(authentication);

        assertNotNull(dto);
        assertEquals(100L, dto.asignacionId());
        assertEquals("Carlos Operario", dto.operarioNombre());
        assertEquals(10L, dto.atraccionId());
        assertEquals("Piscina de Pelotas", dto.atraccionNombre());
        assertEquals(20, dto.capacidadMaxima());
        assertEquals(6, dto.aforoActual());
        assertEquals(14, dto.cuposDisponibles());
        assertEquals("3 a 10 años", dto.rangoEdad());
    }

    @Test
    @DisplayName("obtenerEstacionActiva - Lanza ReglaNegocioException si el usuario no está autenticado")
    void obtenerEstacionActiva_usuarioNoAutenticado_lanzaExcepcion() {
        assertThrows(ReglaNegocioException.class, () -> operacionService.obtenerEstacionActiva(null));
    }

    @Test
    @DisplayName("obtenerEstacionActiva - Lanza RecursoNoEncontradoException si no tiene estación asignada activa")
    void obtenerEstacionActiva_sinAsignacion_lanza404() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.empty());
        when(asignacionRepository.findByUsuarioIdAndEstado(eq(5L), eq(EstadoAsignacion.ACTIVA)))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> operacionService.obtenerEstacionActiva(authentication));
    }

    // =========================================================
    // Pruebas HU-24.04: Intercepción de Estación No Asignada
    // =========================================================

    @Test
    @DisplayName("escanearTicket - Lanza EstacionNoAutorizadaException (403) si el operario escanea otra atracción (HU-24.04)")
    void escanearTicket_operarioOtraEstacion_lanza403() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        // Intenta escanear para "otraAtraccion" (ID 20), pero su asignación es para "atraccion" (ID 10)
        when(atraccionRepository.findById(20L)).thenReturn(Optional.of(otraAtraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 20L, LocalDateTime.now());

        assertThrows(EstacionNoAutorizadaException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );
    }

    // =========================================================
    // Pruebas HU-34.01 y HU-34.02: Control de Aforo Máximo
    // =========================================================

    @Test
    @DisplayName("escanearTicket - Lanza AforoCompletoException (409) si la atracción está al 100% de aforo")
    void escanearTicket_aforoLleno_lanza409() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        // Capacidad es 20, simulamos 20 sesiones activas
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any()))
                .thenReturn(20);

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        assertThrows(AforoCompletoException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );

        // Verifica auditoría de log de rechazo
        verify(logEscaneoQrRepository, atLeastOnce()).save(any(LogEscaneoQr.class));
    }

    // =========================================================
    // Pruebas HU-26.03: Validación Algorítmica de Estado del Ticket
    // =========================================================

    @Test
    @DisplayName("escanearTicket - Lanza RecursoNoEncontradoException (404) si el ticket UUID no existe")
    void escanearTicket_ticketNoEncontrado_lanza404() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.empty());

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        assertThrows(RecursoNoEncontradoException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );

        verify(logEscaneoQrRepository, atLeastOnce()).save(any(LogEscaneoQr.class));
    }

    @Test
    @DisplayName("escanearTicket - Lanza TicketInvalidoOperacionException con CONFLICT (409) si el ticket está CONSUMIDO")
    void escanearTicket_ticketConsumido_lanza409() {
        ticket.setEstadoTicket(EstadoTicket.CONSUMIDO);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        TicketInvalidoOperacionException ex = assertThrows(TicketInvalidoOperacionException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(logEscaneoQrRepository, atLeastOnce()).save(any(LogEscaneoQr.class));
    }

    // =========================================================
    // Pruebas HU-26.04: Coincidencia Estricta de Atracción
    // =========================================================

    @Test
    @DisplayName("escanearTicket - Lanza TicketInvalidoOperacionException con 422 si el ticket es de otra atracción (HU-26.04)")
    void escanearTicket_atraccionDistinta_lanza422() {
        // Ticket fue comprado para otra atracción
        detalleVenta.setAtraccion(otraAtraccion);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        TicketInvalidoOperacionException ex = assertThrows(TicketInvalidoOperacionException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertTrue(ex.getMessage().contains("Toro Mecánico"));
    }

    // =========================================================
    // Pruebas HU-26.05: Rango de Edad del Menor
    // =========================================================

    @Test
    @DisplayName("escanearTicket - Lanza TicketInvalidoOperacionException con 422 si el menor es menor que edadMinima")
    void escanearTicket_menorMenorDeEdadMinima_lanza422() {
        // Atracción requiere mínimo 3 años, menor tiene 2 años
        menor.setEdad(2);
        menor.setFechaNacimiento(LocalDate.now().minusYears(2));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        TicketInvalidoOperacionException ex = assertThrows(TicketInvalidoOperacionException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertTrue(ex.getMessage().contains("edad mínima requerida"));
    }

    @Test
    @DisplayName("escanearTicket - Lanza TicketInvalidoOperacionException con 422 si el menor supera edadMaxima")
    void escanearTicket_menorMayorDeEdadMaxima_lanza422() {
        // Atracción permite máximo 10 años, menor tiene 12 años
        menor.setEdad(12);
        menor.setFechaNacimiento(LocalDate.now().minusYears(12));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        TicketInvalidoOperacionException ex = assertThrows(TicketInvalidoOperacionException.class, () ->
                operacionService.escanearTicket(request, authentication, "192.168.1.100")
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertTrue(ex.getMessage().contains("supera la edad máxima permitida"));
    }

    // =========================================================
    // Pruebas Éxito Validación y Auditoría Log (HU-25.03, HU-26.02)
    // =========================================================

    @Test
    @DisplayName("escanearTicket - Éxito con tarifa de tiempo retorna DTO y registra log en BD (HU-25.03)")
    void escanearTicket_exito_registraLog() {
        TarifaTiempo tarifaTiempo = TarifaTiempo.builder()
                .id(1L)
                .duracionMinutos(30)
                .precio(new BigDecimal("30000.00"))
                .build();

        when(tarifaTiempoRepository.findById(1L)).thenReturn(Optional.of(tarifaTiempo));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(3);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        EscaneoQrRequestDTO request = new EscaneoQrRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        ValidacionTicketResponseDTO response = operacionService.escanearTicket(request, authentication, "192.168.1.100");

        assertNotNull(response);
        assertTrue(response.valido());
        assertEquals(ticketUuid, response.uuidTicket());
        assertEquals("Piscina de Pelotas", response.atraccionNombre());
        assertEquals("Sofia Restrepo", response.menorNombre());
        assertEquals(5, response.menorEdad());
        assertEquals("Valentina Restrepo", response.acudienteNombre());
        assertEquals(30, response.minutosAutorizados());

        verify(logEscaneoQrRepository).save(argThat(log ->
                ticketUuid.equals(log.getUuidLeido()) &&
                        "VALIDADO_EXITOSO".equals(log.getResultado()) &&
                        "192.168.1.100".equals(log.getDireccionIp())
        ));
    }

    // =========================================================
    // Pruebas HU-34.03: Consulta de Aforo Actual
    // =========================================================

    @Test
    @DisplayName("obtenerAforoActual - Calcula cupos y porcentaje de aforo correctamente (HU-34.03)")
    void obtenerAforoActual_calculaMetricasCorrectamente() {
        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(14);

        AforoActualDTO dto = operacionService.obtenerAforoActual(10L);

        assertNotNull(dto);
        assertEquals(10L, dto.atraccionId());
        assertEquals(20, dto.capacidadMaxima());
        assertEquals(14, dto.ocupacionActiva());
        assertEquals(6, dto.cuposLibres());
        assertEquals(70.0, dto.porcentajeOcupacion());
        assertFalse(dto.aforoCompleto());
    }

    @Test
    @DisplayName("obtenerAforoActual - Lanza RecursoNoEncontradoException si atracción no existe")
    void obtenerAforoActual_atraccionNoExiste_lanza404() {
        when(atraccionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> operacionService.obtenerAforoActual(999L));
    }

    // =========================================================
    // Pruebas Fase 2: Inicio, Cola Activa, Pausas y Reanudación
    // =========================================================

    @Test
    @DisplayName("iniciarSesion - Cambia ticket a EN_USO, guarda sesión, calcula horaFin y publica por WebSocket (HU-27.05)")
    void iniciarSesion_exito() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(4);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        when(sesionJuegoRepository.save(any(SesionJuego.class))).thenAnswer(invocation -> {
            SesionJuego s = invocation.getArgument(0);
            s.setId(500L);
            return s;
        });

        InicioSesionRequestDTO request = new InicioSesionRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        SesionJuegoResponseDTO response = operacionService.iniciarSesion(request, authentication);

        assertNotNull(response);
        assertEquals(500L, response.sesionId());
        assertEquals("Piscina de Pelotas", response.atraccionNombre());
        assertEquals("Sofia Restrepo", response.menorNombre());
        assertEquals("EN_CURSO", response.estadoSesion());
        assertEquals(ticketUuid, response.ticketUuid());

        assertEquals(EstadoTicket.EN_USO, ticket.getEstadoTicket());
        verify(ticketRepository).save(ticket);
        verify(sesionJuegoRepository).save(any(SesionJuego.class));
        verify(webSocketPublisher).publicarNuevoIngreso(eq(10L), any(SesionJuegoResponseDTO.class), any(ColaAtraccionResponseDTO.class));
    }

    @Test
    @DisplayName("iniciarSesion - Lanza AforoCompletoException (409) si la atracción está al 100% de aforo")
    void iniciarSesion_aforoLleno_lanza409() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(20);

        InicioSesionRequestDTO request = new InicioSesionRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        assertThrows(AforoCompletoException.class, () -> operacionService.iniciarSesion(request, authentication));
    }

    @Test
    @DisplayName("iniciarSesion - Lanza TicketInvalidoOperacionException con CONFLICT (409) si el ticket no está en PAGADO")
    void iniciarSesion_ticketNoPagado_lanza409() {
        ticket.setEstadoTicket(EstadoTicket.CONSUMIDO);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        InicioSesionRequestDTO request = new InicioSesionRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        assertThrows(TicketInvalidoOperacionException.class, () -> operacionService.iniciarSesion(request, authentication));
    }

    @Test
    @DisplayName("iniciarSesion - Lanza TicketInvalidoOperacionException con 422 si el ticket es de otra atracción")
    void iniciarSesion_atraccionDistinta_lanza422() {
        detalleVenta.setAtraccion(otraAtraccion);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(eq(10L), any())).thenReturn(5);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        InicioSesionRequestDTO request = new InicioSesionRequestDTO(ticketUuid, 10L, LocalDateTime.now());

        assertThrows(TicketInvalidoOperacionException.class, () -> operacionService.iniciarSesion(request, authentication));
    }

    @Test
    @DisplayName("obtenerCola - Retorna cola con sesiones ordenadas y métricas consolidadas (HU-28.02)")
    void obtenerCola_exito() {
        SesionJuego sesionActiva = SesionJuego.builder()
                .id(100L)
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaInicio(LocalDateTime.now().minusMinutes(5))
                .horaFin(LocalDateTime.now().plusMinutes(10))
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(eq(10L), any()))
                .thenReturn(List.of(sesionActiva));

        ColaAtraccionResponseDTO cola = operacionService.obtenerCola(10L);

        assertNotNull(cola);
        assertEquals(10L, cola.atraccionId());
        assertEquals("Piscina de Pelotas", cola.atraccionNombre());
        assertEquals(20, cola.capacidadMaxima());
        assertEquals(1, cola.ocupacionActiva());
        assertEquals(19, cola.cuposDisponibles());
        assertEquals(1, cola.sesionesActivas().size());
        assertEquals(100L, cola.sesionesActivas().get(0).sesionId());
    }

    @Test
    @DisplayName("pausarSesion - Congela tiempo restante, cambia estado a PAUSADA y persiste incidente de auditoría (HU-29.03)")
    void pausarSesion_exito() {
        LocalDateTime now = LocalDateTime.now();
        SesionJuego sesionEnCurso = SesionJuego.builder()
                .id(100L)
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaInicio(now.minusMinutes(5))
                .horaFin(now.plusSeconds(600))
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionEnCurso));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        PausaRequestDTO request = new PausaRequestDTO("Menor requiere ir al baño");

        SesionJuegoResponseDTO response = operacionService.pausarSesion(100L, request, authentication);

        assertNotNull(response);
        assertEquals("PAUSADA", response.estadoSesion());
        assertNotNull(response.tiempoRestantePausa());
        assertTrue(response.tiempoRestantePausa() > 0);

        assertEquals(EstadoSesionJuego.PAUSADA, sesionEnCurso.getEstadoSesion());
        verify(sesionJuegoRepository).save(sesionEnCurso);
        verify(logIncidentePausaRepository).save(any(LogIncidentePausa.class));
        verify(webSocketPublisher).publicarPausa(eq(10L), eq(100L), eq("Menor requiere ir al baño"), any(), any());
    }

    @Test
    @DisplayName("pausarSesion - Lanza ReglaNegocioException si la sesión no está en curso")
    void pausarSesion_sesionNoEnCurso_lanzaReglaNegocio() {
        SesionJuego sesionPausada = SesionJuego.builder()
                .id(100L)
                .atraccion(atraccion)
                .operario(operario)
                .estadoSesion(EstadoSesionJuego.PAUSADA)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionPausada));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        PausaRequestDTO request = new PausaRequestDTO("Menor requiere calzado");

        assertThrows(ReglaNegocioException.class, () -> operacionService.pausarSesion(100L, request, authentication));
    }

    @Test
    @DisplayName("reanudarSesion - Recalcula horaFin sin perder tiempo congelado y cierra auditoría de pausa (HU-29.04)")
    void reanudarSesion_exito() {
        LocalDateTime now = LocalDateTime.now();
        SesionJuego sesionPausada = SesionJuego.builder()
                .id(100L)
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaInicio(now.minusMinutes(10))
                .horaFin(now.plusMinutes(5))
                .duracionSegundos(900)
                .tiempoRestantePausa(450)
                .fechaPausa(now.minusMinutes(2))
                .estadoSesion(EstadoSesionJuego.PAUSADA)
                .build();

        LogIncidentePausa logPausa = LogIncidentePausa.builder()
                .id(1L)
                .sesionJuego(sesionPausada)
                .operario(operario)
                .motivo("Ir al baño")
                .fechaInicioPausa(now.minusMinutes(2))
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionPausada));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(logIncidentePausaRepository.findFirstBySesionJuegoIdAndFechaFinPausaIsNullOrderByFechaInicioPausaDesc(100L))
                .thenReturn(Optional.of(logPausa));

        SesionJuegoResponseDTO response = operacionService.reanudarSesion(100L, authentication);

        assertNotNull(response);
        assertEquals("EN_CURSO", response.estadoSesion());
        assertNull(sesionPausada.getTiempoRestantePausa());
        assertNull(sesionPausada.getFechaPausa());
        assertEquals(EstadoSesionJuego.EN_CURSO, sesionPausada.getEstadoSesion());

        // Verifica que la auditoría de pausa se haya cerrado
        assertNotNull(logPausa.getFechaFinPausa());
        assertNotNull(logPausa.getDuracionSegundos());

        verify(sesionJuegoRepository).save(sesionPausada);
        verify(logIncidentePausaRepository).save(logPausa);
        verify(webSocketPublisher).publicarReanudacion(eq(10L), eq(100L), any(), any());
    }

    @Test
    @DisplayName("reanudarSesion - Lanza ReglaNegocioException si la sesión no se encuentra pausada")
    void reanudarSesion_sesionNoPausada_lanzaReglaNegocio() {
        SesionJuego sesionEnCurso = SesionJuego.builder()
                .id(100L)
                .atraccion(atraccion)
                .operario(operario)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionEnCurso));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        assertThrows(ReglaNegocioException.class, () -> operacionService.reanudarSesion(100L, authentication));
    }

    @Test
    @DisplayName("pausaGeneral - Congela todas las sesiones activas en la atracción ante contingencia general (HU-29.05)")
    void pausaGeneral_exito() {
        LocalDateTime now = LocalDateTime.now();
        SesionJuego sesion1 = SesionJuego.builder()
                .id(101L)
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaFin(now.plusSeconds(300))
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        SesionJuego sesion2 = SesionJuego.builder()
                .id(102L)
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaFin(now.plusSeconds(600))
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(eq(10L), eq(List.of(EstadoSesionJuego.EN_CURSO))))
                .thenReturn(List.of(sesion1, sesion2));

        PausaGeneralRequestDTO request = new PausaGeneralRequestDTO("Lluvia fuerte y tormenta eléctrica");

        ColaAtraccionResponseDTO cola = operacionService.pausaGeneral(10L, request, authentication);

        assertNotNull(cola);
        assertEquals(EstadoSesionJuego.PAUSADA, sesion1.getEstadoSesion());
        assertEquals(EstadoSesionJuego.PAUSADA, sesion2.getEstadoSesion());

        verify(sesionJuegoRepository, times(2)).save(any(SesionJuego.class));
        verify(logIncidentePausaRepository, times(2)).save(any(LogIncidentePausa.class));
        verify(webSocketPublisher).publicarPausaGeneral(eq(10L), eq("Lluvia fuerte y tormenta eléctrica"), any());
    }

    // =========================================================
    // Pruebas Fase 3: Egreso Físico, Descuento Intentos y Sincronización Epoch
    // =========================================================

    @Test
    @DisplayName("registrarSalida - Finaliza sesión, marca ticket CONSUMIDO y libera aforo por WebSocket (HU-32.02 a HU-32.05)")
    void registrarSalida_exito() {
        LocalDateTime now = LocalDateTime.now();
        ticket.setEstadoTicket(EstadoTicket.EN_USO);

        SesionJuego sesionActiva = SesionJuego.builder()
                .id(100L)
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaInicio(now.minusMinutes(15))
                .horaFin(now)
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionActiva));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        SesionJuegoResponseDTO response = operacionService.registrarSalida(100L, authentication);

        assertNotNull(response);
        assertEquals("FINALIZADA", response.estadoSesion());
        assertEquals(EstadoSesionJuego.FINALIZADA, sesionActiva.getEstadoSesion());
        assertNotNull(sesionActiva.getFechaSalidaReal());

        // Verificación de actualización atómica del ticket a CONSUMIDO con fechaConsumo (HU-32.03)
        assertEquals(EstadoTicket.CONSUMIDO, ticket.getEstadoTicket());
        assertNotNull(ticket.getFechaConsumo());
        verify(ticketRepository).save(ticket);
        verify(sesionJuegoRepository).save(sesionActiva);

        // Verificación de emisión WebSocket de desocupación / liberación de aforo (HU-32.05)
        verify(webSocketPublisher).publicarSesionFinalizada(eq(10L), any(), any());
    }

    @Test
    @DisplayName("registrarSalida - Lanza ReglaNegocioException si la sesión ya se encuentra FINALIZADA")
    void registrarSalida_sesionYaFinalizada_lanzaReglaNegocio() {
        SesionJuego sesionFinalizada = SesionJuego.builder()
                .id(100L)
                .atraccion(atraccion)
                .operario(operario)
                .estadoSesion(EstadoSesionJuego.FINALIZADA)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@territoriodeninos.com");
        when(authentication.getName()).thenReturn("carlos@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("carlos@territoriodeninos.com")).thenReturn(Optional.of(operario));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionFinalizada));
        when(asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                eq(5L), eq(LocalDate.now()), eq(EstadoAsignacion.ACTIVA)
        )).thenReturn(Optional.of(asignacion));

        assertThrows(ReglaNegocioException.class, () -> operacionService.registrarSalida(100L, authentication));
    }

    @Test
    @DisplayName("descontarIntento - Descuenta un intento y continúa en curso si quedan intentos (HU-33.03)")
    void descontarIntento_exito_continua() {
        SesionJuego sesionToro = SesionJuego.builder()
                .id(100L)
                .ticket(ticket)
                .atraccion(otraAtraccion)
                .menor(menor)
                .operario(admin)
                .intentosTotales(3)
                .intentosRestantes(3)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("admin@territoriodeninos.com");
        when(authentication.getName()).thenReturn("admin@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("admin@territoriodeninos.com")).thenReturn(Optional.of(admin));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionToro));

        RegistroIntentoEventoDTO request = new RegistroIntentoEventoDTO(1, 25, "Primera ronda");

        SesionJuegoResponseDTO response = operacionService.descontarIntento(100L, request, authentication);

        assertNotNull(response);
        assertEquals(2, response.intentosRestantes());
        assertEquals(EstadoSesionJuego.EN_CURSO, sesionToro.getEstadoSesion());

        verify(sesionJuegoRepository).save(sesionToro);
        verify(webSocketPublisher).publicarColaActualizada(eq(20L), any());
    }

    @Test
    @DisplayName("descontarIntento - Finaliza automáticamente la sesión y marca ticket CONSUMIDO al llegar a 0 intentos (HU-33.04)")
    void descontarIntento_llegaACero_finalizaAutomaticamente() {
        ticket.setEstadoTicket(EstadoTicket.EN_USO);

        SesionJuego sesionUltimoIntento = SesionJuego.builder()
                .id(100L)
                .ticket(ticket)
                .atraccion(otraAtraccion)
                .menor(menor)
                .operario(admin)
                .intentosTotales(3)
                .intentosRestantes(1)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("admin@territoriodeninos.com");
        when(authentication.getName()).thenReturn("admin@territoriodeninos.com");
        when(usuarioRepository.findByCorreo("admin@territoriodeninos.com")).thenReturn(Optional.of(admin));

        when(sesionJuegoRepository.findById(100L)).thenReturn(Optional.of(sesionUltimoIntento));

        RegistroIntentoEventoDTO request = new RegistroIntentoEventoDTO(3, 18, "Tercera caída");

        SesionJuegoResponseDTO response = operacionService.descontarIntento(100L, request, authentication);

        assertNotNull(response);
        assertEquals(0, response.intentosRestantes());
        assertEquals("FINALIZADA", response.estadoSesion());
        assertEquals(EstadoSesionJuego.FINALIZADA, sesionUltimoIntento.getEstadoSesion());
        assertNotNull(sesionUltimoIntento.getFechaSalidaReal());

        assertEquals(EstadoTicket.CONSUMIDO, ticket.getEstadoTicket());
        assertNotNull(ticket.getFechaConsumo());
        verify(ticketRepository).save(ticket);
        verify(sesionJuegoRepository).save(sesionUltimoIntento);
        verify(webSocketPublisher).publicarSesionFinalizada(eq(20L), any(), any());
    }

    @Test
    @DisplayName("sincronizarReloj - Entrega epoch Unix oficial del servidor y cola recalibrada (HU-35.01, HU-35.02)")
    void sincronizarReloj_exito() {
        when(atraccionRepository.findById(10L)).thenReturn(Optional.of(atraccion));
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(eq(10L), any()))
                .thenReturn(List.of());

        SincronizacionRelojResponseDTO dto = operacionService.sincronizarReloj(10L);

        assertNotNull(dto);
        assertEquals(10L, dto.atraccionId());
        assertEquals("Piscina de Pelotas", dto.atraccionNombre());
        assertTrue(dto.serverEpochMillis() > 0);
        assertNotNull(dto.serverTimestamp());
        assertNotNull(dto.colaActualizada());
    }
}
