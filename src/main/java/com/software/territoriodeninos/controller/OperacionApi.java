package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Operación de Atracciones", description = "Endpoints para escaneo QR, validación de acceso, control de aforo y temporización en tiempo real")
public interface OperacionApi {

    @Operation(
        summary = "Escanear y validar código QR de ticket en puesto de acceso",
        description = "Valida correspondencia de estación asignada (403), estado PAGADO (409), atracción (422), rango de edad (422), aforo máximo (409) y registra auditoría antifraude (HU-25.02, HU-26.03 a HU-26.06, HU-34.01, HU-34.02)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket validado exitosamente para ingreso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ValidacionTicketResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros faltantes"),
        @ApiResponse(responseCode = "403", description = "Operario no autorizado en esta estación"),
        @ApiResponse(responseCode = "404", description = "Ticket o atracción no encontrada"),
        @ApiResponse(responseCode = "409", description = "Conflicto: Aforo al 100% o ticket no se encuentra en estado PAGADO"),
        @ApiResponse(responseCode = "422", description = "Boleto incompatible: Atracción distinta o menor fuera del rango de edad")
    })
    ResponseEntity<ValidacionTicketResponseDTO> escanearTicket(
            @Valid @RequestBody EscaneoQrRequestDTO request,
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    );

    @Operation(
        summary = "Iniciar sesión de juego tras validación QR",
        description = "Cambia el ticket a EN_USO, calcula la horaFin exacta, persiste SesionJuego y notifica por WebSocket (HU-27.05, HU-27.06)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión iniciada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SesionJuegoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de inicio inválidos"),
        @ApiResponse(responseCode = "403", description = "Operario no autorizado en la estación"),
        @ApiResponse(responseCode = "404", description = "Ticket o atracción no encontrada"),
        @ApiResponse(responseCode = "409", description = "Aforo completo o ticket no está en estado PAGADO")
    })
    ResponseEntity<SesionJuegoResponseDTO> iniciarSesion(
            @Valid @RequestBody InicioSesionRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
        summary = "Consultar cola activa y menores en juego de una atracción",
        description = "Entrega todas las sesiones activas ordenadas por horaFin ascendente (próximos a salir primero) (HU-28.02)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cola consolidada de la atracción",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ColaAtraccionResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Atracción no encontrada")
    })
    ResponseEntity<ColaAtraccionResponseDTO> obtenerCola(@PathVariable Long atraccionId);

    @Operation(
        summary = "Pausar sesión de juego individual por contingencia",
        description = "Congela el tiempo restante de un niño por llanto, calzado, baño, etc. y registra en log_incidente_pausa (HU-29.03)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión pausada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SesionJuegoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Motivo de pausa faltante o sesión no en curso"),
        @ApiResponse(responseCode = "403", description = "Operario no asignado a la atracción"),
        @ApiResponse(responseCode = "404", description = "Sesión no encontrada")
    })
    ResponseEntity<SesionJuegoResponseDTO> pausarSesion(
            @PathVariable Long id,
            @Valid @RequestBody PausaRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
        summary = "Reanudar sesión de juego pausada",
        description = "Retoma el cronómetro recalculando la nueva horaFin sin perder tiempo contratado (HU-29.04)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión reanudada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SesionJuegoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Sesión no se encuentra pausada"),
        @ApiResponse(responseCode = "403", description = "Operario no asignado a la atracción"),
        @ApiResponse(responseCode = "404", description = "Sesión no encontrada")
    })
    ResponseEntity<SesionJuegoResponseDTO> reanudarSesion(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
        summary = "Pausa general de estación por contingencia climática o técnica",
        description = "Congela simultáneamente todas las sesiones activas en la estación (HU-29.05)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pausa general activada y cola congelada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ColaAtraccionResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Motivo de pausa general requerido"),
        @ApiResponse(responseCode = "403", description = "Operario no autorizado en esta estación"),
        @ApiResponse(responseCode = "404", description = "Atracción no encontrada")
    })
    ResponseEntity<ColaAtraccionResponseDTO> pausaGeneral(
            @PathVariable Long id,
            @Valid @RequestBody PausaGeneralRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
        summary = "Registrar egreso físico del menor de la atracción",
        description = "Formaliza la salida, actualiza ticket a CONSUMIDO atómicamente y libera cupo de aforo (HU-32.02, HU-32.03, HU-32.04)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Salida formalizada y aforo liberado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SesionJuegoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Sesión ya finalizada"),
        @ApiResponse(responseCode = "403", description = "Operario no autorizado en esta estación"),
        @ApiResponse(responseCode = "404", description = "Sesión no encontrada")
    })
    ResponseEntity<SesionJuegoResponseDTO> registrarSalida(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
        summary = "Descontar intento o caída en atracción por evento (Toro Mecánico)",
        description = "Descuenta un intento y finaliza la sesión pasando ticket a CONSUMIDO al llegar a 0 (HU-33.03, HU-33.04)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Intento descontado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SesionJuegoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Sesión no válida o ya finalizada"),
        @ApiResponse(responseCode = "403", description = "Operario no asignado a la atracción"),
        @ApiResponse(responseCode = "404", description = "Sesión no encontrada")
    })
    ResponseEntity<SesionJuegoResponseDTO> descontarIntento(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RegistroIntentoEventoDTO request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
        summary = "Sincronizar reloj de terminal y recalibrar cola activa",
        description = "Entrega epoch Unix oficial del servidor y estado recalibrado de la cola (HU-35.01, HU-35.02)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timestamp de servidor y cola calibrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SincronizacionRelojResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Atracción no encontrada")
    })
    ResponseEntity<SincronizacionRelojResponseDTO> sincronizarReloj(@PathVariable Long atraccionId);

    @Operation(
        summary = "Consultar métricas de aforo en tiempo real de una atracción",
        description = "Entrega ocupación activa, capacidad total y cupos libres inmediatos (HU-34.03)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Métricas de aforo calculadas",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AforoActualDTO.class))),
        @ApiResponse(responseCode = "404", description = "Atracción no encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    ResponseEntity<AforoActualDTO> obtenerAforoActual(@PathVariable Long id);
}
