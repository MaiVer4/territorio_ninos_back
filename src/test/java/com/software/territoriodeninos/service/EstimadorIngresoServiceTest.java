package com.software.territoriodeninos.service;

import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.DetalleVenta;
import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import com.software.territoriodeninos.util.IngresoTextoFormatter;
import com.software.territoriodeninos.util.TipoAtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimadorIngresoServiceTest {

    @Mock SesionJuegoRepository sesionJuegoRepository;
    @Mock TicketRepository ticketRepository;
    @Mock TarifaTiempoRepository tarifaTiempoRepository;
    @InjectMocks EstimadorIngresoServiceImpl estimador;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 19, 14, 0);
    private Atraccion atraccion;

    @BeforeEach
    void setUp() {
        atraccion = Atraccion.builder().id(1L).nombre("Saltarines").capacidadMaxima(2)
                .duracionMinutosBase(15).tipo(TipoAtraccion.TEMPORIZADA).build();
        lenient().when(ticketRepository.findEnEsperaPorAtraccion(eq(1L), any(), any())).thenReturn(List.of());
    }

    private SesionJuego sesion(LocalDateTime fin) {
        return SesionJuego.builder().estadoSesion(EstadoSesionJuego.EN_CURSO)
                .horaInicio(ahora.minusMinutes(10)).horaFin(fin).build();
    }

    @Test
    void conCupoLibre_ingresoAhora() {
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(anyLong(), anyCollection()))
                .thenReturn(List.of(sesion(ahora.plusMinutes(20))));
        assertThat(estimador.estimarProximoIngreso(atraccion, ahora)).isNull();
    }

    @Test
    void lleno_usaProximoEnSalir() {
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(anyLong(), anyCollection()))
                .thenReturn(List.of(sesion(ahora.plusMinutes(20)), sesion(ahora.plusMinutes(5))));
        assertThat(estimador.estimarProximoIngreso(atraccion, ahora)).isEqualTo(ahora.plusMinutes(5));
    }

    @Test
    void colaFifo_escalonaBoletasNuevas() {
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(anyLong(), anyCollection()))
                .thenReturn(List.of(sesion(ahora.plusMinutes(20)), sesion(ahora.plusMinutes(5))));
        List<LocalDateTime> horas = estimador.estimar(atraccion, List.of(30, 30, 30), ahora);
        // 1ra toma 14:05, 2da 14:20, 3ra reusa slot de la 1ra -> 14:35
        assertThat(horas).containsExactly(ahora.plusMinutes(5), ahora.plusMinutes(20), ahora.plusMinutes(35));
    }

    @Test
    void boletaEnEspera_reservaCupoAntesQueNuevas() {
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(anyLong(), anyCollection()))
                .thenReturn(List.of(sesion(ahora.plusMinutes(20)), sesion(ahora.plusMinutes(5))));
        Ticket enEspera = Ticket.builder().fechaEmision(ahora.minusMinutes(1))
                .horaIngresoEstimada(ahora.plusMinutes(5)).detalleVenta(DetalleVenta.builder().build()).build();
        when(ticketRepository.findEnEsperaPorAtraccion(eq(1L), any(), any())).thenReturn(List.of(enEspera));
        assertThat(estimador.estimarProximoIngreso(atraccion, ahora)).isEqualTo(ahora.plusMinutes(20));
    }

    @Test
    void boletaAbandonada_noReservaCupo() {
        when(sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(anyLong(), anyCollection()))
                .thenReturn(List.of(sesion(ahora.plusMinutes(20))));
        Ticket abandonada = Ticket.builder().fechaEmision(ahora.minusHours(2))
                .horaIngresoEstimada(ahora.minusMinutes(30)).detalleVenta(DetalleVenta.builder().build()).build();
        when(ticketRepository.findEnEsperaPorAtraccion(eq(1L), any(), any())).thenReturn(List.of(abandonada));
        assertThat(estimador.estimarProximoIngreso(atraccion, ahora)).isNull();
    }

    @Test
    void formatoTexto() {
        assertThat(IngresoTextoFormatter.texto(null, TipoAtraccion.TEMPORIZADA)).isEqualTo("AHORA");
        assertThat(IngresoTextoFormatter.texto(ahora.plusMinutes(35), TipoAtraccion.TEMPORIZADA)).isEqualTo("14:35 (aprox.)");
        assertThat(IngresoTextoFormatter.texto(ahora, TipoAtraccion.EVENTOS_CAIDAS)).isEqualTo("PRÓXIMO TURNO");
    }
}
