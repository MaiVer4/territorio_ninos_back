package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AforoActualDTO;
import com.software.territoriodeninos.dto.ColaAtraccionResponseDTO;
import com.software.territoriodeninos.dto.EscaneoQrRequestDTO;
import com.software.territoriodeninos.dto.EstacionTrabajoDTO;
import com.software.territoriodeninos.dto.InicioSesionRequestDTO;
import com.software.territoriodeninos.dto.PausaGeneralRequestDTO;
import com.software.territoriodeninos.dto.PausaRequestDTO;
import com.software.territoriodeninos.dto.RegistroIntentoEventoDTO;
import com.software.territoriodeninos.dto.SesionJuegoResponseDTO;
import com.software.territoriodeninos.dto.SincronizacionRelojResponseDTO;
import com.software.territoriodeninos.dto.ValidacionTicketResponseDTO;
import com.software.territoriodeninos.entity.AsignacionOperario;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.DetalleVenta;
import com.software.territoriodeninos.entity.LogEscaneoQr;
import com.software.territoriodeninos.entity.LogIncidentePausa;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.entity.Ticket;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.exception.AforoCompletoException;
import com.software.territoriodeninos.exception.EstacionNoAutorizadaException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.exception.TicketInvalidoOperacionException;
import com.software.territoriodeninos.repository.AsignacionOperarioRepository;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.LogEscaneoQrRepository;
import com.software.territoriodeninos.repository.LogIncidentePausaRepository;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.repository.TicketRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.AuthenticatedUserProvider;
import com.software.territoriodeninos.util.EstadoAsignacion;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.NombreRol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class OperacionServiceImpl implements OperacionService {

    private final AsignacionOperarioRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtraccionRepository atraccionRepository;
    private final TicketRepository ticketRepository;
    private final SesionJuegoRepository sesionJuegoRepository;
    private final LogEscaneoQrRepository logEscaneoQrRepository;
    private final LogIncidentePausaRepository logIncidentePausaRepository;
    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final TarifaEventoRepository tarifaEventoRepository;
    private final OperacionWebSocketPublisher webSocketPublisher;
    private final AuditoriaOperacionService auditoriaOperacionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public OperacionServiceImpl(
            AsignacionOperarioRepository asignacionRepository,
            UsuarioRepository usuarioRepository,
            AtraccionRepository atraccionRepository,
            TicketRepository ticketRepository,
            SesionJuegoRepository sesionJuegoRepository,
            LogEscaneoQrRepository logEscaneoQrRepository,
            LogIncidentePausaRepository logIncidentePausaRepository,
            TarifaTiempoRepository tarifaTiempoRepository,
            TarifaEventoRepository tarifaEventoRepository,
            OperacionWebSocketPublisher webSocketPublisher,
            AuditoriaOperacionService auditoriaOperacionService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.atraccionRepository = atraccionRepository;
        this.ticketRepository = ticketRepository;
        this.sesionJuegoRepository = sesionJuegoRepository;
        this.logEscaneoQrRepository = logEscaneoQrRepository;
        this.logIncidentePausaRepository = logIncidentePausaRepository;
        this.tarifaTiempoRepository = tarifaTiempoRepository;
        this.tarifaEventoRepository = tarifaEventoRepository;
        this.webSocketPublisher = webSocketPublisher;
        this.auditoriaOperacionService = auditoriaOperacionService;
        this.authenticatedUserProvider = authenticatedUserProvider != null
                ? authenticatedUserProvider
                : new AuthenticatedUserProvider(usuarioRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public EstacionTrabajoDTO obtenerEstacionActiva(Authentication auth) {
        Usuario usuario = resolverUsuarioAutenticado(auth);
        if (usuario == null) {
            throw new ReglaNegocioException("Usuario no autenticado o sesión no válida.");
        }

        AsignacionOperario asignacion = resolverAsignacionActiva(usuario);
        if (asignacion == null) {
            throw new RecursoNoEncontradoException(
                    "El operario " + usuario.getNombre() + " (" + usuario.getCorreo() + ") no tiene una estación de trabajo activa asignada en este momento."
            );
        }

        Atraccion atraccion = asignacion.getAtraccion();
        int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        int aforoActual = calcularOcupacionActiva(atraccion.getId());
        int cuposDisponibles = Math.max(0, capacidad - aforoActual);

        String rangoEdad = EstacionTrabajoDTO.formatearRangoEdad(atraccion.getEdadMinima(), atraccion.getEdadMaxima());

        return new EstacionTrabajoDTO(
                asignacion.getId(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                atraccion.getId(),
                atraccion.getNombre(),
                atraccion.getDescripcion(),
                atraccion.getTipo() != null ? atraccion.getTipo().name() : "TEMPORIZADA",
                capacidad,
                aforoActual,
                cuposDisponibles,
                atraccion.getDuracionMinutosBase(),
                atraccion.getToleranciaMinutos(),
                atraccion.getCaidasLimite(),
                atraccion.getEdadMinima(),
                atraccion.getEdadMaxima(),
                rangoEdad,
                atraccion.getEstado() != null ? atraccion.getEstado().name() : "ACTIVO",
                asignacion.getFechaAsignacion(),
                asignacion.getHoraInicio()
        );
    }

    @Override
    @Transactional(noRollbackFor = {
            AforoCompletoException.class,
            RecursoNoEncontradoException.class,
            TicketInvalidoOperacionException.class,
            EstacionNoAutorizadaException.class
    })
    public ValidacionTicketResponseDTO escanearTicket(EscaneoQrRequestDTO request, Authentication auth, String clientIp) {
        if (request == null || request.uuid() == null || request.atraccionId() == null) {
            throw new ReglaNegocioException("Los parámetros de lectura QR (UUID del ticket y atraccionId) son obligatorios.");
        }

        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario en sesión no válido para registrar la operación.");
        }

        Atraccion atraccionEstacion = atraccionRepository.findById(request.atraccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + request.atraccionId()));

        // 1. Intercepción de seguridad: Validar que el operario no intente operar otra estación (HU-24.04 / CA-02 -> HTTP 403)
        validarEstacionAsignadaAlOperario(operario, atraccionEstacion);

        // 2. Control de Aforo en tiempo real (HU-34.01, HU-34.02 -> HTTP 409)
        int capacidadMaxima = atraccionEstacion.getCapacidadMaxima() != null ? atraccionEstacion.getCapacidadMaxima() : 1;
        int ocupacionActiva = calcularOcupacionActiva(atraccionEstacion.getId());
        if (ocupacionActiva >= capacidadMaxima) {
            registrarLogEscaneo(request.uuid(), false, atraccionEstacion, operario, clientIp,
                    request.timestamp(), "RECHAZADO_AFORO_COMPLETO",
                    "Aforo completo (" + ocupacionActiva + "/" + capacidadMaxima + ")");
            throw new AforoCompletoException(
                    "La atracción '" + atraccionEstacion.getNombre() + "' ha alcanzado el 100% de su capacidad máxima (" +
                            ocupacionActiva + "/" + capacidadMaxima + " niños). No se permiten nuevos ingresos."
            );
        }

        // 3. Validación algorítmica de existencia y estado PAGADO (HU-26.03 / CA-01 -> HTTP 409 si CONSUMIDO/ANULADO)
        Ticket ticket = ticketRepository.findByUuidCodigo(request.uuid())
                .orElse(null);

        if (ticket == null) {
            registrarLogEscaneo(request.uuid(), false, atraccionEstacion, operario, clientIp,
                    request.timestamp(), "RECHAZADO_NO_ENCONTRADO", "Ticket inexistente");
            throw new RecursoNoEncontradoException("Ticket no encontrado con el código UUID: " + request.uuid());
        }

        if (!EstadoTicket.PAGADO.equals(ticket.getEstadoTicket())) {
            String motivo = "El ticket no se encuentra en estado PAGADO (estado actual: " + ticket.getEstadoTicket() + ").";
            registrarLogEscaneo(request.uuid(), true, atraccionEstacion, operario, clientIp,
                    request.timestamp(), "RECHAZADO_ESTADO_INVALIDO", motivo);
            throw new TicketInvalidoOperacionException(motivo, HttpStatus.CONFLICT);
        }

        // 4. Validación estricta de coincidencia de atracción (HU-26.04 / CA-02 -> HTTP 422)
        DetalleVenta detalle = ticket.getDetalleVenta();
        Atraccion atraccionTicket = detalle != null ? detalle.getAtraccion() : null;
        if (atraccionTicket == null || !atraccionTicket.getId().equals(atraccionEstacion.getId())) {
            String nombreEsperada = atraccionTicket != null ? atraccionTicket.getNombre() : "desconocida";
            String motivo = "El ticket fue adquirido para la atracción '" + nombreEsperada +
                    "' y no puede ser redimido en la estación '" + atraccionEstacion.getNombre() + "'.";
            registrarLogEscaneo(request.uuid(), true, atraccionEstacion, operario, clientIp,
                    request.timestamp(), "RECHAZADO_ATRACCION_DISTINTA", motivo);
            throw new TicketInvalidoOperacionException(motivo, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // 5. Validación automática del rango de edad del menor (HU-26.05 / CA-03 -> HTTP 422)
        Menor menor = ticket.getMenor() != null ? ticket.getMenor() : (detalle != null ? detalle.getMenor() : null);
        validarRangoEdadMenor(menor, atraccionEstacion, request.uuid(), operario, clientIp, request.timestamp());

        // 6. Resolver minutos o intentos contratados
        Integer minutosAutorizados = resolverMinutosAutorizados(detalle, atraccionEstacion);
        Integer intentosAutorizados = resolverIntentosAutorizados(detalle, atraccionEstacion);

        // 7. Registro de auditoría exitoso en base de datos (HU-25.03)
        registrarLogEscaneo(request.uuid(), true, atraccionEstacion, operario, clientIp,
                request.timestamp(), "VALIDADO_EXITOSO", "Validación completada satisfactoriamente");

        Venta venta = detalle != null ? detalle.getVenta() : null;
        Cliente cliente = venta != null ? venta.getCliente() : (menor != null ? menor.getCliente() : null);

        log.info("Ticket UUID {} validado exitosamente para menor {} en atracción {} por operario {}",
                ticket.getUuidCodigo(), menor != null ? menor.getNombreCompleto() : "N/A",
                atraccionEstacion.getNombre(), operario.getCorreo());

        return new ValidacionTicketResponseDTO(
                true,
                "Ticket validado exitosamente para ingreso a " + atraccionEstacion.getNombre(),
                ticket.getUuidCodigo(),
                ticket.getEstadoTicket().name(),
                atraccionEstacion.getId(),
                atraccionEstacion.getNombre(),
                atraccionEstacion.getTipo() != null ? atraccionEstacion.getTipo().name() : "TEMPORIZADA",
                menor != null ? menor.getId() : null,
                menor != null ? menor.getNombreCompleto() : "Menor sin registrar",
                menor != null ? (menor.getEdadActual() != null ? menor.getEdadActual() : menor.getEdad()) : null,
                cliente != null ? cliente.getNombreCompleto() : "Acudiente",
                cliente != null ? cliente.getNumeroDocumento() : "N/A",
                cliente != null ? cliente.getTelefono() : "N/A",
                detalle != null && detalle.getTipoTarifa() != null ? detalle.getTipoTarifa().name() : "TIEMPO",
                minutosAutorizados,
                intentosAutorizados,
                atraccionEstacion.getDuracionMinutosBase(),
                atraccionEstacion.getToleranciaMinutos(),
                atraccionEstacion.getCaidasLimite(),
                ticket.getFechaEmision()
        );
    }

    @Override
    public SesionJuegoResponseDTO iniciarSesion(InicioSesionRequestDTO request, Authentication auth) {
        if (request == null || request.uuidTicket() == null || request.atraccionId() == null) {
            throw new ReglaNegocioException("El UUID del ticket y el atraccionId son obligatorios para iniciar la sesión.");
        }

        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario no autenticado para registrar el inicio de sesión.");
        }

        Atraccion atraccion = atraccionRepository.findByIdForUpdate(request.atraccionId())
                .or(() -> atraccionRepository.findById(request.atraccionId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + request.atraccionId()));

        validarEstacionAsignadaAlOperario(operario, atraccion);

        // Control de aforo
        int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        int ocupacionActiva = calcularOcupacionActiva(atraccion.getId());
        if (ocupacionActiva >= capacidad) {
            throw new AforoCompletoException(
                    "Aforo completo (" + ocupacionActiva + "/" + capacidad + "). No es posible ingresar más participantes a " + atraccion.getNombre()
            );
        }

        Ticket ticket = ticketRepository.findByUuidCodigoForUpdate(request.uuidTicket())
                .or(() -> ticketRepository.findByUuidCodigo(request.uuidTicket()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + request.uuidTicket()));

        if (!EstadoTicket.PAGADO.equals(ticket.getEstadoTicket())) {
            throw new TicketInvalidoOperacionException(
                    "El ticket debe estar en estado PAGADO para iniciar sesión (estado actual: " + ticket.getEstadoTicket() + ")",
                    HttpStatus.CONFLICT
            );
        }

        DetalleVenta detalle = ticket.getDetalleVenta();
        if (detalle != null && detalle.getAtraccion() != null && !detalle.getAtraccion().getId().equals(atraccion.getId())) {
            throw new TicketInvalidoOperacionException(
                    "El ticket pertenece a la atracción '" + detalle.getAtraccion().getNombre() + "' y no coincide con la estación actual.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        Menor menor = ticket.getMenor() != null ? ticket.getMenor() : (detalle != null ? detalle.getMenor() : null);

        // Calcular duración
        int minutos = resolverMinutosAutorizados(detalle, atraccion);
        int duracionSegundos = Math.max(60, minutos * 60);

        LocalDateTime horaInicio = LocalDateTime.now();
        LocalDateTime horaFin = horaInicio.plusSeconds(duracionSegundos);

        Integer intentos = resolverIntentosAutorizados(detalle, atraccion);

        // Cambiar estado del ticket a EN_USO y registrar auditoría inmutable (HU-38.04)
        String estadoOrigen = ticket.getEstadoTicket() != null ? ticket.getEstadoTicket().name() : "PAGADO";
        ticket.setEstadoTicket(EstadoTicket.EN_USO);
        ticketRepository.save(ticket);
        auditoriaOperacionService.registrarTransicionTicket(
                ticket.getUuidCodigo(),
                estadoOrigen,
                EstadoTicket.EN_USO.name(),
                "Inicio de sesión en atracción: " + atraccion.getNombre(),
                operario.getId(),
                "Terminal Operario"
        );

        SesionJuego sesion = SesionJuego.builder()
                .ticket(ticket)
                .atraccion(atraccion)
                .menor(menor)
                .operario(operario)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .duracionSegundos(duracionSegundos)
                .estadoSesion(EstadoSesionJuego.EN_CURSO)
                .intentosTotales(intentos)
                .intentosRestantes(intentos)
                .segundosExceso(0)
                .alertaPreExpiracionEnviada(false)
                .alertaExpiracionEnviada(false)
                .build();

        sesion = sesionJuegoRepository.save(sesion);

        log.info("Sesión de juego ID #{} iniciada para ticket {} en atracción {} por operario {}",
                sesion.getId(), ticket.getUuidCodigo(), atraccion.getNombre(), operario.getCorreo());

        SesionJuegoResponseDTO sesionDTO = mapearSesionResponse(sesion);
        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(atraccion);

        // Difusión en tiempo real por WebSocket (HU-27.06, HU-28.03)
        webSocketPublisher.publicarNuevoIngreso(atraccion.getId(), sesionDTO, colaDTO);

        return sesionDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public ColaAtraccionResponseDTO obtenerCola(Long atraccionId) {
        Atraccion atraccion = atraccionRepository.findById(atraccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));
        return construirColaAtraccion(atraccion);
    }

    @Override
    public SesionJuegoResponseDTO pausarSesion(Long sesionId, PausaRequestDTO request, Authentication auth) {
        if (request == null || request.motivo() == null || request.motivo().isBlank()) {
            throw new ReglaNegocioException("El motivo de la pausa es obligatorio.");
        }

        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario no autenticado para pausar la sesión.");
        }

        SesionJuego sesion = sesionJuegoRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de juego no encontrada con ID: " + sesionId));

        validarEstacionAsignadaAlOperario(operario, sesion.getAtraccion());

        if (!EstadoSesionJuego.EN_CURSO.equals(sesion.getEstadoSesion())) {
            throw new ReglaNegocioException("Solo se pueden pausar sesiones en estado EN_CURSO (estado actual: " + sesion.getEstadoSesion() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        String motivoTrimmed = request.motivo().trim();
        aplicarPausaASesion(sesion, operario, motivoTrimmed, "Pausa individual: " + motivoTrimmed, false, now);

        log.info("Sesión ID #{} pausada por operario {}. Motivo: {}", sesionId, operario.getCorreo(), request.motivo());

        SesionJuegoResponseDTO sesionDTO = mapearSesionResponse(sesion);
        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(sesion.getAtraccion());

        webSocketPublisher.publicarPausa(sesion.getAtraccion().getId(), sesionId, request.motivo(), sesionDTO, colaDTO);

        return sesionDTO;
    }

    @Override
    public SesionJuegoResponseDTO reanudarSesion(Long sesionId, Authentication auth) {
        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario no autenticado para reanudar la sesión.");
        }

        SesionJuego sesion = sesionJuegoRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de juego no encontrada con ID: " + sesionId));

        validarEstacionAsignadaAlOperario(operario, sesion.getAtraccion());

        if (!EstadoSesionJuego.PAUSADA.equals(sesion.getEstadoSesion())) {
            throw new ReglaNegocioException("Solo se pueden reanudar sesiones que se encuentren PAUSADAS (estado actual: " + sesion.getEstadoSesion() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        int tiempoRestante = sesion.getTiempoRestantePausa() != null ? sesion.getTiempoRestantePausa() : 0;
        LocalDateTime nuevaHoraFin = now.plusSeconds(tiempoRestante);

        // Cerrar auditoría de pausa abierta
        Optional<LogIncidentePausa> logPausaOpt = logIncidentePausaRepository
                .findFirstBySesionJuegoIdAndFechaFinPausaIsNullOrderByFechaInicioPausaDesc(sesionId);
        logPausaOpt.ifPresent(p -> {
            p.setFechaFinPausa(now);
            long durSeg = Duration.between(p.getFechaInicioPausa(), now).toSeconds();
            p.setDuracionSegundos((int) Math.max(0, durSeg));
            logIncidentePausaRepository.save(p);
        });

        sesion.setEstadoSesion(EstadoSesionJuego.EN_CURSO);
        sesion.setHoraFin(nuevaHoraFin);
        sesion.setTiempoRestantePausa(null);
        sesion.setFechaPausa(null);
        sesionJuegoRepository.save(sesion);

        if (sesion.getTicket() != null) {
            sesion.getTicket().setEstadoTicket(EstadoTicket.EN_USO);
            ticketRepository.save(sesion.getTicket());
            auditoriaOperacionService.registrarTransicionTicket(
                    sesion.getTicket().getUuidCodigo(),
                    EstadoTicket.PAUSADO.name(),
                    EstadoTicket.EN_USO.name(),
                    "Reanudación de sesión individual",
                    operario.getId(),
                    "Terminal Operario"
            );
        }

        log.info("Sesión ID #{} reanudada por operario {}. Nueva hora fin: {}", sesionId, operario.getCorreo(), nuevaHoraFin);

        SesionJuegoResponseDTO sesionDTO = mapearSesionResponse(sesion);
        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(sesion.getAtraccion());

        webSocketPublisher.publicarReanudacion(sesion.getAtraccion().getId(), sesionId, sesionDTO, colaDTO);

        return sesionDTO;
    }

    @Override
    public ColaAtraccionResponseDTO pausaGeneral(Long atraccionId, PausaGeneralRequestDTO request, Authentication auth) {
        if (request == null || request.motivo() == null || request.motivo().isBlank()) {
            throw new ReglaNegocioException("El motivo de la pausa general es obligatorio.");
        }

        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario no autenticado para ejecutar la pausa general.");
        }

        Atraccion atraccion = atraccionRepository.findById(atraccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));

        validarEstacionAsignadaAlOperario(operario, atraccion);

        List<SesionJuego> sesionesEnCurso = sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(
                atraccionId, List.of(EstadoSesionJuego.EN_CURSO)
        );

        LocalDateTime now = LocalDateTime.now();
        String motivoTrimmed = request.motivo().trim();
        String motivoAuditoria = "Pausa general en estación " + atraccion.getNombre() + ": " + motivoTrimmed;

        for (SesionJuego s : sesionesEnCurso) {
            aplicarPausaASesion(s, operario, motivoTrimmed, motivoAuditoria, true, now);
        }

        log.warn("Pausa general aplicada en atracción {} por operario {}. Sesiones pausadas: {}. Motivo: {}",
                atraccion.getNombre(), operario.getCorreo(), sesionesEnCurso.size(), request.motivo());

        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(atraccion);
        webSocketPublisher.publicarPausaGeneral(atraccionId, request.motivo(), colaDTO);

        return colaDTO;
    }

    @Override
    public SesionJuegoResponseDTO registrarSalida(Long sesionId, Authentication auth) {
        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario no autenticado para registrar la salida.");
        }

        SesionJuego sesion = sesionJuegoRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de juego no encontrada con ID: " + sesionId));

        validarEstacionAsignadaAlOperario(operario, sesion.getAtraccion());

        if (EstadoSesionJuego.FINALIZADA.equals(sesion.getEstadoSesion())) {
            throw new ReglaNegocioException("La sesión de juego ID #" + sesionId + " ya se encuentra finalizada.");
        }

        LocalDateTime now = LocalDateTime.now();
        sesion.setEstadoSesion(EstadoSesionJuego.FINALIZADA);
        sesion.setFechaSalidaReal(now);

        // Calcular segundos de exceso si egresa tras expirar y tras agotar tolerancia (HU-31.02)
        if (sesion.getHoraFin() != null && now.isAfter(sesion.getHoraFin())) {
            int toleranciaMinutos = sesion.getAtraccion().getToleranciaMinutos() != null ? sesion.getAtraccion().getToleranciaMinutos() : 0;
            long segundosDespuesFin = Duration.between(sesion.getHoraFin(), now).toSeconds();
            long segundosTolerancia = toleranciaMinutos * 60L;
            if (segundosDespuesFin > segundosTolerancia) {
                sesion.setSegundosExceso((int) (segundosDespuesFin - segundosTolerancia));
            } else {
                sesion.setSegundosExceso(0);
            }
        }

        // Actualización atómica de ticket a CONSUMIDO (HU-32.03) y auditoría (HU-38.04)
        Ticket ticket = sesion.getTicket();
        consumirTicketDeSesion(sesion, operario, now, "Salida formal del menor de la atracción");

        sesionJuegoRepository.save(sesion);

        log.info("Salida formal registrada para sesión ID #{} (Menor: {}, Ticket: {}). Estado ticket -> CONSUMIDO",
                sesionId, sesion.getMenor() != null ? sesion.getMenor().getNombreCompleto() : "N/A",
                ticket != null ? ticket.getUuidCodigo() : "N/A");

        SesionJuegoResponseDTO sesionDTO = mapearSesionResponse(sesion);
        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(sesion.getAtraccion());

        // Notificación WebSocket (HU-32.05) y liberación de aforo (HU-32.04)
        webSocketPublisher.publicarSesionFinalizada(sesion.getAtraccion().getId(), sesionDTO, colaDTO);

        return sesionDTO;
    }

    @Override
    public SesionJuegoResponseDTO descontarIntento(Long sesionId, RegistroIntentoEventoDTO request, Authentication auth) {
        Usuario operario = resolverUsuarioAutenticado(auth);
        if (operario == null) {
            throw new ReglaNegocioException("Usuario no autenticado para descontar intento.");
        }

        SesionJuego sesion = sesionJuegoRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de juego no encontrada con ID: " + sesionId));

        validarEstacionAsignadaAlOperario(operario, sesion.getAtraccion());

        if (EstadoSesionJuego.FINALIZADA.equals(sesion.getEstadoSesion())) {
            throw new ReglaNegocioException("No se pueden descontar intentos a una sesión ya finalizada.");
        }

        int intentosActuales = sesion.getIntentosRestantes() != null ? sesion.getIntentosRestantes() : 1;
        int nuevosRestantes = Math.max(0, intentosActuales - 1);
        sesion.setIntentosRestantes(nuevosRestantes);

        LocalDateTime now = LocalDateTime.now();

        // Si los intentos llegan a 0, finalizar automáticamente (HU-33.04)
        if (nuevosRestantes == 0) {
            sesion.setEstadoSesion(EstadoSesionJuego.FINALIZADA);
            sesion.setFechaSalidaReal(now);
            consumirTicketDeSesion(sesion, operario, now, "Sesión finalizada por agotamiento de intentos");
            log.info("Sesión ID #{} finalizada automáticamente por agotamiento de intentos (Toro Mecánico)", sesionId);
        }

        sesionJuegoRepository.save(sesion);

        SesionJuegoResponseDTO sesionDTO = mapearSesionResponse(sesion);
        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(sesion.getAtraccion());

        if (nuevosRestantes == 0) {
            webSocketPublisher.publicarSesionFinalizada(sesion.getAtraccion().getId(), sesionDTO, colaDTO);
        } else {
            webSocketPublisher.publicarColaActualizada(sesion.getAtraccion().getId(), colaDTO);
        }

        return sesionDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public SincronizacionRelojResponseDTO sincronizarReloj(Long atraccionId) {
        Atraccion atraccion = atraccionRepository.findById(atraccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));

        ColaAtraccionResponseDTO colaDTO = construirColaAtraccion(atraccion);

        return new SincronizacionRelojResponseDTO(
                atraccion.getId(),
                atraccion.getNombre(),
                System.currentTimeMillis(),
                LocalDateTime.now(),
                colaDTO
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AforoActualDTO obtenerAforoActual(Long atraccionId) {
        Atraccion atraccion = atraccionRepository.findById(atraccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));

        int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        int ocupacionActiva = calcularOcupacionActiva(atraccionId);
        int cuposLibres = Math.max(0, capacidad - ocupacionActiva);
        double porcentaje = Math.min(100.0, ((double) ocupacionActiva / capacidad) * 100.0);
        boolean aforoCompleto = ocupacionActiva >= capacidad;

        return new AforoActualDTO(
                atraccion.getId(),
                atraccion.getNombre(),
                capacidad,
                ocupacionActiva,
                cuposLibres,
                Math.round(porcentaje * 10.0) / 10.0,
                aforoCompleto,
                LocalDateTime.now()
        );
    }

    // --- Métodos Auxiliares y Mapeos ---

    private void aplicarPausaASesion(
            SesionJuego sesion,
            Usuario operario,
            String motivoTrimmed,
            String motivoAuditoria,
            boolean esPausaGeneral,
            LocalDateTime now
    ) {
        long diff = sesion.getHoraFin() != null ? Duration.between(now, sesion.getHoraFin()).toSeconds() : 0;
        int segundosRestantes = (int) Math.max(0, diff);

        sesion.setEstadoSesion(EstadoSesionJuego.PAUSADA);
        sesion.setTiempoRestantePausa(segundosRestantes);
        sesion.setFechaPausa(now);
        sesionJuegoRepository.save(sesion);

        if (sesion.getTicket() != null) {
            sesion.getTicket().setEstadoTicket(EstadoTicket.PAUSADO);
            ticketRepository.save(sesion.getTicket());
            auditoriaOperacionService.registrarTransicionTicket(
                    sesion.getTicket().getUuidCodigo(),
                    EstadoTicket.EN_USO.name(),
                    EstadoTicket.PAUSADO.name(),
                    motivoAuditoria,
                    operario.getId(),
                    "Terminal Operario"
            );
        }

        String motivoLower = motivoTrimmed.toLowerCase();
        String tipoPausa = (motivoLower.contains("tecnic") ||
                            motivoLower.contains("mantenimiento") ||
                            motivoLower.contains("falla")) ? "TECNICA" : "OPERATIVA";

        LogIncidentePausa logPausa = LogIncidentePausa.builder()
                .sesionJuego(sesion)
                .atraccion(sesion.getAtraccion())
                .operario(operario)
                .motivo(motivoTrimmed)
                .tipoPausa(tipoPausa)
                .esPausaGeneral(esPausaGeneral)
                .fechaInicioPausa(now)
                .build();
        logIncidentePausaRepository.save(logPausa);
    }

    private void consumirTicketDeSesion(
            SesionJuego sesion,
            Usuario operario,
            LocalDateTime now,
            String motivoAuditoria
    ) {
        Ticket ticket = sesion.getTicket();
        if (ticket != null) {
            String estadoAnterior = ticket.getEstadoTicket() != null ? ticket.getEstadoTicket().name() : "EN_USO";
            ticket.setEstadoTicket(EstadoTicket.CONSUMIDO);
            ticket.setFechaConsumo(now);
            ticketRepository.save(ticket);
            auditoriaOperacionService.registrarTransicionTicket(
                    ticket.getUuidCodigo(),
                    estadoAnterior,
                    EstadoTicket.CONSUMIDO.name(),
                    motivoAuditoria,
                    operario.getId(),
                    "Terminal Operario"
            );
        }
    }

    private ColaAtraccionResponseDTO construirColaAtraccion(Atraccion atraccion) {
        int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        List<SesionJuego> activas = sesionJuegoRepository.findByAtraccionIdAndEstadoSesionIn(
                atraccion.getId(), List.of(EstadoSesionJuego.EN_CURSO, EstadoSesionJuego.PAUSADA)
        );

        List<SesionJuegoResponseDTO> dtos = new ArrayList<>();
        for (SesionJuego s : activas) {
            dtos.add(mapearSesionResponse(s));
        }

        int ocupacion = dtos.size();
        int cupos = Math.max(0, capacidad - ocupacion);

        return new ColaAtraccionResponseDTO(
                atraccion.getId(),
                atraccion.getNombre(),
                capacidad,
                ocupacion,
                cupos,
                dtos,
                LocalDateTime.now()
        );
    }

    @Override
    public SesionJuegoResponseDTO mapearSesionResponse(SesionJuego sesion) {
        Atraccion at = sesion.getAtraccion();
        Menor menor = sesion.getMenor();
        Ticket ticket = sesion.getTicket();
        DetalleVenta detalle = ticket != null ? ticket.getDetalleVenta() : null;
        Venta venta = detalle != null ? detalle.getVenta() : null;
        Cliente cliente = venta != null ? venta.getCliente() : (menor != null ? menor.getCliente() : null);
        Usuario op = sesion.getOperario();

        int segundosRestantes;
        LocalDateTime now = LocalDateTime.now();
        if (EstadoSesionJuego.PAUSADA.equals(sesion.getEstadoSesion())) {
            segundosRestantes = sesion.getTiempoRestantePausa() != null ? sesion.getTiempoRestantePausa() : 0;
        } else if (EstadoSesionJuego.EN_CURSO.equals(sesion.getEstadoSesion())) {
            if (sesion.getHoraFin() != null) {
                long diff = Duration.between(now, sesion.getHoraFin()).toSeconds();
                segundosRestantes = (int) Math.max(0, diff);
            } else {
                segundosRestantes = 0;
            }
        } else {
            segundosRestantes = 0;
        }

        boolean preExp = Boolean.TRUE.equals(sesion.getAlertaPreExpiracionEnviada()) ||
                (EstadoSesionJuego.EN_CURSO.equals(sesion.getEstadoSesion()) && segundosRestantes <= 120 && segundosRestantes > 0);
        boolean tiempoExp = EstadoSesionJuego.EN_CURSO.equals(sesion.getEstadoSesion()) && segundosRestantes <= 0;

        return new SesionJuegoResponseDTO(
                sesion.getId(),
                ticket != null ? ticket.getUuidCodigo() : null,
                at != null ? at.getId() : null,
                at != null ? at.getNombre() : "Atracción",
                menor != null ? menor.getId() : null,
                menor != null ? menor.getNombreCompleto() : "Menor",
                menor != null ? (menor.getEdadActual() != null ? menor.getEdadActual() : menor.getEdad()) : null,
                cliente != null ? cliente.getNombreCompleto() : "Acudiente",
                cliente != null ? cliente.getTelefono() : "N/A",
                cliente != null ? cliente.getNumeroDocumento() : "N/A",
                op != null ? op.getNombre() : "Operario",
                op != null ? op.getCorreo() : "N/A",
                sesion.getHoraInicio(),
                sesion.getHoraFin(),
                sesion.getDuracionSegundos(),
                segundosRestantes,
                sesion.getTiempoRestantePausa(),
                sesion.getFechaPausa(),
                sesion.getEstadoSesion() != null ? sesion.getEstadoSesion().name() : "EN_CURSO",
                sesion.getSegundosExceso(),
                sesion.getIntentosTotales(),
                sesion.getIntentosRestantes(),
                preExp,
                tiempoExp
        );
    }

    private void validarRangoEdadMenor(
            Menor menor,
            Atraccion atraccionEstacion,
            UUID uuid,
            Usuario operario,
            String clientIp,
            LocalDateTime timestamp
    ) {
        if (menor != null) {
            Integer edad = menor.getEdadActual() != null ? menor.getEdadActual() : menor.getEdad();
            if (edad != null) {
                if (atraccionEstacion.getEdadMinima() != null && edad < atraccionEstacion.getEdadMinima()) {
                    String motivo = "El menor " + menor.getNombreCompleto() + " (" + edad + " años) no cumple con la edad mínima requerida (" +
                            atraccionEstacion.getEdadMinima() + " años) para la atracción " + atraccionEstacion.getNombre() + ".";
                    registrarLogEscaneo(uuid, true, atraccionEstacion, operario, clientIp,
                            timestamp, "RECHAZADO_EDAD_MINIMA", motivo);
                    throw new TicketInvalidoOperacionException(motivo, HttpStatus.UNPROCESSABLE_ENTITY);
                }
                if (atraccionEstacion.getEdadMaxima() != null && edad > atraccionEstacion.getEdadMaxima()) {
                    String motivo = "El menor " + menor.getNombreCompleto() + " (" + edad + " años) supera la edad máxima permitida (" +
                            atraccionEstacion.getEdadMaxima() + " años) para la atracción " + atraccionEstacion.getNombre() + ".";
                    registrarLogEscaneo(uuid, true, atraccionEstacion, operario, clientIp,
                            timestamp, "RECHAZADO_EDAD_MAXIMA", motivo);
                    throw new TicketInvalidoOperacionException(motivo, HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }
    }

    private void validarEstacionAsignadaAlOperario(Usuario operario, Atraccion atraccionEstacion) {
        if (operario.getRol() != null && NombreRol.OPERARIO.equals(operario.getRol().getNombre())) {
            AsignacionOperario asignacion = resolverAsignacionActiva(operario);
            if (asignacion == null || !asignacion.getAtraccion().getId().equals(atraccionEstacion.getId())) {
                String asignadaNombre = asignacion != null ? asignacion.getAtraccion().getNombre() : "Ninguna";
                throw new EstacionNoAutorizadaException(
                        "No está autorizado para operar o validar boletas en una atracción distinta a su estación asignada. " +
                                "Estación asignada: " + asignadaNombre + ", estación solicitada: " + atraccionEstacion.getNombre()
                );
            }
        }
    }

    private AsignacionOperario resolverAsignacionActiva(Usuario usuario) {
        return asignacionRepository.findFirstByUsuarioIdAndFechaAsignacionAndEstadoOrderByHoraInicioDesc(
                usuario.getId(), LocalDate.now(), EstadoAsignacion.ACTIVA
        ).orElseGet(() ->
                asignacionRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoAsignacion.ACTIVA).orElse(null)
        );
    }

    private int calcularOcupacionActiva(Long atraccionId) {
        int sesionesActivas = sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(
                atraccionId, List.of(EstadoSesionJuego.EN_CURSO, EstadoSesionJuego.PAUSADA)
        );

        if (sesionesActivas > 0) {
            return sesionesActivas;
        }

        LocalDate hoy = LocalDate.now();
        List<Ticket> ticketsEnUso = ticketRepository.findByAtraccionIdAndEstadoTicketAndFechaEmisionBetween(
                atraccionId,
                EstadoTicket.EN_USO,
                hoy.atStartOfDay(),
                hoy.atTime(23, 59, 59)
        );
        return ticketsEnUso.size();
    }

    private Integer resolverMinutosAutorizados(DetalleVenta detalle, Atraccion atraccion) {
        if (detalle == null) {
            return atraccion.getDuracionMinutosBase();
        }
        if (detalle.getTarifaTiempoId() != null) {
            TarifaTiempo tt = tarifaTiempoRepository.findById(detalle.getTarifaTiempoId()).orElse(null);
            if (tt != null && tt.getDuracionMinutos() != null) {
                return tt.getDuracionMinutos();
            }
        }
        return atraccion.getDuracionMinutosBase() != null ? atraccion.getDuracionMinutosBase() : 15;
    }

    private Integer resolverIntentosAutorizados(DetalleVenta detalle, Atraccion atraccion) {
        if (detalle == null) {
            return atraccion.getCaidasLimite();
        }
        if (detalle.getTarifaEventoId() != null) {
            TarifaEvento te = tarifaEventoRepository.findById(detalle.getTarifaEventoId()).orElse(null);
            if (te != null && te.getIntentos() != null) {
                return te.getIntentos();
            }
        }
        return atraccion.getCaidasLimite() != null ? atraccion.getCaidasLimite() : 3;
    }

    private void registrarLogEscaneo(
            UUID uuid,
            boolean encontrado,
            Atraccion atraccion,
            Usuario operario,
            String ip,
            LocalDateTime timestampCliente,
            String resultado,
            String detalle
    ) {
        try {
            String ipSanitizada = ip;
            if (ipSanitizada != null && ipSanitizada.length() > 64) {
                ipSanitizada = ipSanitizada.substring(0, 64);
            }
            LogEscaneoQr logEntity = LogEscaneoQr.builder()
                    .uuidLeido(uuid)
                    .ticketEncontrado(encontrado)
                    .atraccion(atraccion)
                    .operario(operario)
                    .direccionIp(ipSanitizada)
                    .timestampCliente(timestampCliente)
                    .fechaEscaneo(LocalDateTime.now())
                    .resultado(resultado)
                    .detalle(detalle)
                    .build();
            logEscaneoQrRepository.save(logEntity);
        } catch (Exception ex) {
            log.error("Error al persistir registro de auditoría en log_escaneo_qr: {}", ex.getMessage());
        }
    }

    private Usuario resolverUsuarioAutenticado(Authentication auth) {
        if (authenticatedUserProvider != null) {
            return authenticatedUserProvider.resolverUsuario(auth).orElse(null);
        }
        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
        }
        return null;
    }
}
