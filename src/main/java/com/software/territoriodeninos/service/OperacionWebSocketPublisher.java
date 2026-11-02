package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ColaAtraccionResponseDTO;
import com.software.territoriodeninos.dto.OperacionWebSocketEvent;
import com.software.territoriodeninos.dto.SesionJuegoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperacionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publicarNuevoIngreso(Long atraccionId, SesionJuegoResponseDTO sesion, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "NUEVO_INGRESO",
                atraccionId,
                sesion != null ? sesion.sesionId() : null,
                "Nuevo menor ingresó a la atracción",
                sesion,
                cola
        );
        difundir(atraccionId, evento);
    }

    public void publicarPausa(Long atraccionId, Long sesionId, String motivo, SesionJuegoResponseDTO sesion, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "PAUSA",
                atraccionId,
                sesionId,
                "Sesión pausada: " + motivo,
                sesion,
                cola
        );
        difundir(atraccionId, evento);
    }

    public void publicarReanudacion(Long atraccionId, Long sesionId, SesionJuegoResponseDTO sesion, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "REANUDACION",
                atraccionId,
                sesionId,
                "Sesión reanudada",
                sesion,
                cola
        );
        difundir(atraccionId, evento);
    }

    public void publicarPausaGeneral(Long atraccionId, String motivo, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "PAUSA_GENERAL",
                atraccionId,
                null,
                "Pausa general activada: " + motivo,
                null,
                cola
        );
        difundir(atraccionId, evento);
    }

    public void publicarAlertaPreExpiracion(Long atraccionId, SesionJuegoResponseDTO sesion) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "ALERTA_PRE_EXPIRACION",
                atraccionId,
                sesion != null ? sesion.sesionId() : null,
                "Sesión próxima a finalizar (<= 2 minutos)",
                sesion,
                null
        );
        difundir(atraccionId, evento);
    }

    public void publicarColaActualizada(Long atraccionId, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "ACTUALIZACION_COLA",
                atraccionId,
                null,
                "Actualización periódica de la cola",
                null,
                cola
        );
        difundir(atraccionId, evento);
    }

    public void publicarTiempoExpirado(Long atraccionId, SesionJuegoResponseDTO sesion, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "TIEMPO_EXPIRADO",
                atraccionId,
                sesion != null ? sesion.sesionId() : null,
                "Tiempo contratado finalizado (00:00)",
                sesion,
                cola
        );
        difundir(atraccionId, evento);
    }

    public void publicarSesionFinalizada(Long atraccionId, SesionJuegoResponseDTO sesion, ColaAtraccionResponseDTO cola) {
        OperacionWebSocketEvent evento = OperacionWebSocketEvent.crear(
                "SESION_FINALIZADA",
                atraccionId,
                sesion != null ? sesion.sesionId() : null,
                "Menor egresó de la atracción y cupo liberado",
                sesion,
                cola
        );
        difundir(atraccionId, evento);
    }

    private void difundir(Long atraccionId, OperacionWebSocketEvent evento) {
        try {
            String destinoEstacion = "/topic/atraccion/" + atraccionId;
            messagingTemplate.convertAndSend(destinoEstacion, evento);
            messagingTemplate.convertAndSend("/topic/admin/dashboard", evento);
            log.debug("Evento WebSocket {} difundido a {} y /topic/admin/dashboard", evento.tipo(), destinoEstacion);
        } catch (Exception ex) {
            log.warn("No se pudo difundir el evento WebSocket {}: {}", evento.tipo(), ex.getMessage());
        }
    }
}
