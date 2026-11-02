package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.SesionJuegoResponseDTO;
import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesionJuegoMonitorScheduler {

    private final SesionJuegoRepository sesionJuegoRepository;
    private final OperacionWebSocketPublisher webSocketPublisher;
    private final OperacionService operacionService;

    /**
     * Monitoreo continuo de sesiones de juego activas cada 5 segundos (HU-30.01).
     * Identifica sesiones que cruzan el umbral de 2 minutos (<= 120s) restantes y emite alerta WebSocket.
     */
    @Scheduled(fixedDelayString = "${app.operacion.monitor-delay-ms:5000}")
    @Transactional
    public void monitorearSesionesPreExpiracion() {
        LocalDateTime now = LocalDateTime.now();
        List<SesionJuego> sesionesEnCurso = sesionJuegoRepository.findByEstadoSesion(EstadoSesionJuego.EN_CURSO);

        for (SesionJuego sesion : sesionesEnCurso) {
            if (sesion.getHoraFin() == null) {
                continue;
            }

            long segundosRestantes = Duration.between(now, sesion.getHoraFin()).toSeconds();

            // Umbral de 2 minutos (120 segundos) para pre-expiración (HU-30.01 / HU-30.02)
            if (segundosRestantes <= 120 && segundosRestantes > 0 && Boolean.FALSE.equals(sesion.getAlertaPreExpiracionEnviada())) {
                sesion.setAlertaPreExpiracionEnviada(true);
                sesionJuegoRepository.save(sesion);

                SesionJuegoResponseDTO sesionDTO = operacionService.mapearSesionResponse(sesion);
                webSocketPublisher.publicarAlertaPreExpiracion(sesion.getAtraccion().getId(), sesionDTO);

                log.info("Alerta pre-expiración emitida para sesión ID #{} (Menor: {}, {}s restantes)",
                        sesion.getId(),
                        sesion.getMenor() != null ? sesion.getMenor().getNombreCompleto() : "N/A",
                        segundosRestantes);
            }

            // Expiración exacta en 00:00 (HU-31.01) y cálculo de tolerancia/exceso (HU-31.02)
            if (segundosRestantes <= 0) {
                if (Boolean.FALSE.equals(sesion.getAlertaExpiracionEnviada())) {
                    sesion.setAlertaExpiracionEnviada(true);
                    sesionJuegoRepository.save(sesion);

                    SesionJuegoResponseDTO sesionDTO = operacionService.mapearSesionResponse(sesion);
                    webSocketPublisher.publicarTiempoExpirado(sesion.getAtraccion().getId(), sesionDTO, null);

                    log.info("Tiempo expirado (00:00) emitido para sesión ID #{} (Menor: {})",
                            sesion.getId(),
                            sesion.getMenor() != null ? sesion.getMenor().getNombreCompleto() : "N/A");
                }

                int toleranciaMin = sesion.getAtraccion().getToleranciaMinutos() != null ? sesion.getAtraccion().getToleranciaMinutos() : 0;
                long segundosDespuesFin = Math.abs(segundosRestantes);
                long segundosTolerancia = toleranciaMin * 60L;
                if (segundosDespuesFin > segundosTolerancia) {
                    int exceso = (int) (segundosDespuesFin - segundosTolerancia);
                    if (!Integer.valueOf(exceso).equals(sesion.getSegundosExceso())) {
                        sesion.setSegundosExceso(exceso);
                        sesionJuegoRepository.save(sesion);
                    }
                }
            }
        }
    }
}
