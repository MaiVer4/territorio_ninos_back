package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.IncidenteDetalleDTO;
import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AuditoriaTicketRepository;
import com.software.territoriodeninos.repository.LogIncidentePausaRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaOperacionServiceTest - Pruebas de trazabilidad de tickets y monitoreo de incidentes (HU-38, HU-41)")
class AuditoriaOperacionServiceTest {

    @Mock
    private AuditoriaTicketRepository auditoriaTicketRepository;

    @Mock
    private LogIncidentePausaRepository logIncidentePausaRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaOperacionServiceImpl auditoriaOperacionService;

    private UUID ticketUuid;
    private Ticket ticket;
    private Usuario operario;
    private Atraccion atraccion;

    @BeforeEach
    void setUp() {
        ticketUuid = UUID.randomUUID();

        operario = Usuario.builder()
                .id(5L)
                .nombre("Pedro Operario")
                .correo("pedro@parque.com")
                .build();

        atraccion = Atraccion.builder()
                .id(2L)
                .nombre("Montaña Rusa Infantil")
                .build();

        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombres("Carlos")
                .apellidos("Gutiérrez")
                .numeroDocumento("987654321")
                .build();

        Menor menor = Menor.builder()
                .id(1L)
                .nombreCompleto("Mateo Gutiérrez")
                .cliente(cliente)
                .build();

        Venta venta = Venta.builder()
                .id(100L)
                .cliente(cliente)
                .fechaVenta(LocalDateTime.now().minusHours(2))
                .build();

        DetalleVenta detalle = DetalleVenta.builder()
                .id(50L)
                .venta(venta)
                .atraccion(atraccion)
                .menor(menor)
                .build();

        ticket = Ticket.builder()
                .uuidCodigo(ticketUuid)
                .detalleVenta(detalle)
                .menor(menor)
                .estadoTicket(EstadoTicket.EN_USO)
                .fechaEmision(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    @DisplayName("Debe persistir el registro inmutable de transición de ticket (HU-38.01, HU-38.04)")
    void testRegistrarTransicionTicket() {
        auditoriaOperacionService.registrarTransicionTicket(
                ticketUuid,
                "PAGADO",
                "EN_USO",
                "Ingreso a estación",
                5L,
                "Terminal Operario"
        );

        ArgumentCaptor<AuditoriaTicket> captor = ArgumentCaptor.forClass(AuditoriaTicket.class);
        verify(auditoriaTicketRepository).save(captor.capture());

        AuditoriaTicket guardado = captor.getValue();
        assertThat(guardado.getTicketUuid()).isEqualTo(ticketUuid);
        assertThat(guardado.getEstadoOrigen()).isEqualTo("PAGADO");
        assertThat(guardado.getEstadoDestino()).isEqualTo("EN_USO");
        assertThat(guardado.getMotivo()).isEqualTo("Ingreso a estación");
        assertThat(guardado.getUsuarioId()).isEqualTo(5L);
        assertThat(guardado.getDispositivoInfo()).isEqualTo("Terminal Operario");
    }

    @Test
    @DisplayName("Debe consultar timeline cronológico por UUID de boleta (HU-38.03, HU-38.05)")
    void testConsultarTimelinePorUuid() {
        when(ticketRepository.findByUuidCodigo(ticketUuid)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operario));

        AuditoriaTicket ev1 = AuditoriaTicket.builder()
                .id(1L)
                .ticketUuid(ticketUuid)
                .estadoOrigen(null)
                .estadoDestino("PAGADO")
                .motivo("Venta inicial")
                .usuarioId(5L)
                .fechaEvento(LocalDateTime.now().minusHours(2))
                .build();

        AuditoriaTicket ev2 = AuditoriaTicket.builder()
                .id(2L)
                .ticketUuid(ticketUuid)
                .estadoOrigen("PAGADO")
                .estadoDestino("EN_USO")
                .motivo("Ingreso a atracción")
                .usuarioId(5L)
                .fechaEvento(LocalDateTime.now().minusMinutes(30))
                .build();

        when(auditoriaTicketRepository.findByTicketUuidOrderByFechaEventoAsc(ticketUuid))
                .thenReturn(List.of(ev1, ev2));

        List<TimelineTicketDTO> timelines = auditoriaOperacionService.consultarTimelinePorCriterio(ticketUuid.toString());

        assertThat(timelines).hasSize(1);
        TimelineTicketDTO timeline = timelines.get(0);
        assertThat(timeline.ticketUuid()).isEqualTo(ticketUuid);
        assertThat(timeline.atraccionNombre()).isEqualTo("Montaña Rusa Infantil");
        assertThat(timeline.clienteNombre()).isEqualTo("Carlos Gutiérrez");
        assertThat(timeline.menorNombre()).isEqualTo("Mateo Gutiérrez");
        assertThat(timeline.eventos()).hasSize(2);
        assertThat(timeline.eventos().get(0).estadoDestino()).isEqualTo("PAGADO");
        assertThat(timeline.eventos().get(1).estadoDestino()).isEqualTo("EN_USO");
    }

    @Test
    @DisplayName("Debe consultar boletas por documento del acudiente (HU-38.05)")
    void testConsultarTimelinePorDocumentoAcudiente() {
        when(ticketRepository.findByDetalleVentaVentaClienteNumeroDocumentoOrderByFechaEmisionDesc("987654321"))
                .thenReturn(List.of(ticket));
        when(auditoriaTicketRepository.findByTicketUuidOrderByFechaEventoAsc(ticketUuid))
                .thenReturn(List.of());

        List<TimelineTicketDTO> timelines = auditoriaOperacionService.consultarTimelinePorCriterio("987654321");

        assertThat(timelines).hasSize(1);
        assertThat(timelines.get(0).clienteDocumento()).isEqualTo("987654321");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el criterio no retorna ningún ticket")
    void testConsultarTimelineNoEncontrado() {
        when(ticketRepository.findByDetalleVentaVentaClienteNumeroDocumentoOrderByFechaEmisionDesc("000000"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> auditoriaOperacionService.consultarTimelinePorCriterio("000000"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Debe consultar incidentes y calcular tiempo de inactividad operativa vs técnica (HU-41.02, HU-41.03)")
    void testConsultarIncidentes() {
        LogIncidentePausa logOperativo = LogIncidentePausa.builder()
                .id(1L)
                .atraccion(atraccion)
                .operario(operario)
                .motivo("Limpieza de atracción")
                .tipoPausa("OPERATIVA")
                .esPausaGeneral(false)
                .fechaInicioPausa(LocalDateTime.now().minusHours(3))
                .fechaFinPausa(LocalDateTime.now().minusHours(3).plusMinutes(15))
                .duracionSegundos(900)
                .build();

        LogIncidentePausa logTecnico = LogIncidentePausa.builder()
                .id(2L)
                .atraccion(atraccion)
                .operario(operario)
                .motivo("Mantenimiento correctivo de arnés")
                .tipoPausa("TECNICA")
                .esPausaGeneral(true)
                .fechaInicioPausa(LocalDateTime.now().minusHours(1))
                .fechaFinPausa(LocalDateTime.now().minusHours(1).plusMinutes(30))
                .duracionSegundos(1800)
                .build();

        when(logIncidentePausaRepository.findIncidentesConFiltros(any(), any(), any(), any()))
                .thenReturn(List.of(logOperativo, logTecnico));

        ResumenIncidentesAuditoriaDTO resumen = auditoriaOperacionService.consultarIncidentes(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                2L,
                null
        );

        assertThat(resumen.totalIncidentes()).isEqualTo(2);
        assertThat(resumen.tiempoTotalInactividadSegundos()).isEqualTo(2700L); // 900 + 1800
        assertThat(resumen.tiempoTotalInactividadMinutos()).isEqualTo(45.0);   // 2700 / 60
        assertThat(resumen.inactividadOperativaSegundos()).isEqualTo(900L);
        assertThat(resumen.inactividadTecnicaSegundos()).isEqualTo(1800L);
        assertThat(resumen.incidentes()).hasSize(2);
    }
}
