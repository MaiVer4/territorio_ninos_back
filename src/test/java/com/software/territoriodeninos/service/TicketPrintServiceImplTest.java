package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.ReimpresionRequestDTO;
import com.software.territoriodeninos.dto.TicketPrintDataDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.NombreRol;
import com.software.territoriodeninos.util.TicketPrintFormatter;
import com.software.territoriodeninos.util.TipoTarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketPrintServiceImplTest - Pruebas unitarias para el servicio de preparación de impresión térmica (HU-21.01, HU-21.03)")
class TicketPrintServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ConfiguracionParqueService configuracionParqueService;

    @Mock
    private TicketPrintFormatter ticketPrintFormatter;

    @Mock
    private TarifaTiempoRepository tarifaTiempoRepository;

    @Mock
    private TarifaEventoRepository tarifaEventoRepository;

    @Mock
    private LogReimpresionRepository logReimpresionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @Mock
    private AuditoriaOperacionService auditoriaOperacionService;

    @InjectMocks
    private TicketPrintServiceImpl ticketPrintService;

    private UUID ticketUuid;
    private Ticket ticket;
    private DetalleVenta detalleVenta;
    private Atraccion atraccion;
    private Menor menor;
    private ConfiguracionParqueDTO configuracionParqueDTO;

    @BeforeEach
    void setUp() {
        ticketUuid = UUID.fromString("93957dbe-823c-4b57-b5e5-629857541d06");

        atraccion = Atraccion.builder()
                .id(1L)
                .nombre("PISCINA DE PELOTAS")
                .duracionMinutosBase(15)
                .caidasLimite(3)
                .build();

        menor = Menor.builder()
                .id(5L)
                .nombreCompleto("Sofia Restrepo")
                .build();

        TurnoCaja turnoActivo = TurnoCaja.builder()
                .id(1L)
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        Venta venta = Venta.builder()
                .id(100L)
                .turnoCaja(turnoActivo)
                .build();

        detalleVenta = DetalleVenta.builder()
                .id(10L)
                .venta(venta)
                .atraccion(atraccion)
                .menor(menor)
                .tipoTarifa(TipoTarifa.TIEMPO)
                .tarifaTiempoId(101L)
                .precioUnitario(new BigDecimal("30000.00"))
                .cantidad(1)
                .build();

        ticket = Ticket.builder()
                .uuidCodigo(ticketUuid)
                .detalleVenta(detalleVenta)
                .menor(menor)
                .estadoTicket(EstadoTicket.PAGADO)
                .fechaEmision(LocalDateTime.now())
                .build();

        configuracionParqueDTO = ConfiguracionParqueDTO.builder()
                .nombreParque("Territorio de Niños")
                .nit("901.458.789-2")
                .razonSocial("Territorio de Niños S.A.S.")
                .pieTicketLegal("Términos del parque")
                .build();
    }

    @Test
    @DisplayName("generarPayloadImpresion - Resuelve correctamente datos de ticket y configuración (HU-21.01)")
    void generarPayloadImpresion_exito() {
        TarifaTiempo tarifaTiempo = TarifaTiempo.builder()
                .id(101L)
                .duracionMinutos(30)
                .precio(new BigDecimal("30000.00"))
                .build();

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));
        when(tarifaTiempoRepository.findById(101L)).thenReturn(Optional.of(tarifaTiempo));
        when(configuracionParqueService.obtenerConfiguracion()).thenReturn(configuracionParqueDTO);
        when(ticketPrintFormatter.formatear(any(TicketPrintDataDTO.class), eq(configuracionParqueDTO)))
                .thenReturn("ESC/POS MOCK PAYLOAD");

        String resultado = ticketPrintService.generarPayloadImpresion(ticketUuid);

        assertThat(resultado).isEqualTo("ESC/POS MOCK PAYLOAD");

        ArgumentCaptor<TicketPrintDataDTO> captor = ArgumentCaptor.forClass(TicketPrintDataDTO.class);
        verify(ticketPrintFormatter).formatear(captor.capture(), eq(configuracionParqueDTO));

        TicketPrintDataDTO data = captor.getValue();
        assertThat(data.uuidTicket()).isEqualTo(ticketUuid);
        assertThat(data.nombreAtraccion()).isEqualTo("PISCINA DE PELOTAS");
        assertThat(data.nombreMenor()).isEqualTo("Sofia Restrepo");
        assertThat(data.tiempoOModalidad()).isEqualTo("30 min");
        assertThat(data.precioUnitario()).isEqualByComparingTo("30000.00");
    }

    @Test
    @DisplayName("generarPayloadImpresion - Lanza RecursoNoEncontradoException si el ticket no existe")
    void generarPayloadImpresion_ticketNoEncontrado_lanzaRecursoNoEncontradoException() {
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketPrintService.generarPayloadImpresion(ticketUuid))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ticketUuid.toString());
    }

    @Test
    @DisplayName("generarPayloadImpresion - Lanza ReglaNegocioException si el UUID es nulo")
    void generarPayloadImpresion_uuidNulo_lanzaReglaNegocioException() {
        assertThatThrownBy(() -> ticketPrintService.generarPayloadImpresion(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("UUID del ticket es obligatorio");
    }

    @Test
    @DisplayName("V-04: generarPayloadImpresion - Rechaza tickets en estado CONSUMIDO con ConflictoException")
    void generarPayloadImpresion_ticketConsumido_lanzaConflictoException() {
        ticket.setEstadoTicket(EstadoTicket.CONSUMIDO);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketPrintService.generarPayloadImpresion(ticketUuid))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("no admite impresión directa");
    }

    @Test
    @DisplayName("V-04: generarPayloadImpresion - Rechaza tickets en estado ANULADO con ConflictoException")
    void generarPayloadImpresion_ticketAnulado_lanzaConflictoException() {
        ticket.setEstadoTicket(EstadoTicket.ANULADO);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketPrintService.generarPayloadImpresion(ticketUuid))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("no admite impresión directa");
    }

    @Test
    @DisplayName("V-04: generarPayloadImpresion - Rechaza tickets cuyo turno no está ABIERTO")
    void generarPayloadImpresion_turnoNoAbierto_lanzaConflictoException() {
        TurnoCaja turnoCerrado = TurnoCaja.builder().id(1L).estado(EstadoTurnoCaja.CERRADO).build();
        Venta ventaTurnoCerrado = Venta.builder().turnoCaja(turnoCerrado).build();
        detalleVenta.setVenta(ventaTurnoCerrado);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketPrintService.generarPayloadImpresion(ticketUuid))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("no pertenece a un turno de caja actualmente activo");
    }

    @Test
    @DisplayName("generarPayloadImpresion - Resuelve modalidad para tarifa de tipo EVENTO")
    void generarPayloadImpresion_tarifaEvento_resuelveIntentos() {
        detalleVenta.setTipoTarifa(TipoTarifa.EVENTO);
        detalleVenta.setTarifaEventoId(202L);

        TarifaEvento tarifaEvento = TarifaEvento.builder()
                .id(202L)
                .intentos(5)
                .build();

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));
        when(tarifaEventoRepository.findById(202L)).thenReturn(Optional.of(tarifaEvento));
        when(configuracionParqueService.obtenerConfiguracion()).thenReturn(configuracionParqueDTO);
        when(ticketPrintFormatter.formatear(any(), any())).thenReturn("MOCK");

        ticketPrintService.generarPayloadImpresion(ticketUuid);

        ArgumentCaptor<TicketPrintDataDTO> captor = ArgumentCaptor.forClass(TicketPrintDataDTO.class);
        verify(ticketPrintFormatter).formatear(captor.capture(), any());
        assertThat(captor.getValue().tiempoOModalidad()).isEqualTo("5 intentos");
    }

    @Test
    @DisplayName("HU-22.04 / HU-22.05: Reimpresión exitosa - genera payload con marca de reimpresión y guarda auditoría")
    void reimprimirTicket_exito() {
        TurnoCaja turno = TurnoCaja.builder()
                .id(99L)
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        Venta venta = Venta.builder()
                .id(50L)
                .turnoCaja(turno)
                .build();
        detalleVenta.setVenta(venta);

        Rol rolTaquillero = Rol.builder()
                .id(1L)
                .nombre(NombreRol.TAQUILLERO)
                .build();
        Usuario cajero = Usuario.builder()
                .id(7L)
                .correo("cajero@territoriodeninos.com")
                .rol(rolTaquillero)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cajero@territoriodeninos.com", "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        );

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(7L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turno));
        when(configuracionParqueService.obtenerConfiguracion()).thenReturn(configuracionParqueDTO);
        when(ticketPrintFormatter.formatear(any(TicketPrintDataDTO.class), eq(configuracionParqueDTO)))
                .thenReturn("ESC/POS MOCK REIMPRESION");

        ReimpresionRequestDTO request = new ReimpresionRequestDTO("Atasco de papel en la impresora");

        String payload = ticketPrintService.reimprimirTicket(ticketUuid, request, auth);

        assertThat(payload).isEqualTo("ESC/POS MOCK REIMPRESION");

        // Verificar que se guardó en log_reimpresion (HU-22.02, CA-03)
        ArgumentCaptor<LogReimpresion> logCaptor = ArgumentCaptor.forClass(LogReimpresion.class);
        verify(logReimpresionRepository).save(logCaptor.capture());
        LogReimpresion logGuardado = logCaptor.getValue();
        assertThat(logGuardado.getTicket()).isEqualTo(ticket);
        assertThat(logGuardado.getUsuario()).isEqualTo(cajero);
        assertThat(logGuardado.getMotivo()).isEqualTo("Atasco de papel en la impresora");
        assertThat(logGuardado.getFechaReimpresion()).isNotNull();

        // Verificar que printData tiene esReimpresion = true (HU-22.05, CA-02)
        ArgumentCaptor<TicketPrintDataDTO> printCaptor = ArgumentCaptor.forClass(TicketPrintDataDTO.class);
        verify(ticketPrintFormatter).formatear(printCaptor.capture(), eq(configuracionParqueDTO));
        assertThat(printCaptor.getValue().esReimpresionSegura()).isTrue();
    }

    @Test
    @DisplayName("HU-22.04 CA-04: Lanza ConflictoException (HTTP 409) si el ticket está en estado CONSUMIDO")
    void reimprimirTicket_ticketConsumido_lanzaConflictoException409() {
        ticket.setEstadoTicket(EstadoTicket.CONSUMIDO);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        ReimpresionRequestDTO request = new ReimpresionRequestDTO("Boleto mojado");

        assertThatThrownBy(() -> ticketPrintService.reimprimirTicket(ticketUuid, request, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya se encuentra en estado CONSUMIDO");
    }

    @Test
    @DisplayName("HU-22.04: Lanza ConflictoException si el ticket está ANULADO")
    void reimprimirTicket_ticketAnulado_lanzaConflictoException() {
        ticket.setEstadoTicket(EstadoTicket.ANULADO);
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        ReimpresionRequestDTO request = new ReimpresionRequestDTO("Prueba");

        assertThatThrownBy(() -> ticketPrintService.reimprimirTicket(ticketUuid, request, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("se encuentra ANULADO");
    }

    @Test
    @DisplayName("HU-22.04 CA-01: Lanza ConflictoException si la boleta pertenece a un turno CERRADO")
    void reimprimirTicket_turnoNoAbierto_lanzaConflictoException() {
        TurnoCaja turnoCerrado = TurnoCaja.builder()
                .id(88L)
                .estado(EstadoTurnoCaja.CERRADO)
                .build();
        Venta venta = Venta.builder()
                .id(40L)
                .turnoCaja(turnoCerrado)
                .build();
        detalleVenta.setVenta(venta);

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));

        ReimpresionRequestDTO request = new ReimpresionRequestDTO("Reimpresión");

        assertThatThrownBy(() -> ticketPrintService.reimprimirTicket(ticketUuid, request, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("turno de caja actualmente activo (ABIERTO)");
    }

    @Test
    @DisplayName("HU-22.04 CA-01: Lanza ConflictoException si el cajero intenta reimprimir un ticket de otro turno")
    void reimprimirTicket_turnoCajeroDistinto_lanzaConflictoException() {
        TurnoCaja turnoVenta = TurnoCaja.builder()
                .id(100L)
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        Venta venta = Venta.builder()
                .id(41L)
                .turnoCaja(turnoVenta)
                .build();
        detalleVenta.setVenta(venta);

        Rol rolTaquillero = Rol.builder()
                .id(1L)
                .nombre(NombreRol.TAQUILLERO)
                .build();
        Usuario cajero = Usuario.builder()
                .id(7L)
                .correo("cajero@territoriodeninos.com")
                .rol(rolTaquillero)
                .build();

        TurnoCaja turnoDistinto = TurnoCaja.builder()
                .id(101L)
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cajero@territoriodeninos.com", "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        );

        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(7L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoDistinto));

        ReimpresionRequestDTO request = new ReimpresionRequestDTO("Reimpresión");

        assertThatThrownBy(() -> ticketPrintService.reimprimirTicket(ticketUuid, request, auth))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("no coincide con el turno activo del cajero");
    }

    @Test
    @DisplayName("HU-22.03: Lanza ReglaNegocioException si el motivo está vacío o es nulo")
    void reimprimirTicket_motivoInvalido_lanzaReglaNegocioException() {
        assertThatThrownBy(() -> ticketPrintService.reimprimirTicket(ticketUuid, null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("motivo de reimpresión es obligatorio");

        ReimpresionRequestDTO requestVacio = new ReimpresionRequestDTO("   ");
        assertThatThrownBy(() -> ticketPrintService.reimprimirTicket(ticketUuid, requestVacio, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("motivo de reimpresión es obligatorio");
    }
}
