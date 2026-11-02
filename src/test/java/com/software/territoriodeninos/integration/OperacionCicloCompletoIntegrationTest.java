package com.software.territoriodeninos.integration;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.service.OperacionService;
import com.software.territoriodeninos.service.OperacionWebSocketPublisher;
import com.software.territoriodeninos.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("OperacionCicloCompletoIntegrationTest - Flujo de punta a punta: Validación -> Inicio -> Pausa -> Reanudación -> Salida / Evento")
class OperacionCicloCompletoIntegrationTest {

    @Autowired
    private OperacionService operacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private AtraccionRepository atraccionRepository;

    @Autowired
    private AsignacionOperarioRepository asignacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MenorRepository menorRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SesionJuegoRepository sesionJuegoRepository;

    @MockitoBean
    private OperacionWebSocketPublisher webSocketPublisher;

    private Usuario operario;
    private Atraccion atraccion;
    private Menor menor;
    private Ticket ticket;
    private UUID ticketUuid;
    private Authentication authOperario;

    @BeforeEach
    void setUp() {
        ticketUuid = UUID.randomUUID();

        Rol rolOperario = rolRepository.findByNombre(NombreRol.OPERARIO)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(NombreRol.OPERARIO).build()));

        operario = usuarioRepository.save(Usuario.builder()
                .nombre("Juan Perez Operario")
                .correo("operario.test." + UUID.randomUUID() + "@territorio.com")
                .password("$2a$10$abcdefghijklmnopqrstuvwxyz123456")
                .rol(rolOperario)
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build());

        atraccion = atraccionRepository.save(Atraccion.builder()
                .nombre("Piscina de Pelotas Test " + UUID.randomUUID())
                .descripcion("Atracción acuática")
                .tarifaMinuto(new BigDecimal("1000.00"))
                .tipo(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(10)
                .duracionMinutosBase(15)
                .toleranciaMinutos(5)
                .caidasLimite(3)
                .edadMinima(2)
                .edadMaxima(12)
                .estado(EstadoAtraccion.ACTIVO)
                .build());

        asignacionRepository.save(AsignacionOperario.builder()
                .usuario(operario)
                .atraccion(atraccion)
                .fechaAsignacion(LocalDate.now())
                .horaInicio(LocalDateTime.now().minusHours(1))
                .estado(EstadoAsignacion.ACTIVA)
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .numeroDocumento("9988776655")
                .nombres("Maria")
                .apellidos("Gomez")
                .correo("maria@test.com")
                .telefono("3001234567")
                .build());

        menor = menorRepository.save(Menor.builder()
                .nombreCompleto("Carlitos Gomez")
                .numeroDocumento("11998877")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .edad(6)
                .cliente(cliente)
                .build());

        TurnoCaja turnoCaja = turnoCajaRepository.save(TurnoCaja.builder()
                .usuarioId(operario.getId())
                .montoBase(new BigDecimal("100000.00"))
                .ventasEfectivoSistema(BigDecimal.ZERO)
                .fechaApertura(LocalDateTime.now())
                .estado(EstadoTurnoCaja.ABIERTO)
                .build());

        Venta venta = ventaRepository.save(Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("15000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.PAGADO)
                .fechaVenta(LocalDateTime.now())
                .build());

        DetalleVenta detalle = detalleVentaRepository.save(DetalleVenta.builder()
                .venta(venta)
                .atraccion(atraccion)
                .menor(menor)
                .cantidad(1)
                .precioUnitario(new BigDecimal("15000.00"))
                .subtotal(new BigDecimal("15000.00"))
                .tipoTarifa(TipoTarifa.TIEMPO)
                .build());

        ticket = ticketRepository.save(Ticket.builder()
                .uuidCodigo(ticketUuid)
                .detalleVenta(detalle)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build());

        authOperario = new UsernamePasswordAuthenticationToken(
                operario.getCorreo(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_OPERARIO"))
        );
    }

    @Test
    @DisplayName("Ciclo completo: Estación -> Escaneo QR -> Iniciar Sesión -> Pausar -> Reanudar -> Egreso físico")
    void cicloCompletoDeOperacion() {
        // 1. Obtener estación activa del operario (HU-24.03)
        EstacionTrabajoDTO estacion = operacionService.obtenerEstacionActiva(authOperario);
        assertNotNull(estacion);
        assertEquals(atraccion.getId(), estacion.atraccionId());
        assertEquals(10, estacion.capacidadMaxima());
        assertEquals(0, estacion.aforoActual());

        // 2. Escanear y validar código QR (HU-25.02, HU-26.03)
        EscaneoQrRequestDTO escaneoReq = new EscaneoQrRequestDTO(ticketUuid, atraccion.getId(), LocalDateTime.now());
        ValidacionTicketResponseDTO validacion = operacionService.escanearTicket(escaneoReq, authOperario, "127.0.0.1");
        assertTrue(validacion.valido());
        assertEquals("PAGADO", validacion.estadoActual());
        assertEquals(15, validacion.minutosAutorizados());

        // 3. Iniciar sesión formal de juego (HU-27.05)
        InicioSesionRequestDTO inicioReq = new InicioSesionRequestDTO(ticketUuid, atraccion.getId(), LocalDateTime.now());
        SesionJuegoResponseDTO sesionIniciada = operacionService.iniciarSesion(inicioReq, authOperario);
        assertNotNull(sesionIniciada);
        assertEquals("EN_CURSO", sesionIniciada.estadoSesion());

        // Verificar que el ticket pasó a EN_USO
        Ticket ticketEnUso = ticketRepository.findByUuidCodigo(ticketUuid).orElseThrow();
        assertEquals(EstadoTicket.EN_USO, ticketEnUso.getEstadoTicket());

        // 4. Pausar sesión por contingencia (HU-29.03)
        PausaRequestDTO pausaReq = new PausaRequestDTO("Menor requiere ajustar calzado");
        SesionJuegoResponseDTO sesionPausada = operacionService.pausarSesion(sesionIniciada.sesionId(), pausaReq, authOperario);
        assertEquals("PAUSADA", sesionPausada.estadoSesion());
        assertNotNull(sesionPausada.tiempoRestantePausa());

        // 5. Reanudar sesión (HU-29.04)
        SesionJuegoResponseDTO sesionReanudada = operacionService.reanudarSesion(sesionIniciada.sesionId(), authOperario);
        assertEquals("EN_CURSO", sesionReanudada.estadoSesion());
        assertNull(sesionReanudada.tiempoRestantePausa());

        // 6. Consultar aforo en tiempo real (HU-34.03)
        AforoActualDTO aforo = operacionService.obtenerAforoActual(atraccion.getId());
        assertEquals(1, aforo.ocupacionActiva());
        assertEquals(9, aforo.cuposLibres());

        // 7. Sincronizar reloj de terminal móvil (HU-35.01)
        SincronizacionRelojResponseDTO sync = operacionService.sincronizarReloj(atraccion.getId());
        assertTrue(sync.serverEpochMillis() > 0);
        assertEquals(1, sync.colaActualizada().ocupacionActiva());

        // 8. Registrar egreso físico formal (HU-32.02, HU-32.03, HU-32.04)
        SesionJuegoResponseDTO sesionFinalizada = operacionService.registrarSalida(sesionIniciada.sesionId(), authOperario);
        assertEquals("FINALIZADA", sesionFinalizada.estadoSesion());

        // Ticket debe quedar CONSUMIDO con fecha de consumo atómica
        Ticket ticketConsumido = ticketRepository.findByUuidCodigo(ticketUuid).orElseThrow();
        assertEquals(EstadoTicket.CONSUMIDO, ticketConsumido.getEstadoTicket());
        assertNotNull(ticketConsumido.getFechaConsumo());

        // Aforo debe haber liberado el cupo
        AforoActualDTO aforoFinal = operacionService.obtenerAforoActual(atraccion.getId());
        assertEquals(0, aforoFinal.ocupacionActiva());
        assertEquals(10, aforoFinal.cuposLibres());
    }
}
