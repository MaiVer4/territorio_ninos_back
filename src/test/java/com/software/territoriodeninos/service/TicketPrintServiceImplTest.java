package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.TicketPrintDataDTO;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.EstadoTicket;
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

        detalleVenta = DetalleVenta.builder()
                .id(10L)
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
}
