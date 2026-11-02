package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.SesionJuegoResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SesionJuegoMonitorSchedulerTest - Pruebas del monitor @Scheduled de pre-expiración de 120s (HU-30.01 / HU-30.02)")
class SesionJuegoMonitorSchedulerTest {

    @Mock
    private SesionJuegoRepository sesionJuegoRepository;

    @Mock
    private OperacionWebSocketPublisher webSocketPublisher;

    @Mock
    private OperacionService operacionService;

    @InjectMocks
    private SesionJuegoMonitorScheduler scheduler;

    private Atraccion atraccion;
    private Menor menor;
    private SesionJuego sesionCercanaAExpirar;
    private SesionJuego sesionConMuchoTiempo;
    private SesionJuego sesionYaAlertada;
    private SesionJuegoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        atraccion = Atraccion.builder()
                .id(1L)
                .nombre("Piscina de Pelotas")
                .build();

        menor = Menor.builder()
                .id(10L)
                .nombreCompleto("Sofia Restrepo")
                .edad(5)
                .build();

        LocalDateTime now = LocalDateTime.now();

        // Quedan 90 segundos para expirar (<= 120s) y no ha sido alertada
        sesionCercanaAExpirar = SesionJuego.builder()
                .id(100L)
                .atraccion(atraccion)
                .menor(menor)
                .horaInicio(now.minusMinutes(13).minusSeconds(30))
                .horaFin(now.plusSeconds(90))
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .alertaPreExpiracionEnviada(false)
                .build();

        // Quedan 300 segundos (> 120s)
        sesionConMuchoTiempo = SesionJuego.builder()
                .id(101L)
                .atraccion(atraccion)
                .menor(menor)
                .horaInicio(now.minusMinutes(10))
                .horaFin(now.plusSeconds(300))
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .alertaPreExpiracionEnviada(false)
                .build();

        // Quedan 60s pero ya fue alertada
        sesionYaAlertada = SesionJuego.builder()
                .id(102L)
                .atraccion(atraccion)
                .menor(menor)
                .horaInicio(now.minusMinutes(14))
                .horaFin(now.plusSeconds(60))
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .alertaPreExpiracionEnviada(true)
                .build();

        responseDTO = new SesionJuegoResponseDTO(
                100L,
                null,
                1L,
                "Piscina de Pelotas",
                10L,
                "Sofia Restrepo",
                5,
                "Valentina Restrepo",
                "3127894561",
                "1094988776",
                "Carlos Operario",
                "carlos@territoriodeninos.com",
                now.minusMinutes(13).minusSeconds(30),
                now.plusSeconds(90),
                900,
                90,
                null,
                null,
                "EN_CURSO",
                0,
                null,
                null,
                true,
                false
        );
    }

    @Test
    @DisplayName("monitorearSesionesPreExpiracion - Emite alerta WebSocket cuando restan <= 120 segundos y marca alertaEnviada")
    void monitorearSesionesPreExpiracion_emiteAlertaCuandoRestanMenosDe120Segundos() {
        when(sesionJuegoRepository.findByEstadoSesion(EstadoSesionJuego.EN_CURSO))
                .thenReturn(List.of(sesionCercanaAExpirar, sesionConMuchoTiempo, sesionYaAlertada));

        when(operacionService.mapearSesionResponse(sesionCercanaAExpirar)).thenReturn(responseDTO);

        scheduler.monitorearSesionesPreExpiracion();

        // Verifica que se haya marcado como alertada y guardado
        assertTrue(sesionCercanaAExpirar.getAlertaPreExpiracionEnviada());
        verify(sesionJuegoRepository).save(sesionCercanaAExpirar);

        // Verifica emisión por WebSocket
        verify(webSocketPublisher).publicarAlertaPreExpiracion(eq(1L), any(SesionJuegoResponseDTO.class));

        // Las otras sesiones no debieron guardarse ni emitir alertas
        verify(sesionJuegoRepository, never()).save(sesionConMuchoTiempo);
        verify(sesionJuegoRepository, never()).save(sesionYaAlertada);
    }

    @Test
    @DisplayName("monitorearSesionesPreExpiracion - Emite TiempoExpiradoEvent en 00:00 y calcula segundos de exceso tras tolerancia (HU-31.01 / HU-31.02)")
    void monitorearSesionesPreExpiracion_emiteTiempoExpiradoYCalculaExceso() {
        LocalDateTime now = LocalDateTime.now();
        atraccion.setToleranciaMinutos(5); // 5 minutos = 300 segundos

        // Sesión finalizó hace 400 segundos (excede la tolerancia por 100 segundos)
        SesionJuego sesionExpirada = SesionJuego.builder()
                .id(200L)
                .atraccion(atraccion)
                .menor(menor)
                .horaInicio(now.minusMinutes(25))
                .horaFin(now.minusSeconds(400))
                .duracionSegundos(900)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .alertaPreExpiracionEnviada(true)
                .alertaExpiracionEnviada(false)
                .segundosExceso(0)
                .build();

        when(sesionJuegoRepository.findByEstadoSesion(EstadoSesionJuego.EN_CURSO))
                .thenReturn(List.of(sesionExpirada));

        when(operacionService.mapearSesionResponse(sesionExpirada)).thenReturn(responseDTO);

        scheduler.monitorearSesionesPreExpiracion();

        // Verifica emisión de evento WebSocket Tiempo Expirado (HU-31.01)
        verify(webSocketPublisher).publicarTiempoExpirado(eq(1L), any(), isNull());
        assertTrue(sesionExpirada.getAlertaExpiracionEnviada());

        // Verifica cálculo de exceso: 400s - 300s = 100s (HU-31.02)
        assertTrue(sesionExpirada.getSegundosExceso() >= 99);
    }
}
