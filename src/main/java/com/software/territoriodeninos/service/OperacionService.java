package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import org.springframework.security.core.Authentication;

public interface OperacionService {

    /**
     * Obtiene la estación física de trabajo actualmente asignada al operario autenticado.
     */
    EstacionTrabajoDTO obtenerEstacionActiva(Authentication auth);

    /**
     * Valida la boleta escaneada por QR contra aforo, estación física y reglas de negocio.
     */
    ValidacionTicketResponseDTO escanearTicket(EscaneoQrRequestDTO request, Authentication auth, String clientIp);

    /**
     * Inicia formalmente la sesión de juego de un menor, pasando el ticket a EN_USO y emitiendo evento WebSocket.
     */
    SesionJuegoResponseDTO iniciarSesion(InicioSesionRequestDTO request, Authentication auth);

    /**
     * Obtiene la cola activa de menores dentro de la atracción ordenada por vencimiento próximo.
     */
    ColaAtraccionResponseDTO obtenerCola(Long atraccionId);

    /**
     * Pausa el cronómetro de una sesión individual por contingencia (llanto, baño, etc.).
     */
    SesionJuegoResponseDTO pausarSesion(Long sesionId, PausaRequestDTO request, Authentication auth);

    /**
     * Reanuda el conteo de una sesión pausada recalculando la nueva horaFin sin perder tiempo contratado.
     */
    SesionJuegoResponseDTO reanudarSesion(Long sesionId, Authentication auth);

    /**
     * Pausa simultáneamente todas las sesiones activas en una estación por evento general (falla técnica, clima).
     */
    ColaAtraccionResponseDTO pausaGeneral(Long atraccionId, PausaGeneralRequestDTO request, Authentication auth);

    /**
     * Formaliza el egreso físico del menor de la atracción, marca ticket como CONSUMIDO y libera aforo (HU-32.02).
     */
    SesionJuegoResponseDTO registrarSalida(Long sesionId, Authentication auth);

    /**
     * Descuenta una caída o intento en atracciones por eventos (Toro Mecánico) y finaliza si llega a 0 (HU-33.03, HU-33.04).
     */
    SesionJuegoResponseDTO descontarIntento(Long sesionId, RegistroIntentoEventoDTO request, Authentication auth);

    /**
     * Sincroniza el reloj de la terminal con el epoch del servidor y entrega la cola recalculada (HU-35.01, HU-35.02).
     */
    SincronizacionRelojResponseDTO sincronizarReloj(Long atraccionId);

    /**
     * Obtiene las métricas de aforo y ocupación en tiempo real para una atracción.
     */
    AforoActualDTO obtenerAforoActual(Long atraccionId);

    /**
     * Convierte una entidad SesionJuego en su representación DTO calculando el tiempo restante actual.
     */
    SesionJuegoResponseDTO mapearSesionResponse(com.software.territoriodeninos.entity.SesionJuego sesion);
}
