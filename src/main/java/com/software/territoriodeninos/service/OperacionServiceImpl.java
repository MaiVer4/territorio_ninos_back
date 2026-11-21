package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AforoActualDTO;
import com.software.territoriodeninos.dto.AutorizarSobreaforoRequestDTO;
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
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.CuentaBloqueadaException;
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
import com.software.territoriodeninos.security.LoginAttemptService;
import com.software.territoriodeninos.util.EstadoAsignacion;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.NombreRol;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final PasswordEncoder passwordEncoder;

    private static final long SOBREAFORO_TTL_SECONDS = 300L;
    private static final int MAX_SOBREAFORO_ENTRIES = 1_000;

    @Setter(onMethod_ = {@org.springframework.beans.factory.annotation.Autowired(required = false)})
    private LoginAttemptService loginAttemptService;

    private final Map<String, Instant> autorizacionesSobreaforo = new ConcurrentHashMap<>();

    @Setter
    @Value("${app.operacion.supervisor-pin:}")
    private String supervisorPin = "";

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
            @org.springframework.beans.factory.annotation.Autowired(required = false) AuthenticatedUserProvider authenticatedUserProvider,
            @org.springframework.beans.factory.annotation.Autowired(required = false) PasswordEncoder passwordEncoder
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
        this.passwordEncoder = passwordEncoder;
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

        return construirEstacionTrabajoDTO(usuario, asignacion.getAtraccion(), asignacion);
    }

    private EstacionTrabajoDTO construirEstacionTrabajoDTO(Usuario usuario, Atraccion atraccion, AsignacionOperario asignacion) {
        int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        int aforoActual = calcularOcupacionActiva(atraccion.getId());
        int cuposDisponibles = Math.max(0, capacidad - aforoActual);

        String rangoEdad = EstacionTrabajoDTO.formatearRangoEdad(atraccion.getEdadMinima(), atraccion.getEdadMaxima());

        return new EstacionTrabajoDTO(
                asignacion != null ? asignacion.getId() : null,
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
                asignacion != null ? asignacion.getFechaAsignacion() : LocalDate.now(),
                asignacion != null ? asignacion.getHoraInicio() : LocalDateTime.now()
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

        if (atraccionEstacion.getEstado() != null && atraccionEstacion.getEstado() != EstadoAtraccion.ACTIVO) {
            throw new TicketInvalidoOperacionException(
                    "La atracción '" + atraccionEstacion.getNombre() + "' se encuentra " +
                            atraccionEstacion.getEstado().name().replace("_", " ").toLowerCase() +
                            " y no permite nuevos ingresos.",
                    HttpStatus.CONFLICT
            );
        }

        // 2. Control de Aforo en tiempo real (HU-34.01, HU-34.02 -> HTTP 409)
        int capacidadMaxima = atraccionEstacion.getCapacidadMaxima() != null ? atraccionEstacion.getCapacidadMaxima() : 1;
        int ocupacionActiva = calcularOcupacionActiva(atraccionEstacion.getId());

        // Resolución flexible de ticket por UUID estándar o prefijo/código corto (HU-25.01)
        UUID ticketUuid = null;
        String codigoRaw = request.uuid() != null ? request.uuid().trim() : "";
        try {
            ticketUuid = UUID.fromString(codigoRaw);
        } catch (IllegalArgumentException ignored) {
            // Formato no estándar, se procederá a búsqueda por prefijo o código
        }

        Ticket ticket = null;
        if (ticketUuid != null) {
            ticket = ticketRepository.findByUuidCodigo(ticketUuid).orElse(null);
        } else if (!codigoRaw.isBlank()) {
            String prefijo = codigoRaw.toLowerCase();
            if (prefijo.startsWith("tdn-qr:")) {
                prefijo = prefijo.substring(7);
            }
            if (prefijo.startsWith("bol-")) {
                prefijo = prefijo.substring(4);
            } else if (prefijo.startsWith("tdn-")) {
                // Soporta formato visible de boleta POS: TDN-{atraccionId}-{prefijoUuid}-{consecutivo} o TDN-{prefijoUuid}
                String[] segmentos = prefijo.split("-");
                if (segmentos.length >= 3 && segmentos[2].matches("[0-9a-f]{4,12}")) {
                    prefijo = segmentos[2];
                } else if (segmentos.length >= 2 && segmentos[1].matches("[0-9a-f]{4,12}")) {
                    prefijo = segmentos[1];
                }
            }
            List<Ticket> coincidentes = ticketRepository.findByUuidCodigoStartingWith(prefijo);
            if (!coincidentes.isEmpty()) {
                ticket = coincidentes.get(0);
                ticketUuid = ticket.getUuidCodigo();
            }
        }

        if (ticket != null && ticketUuid == null) {
            ticketUuid = ticket.getUuidCodigo();
        }

        boolean sobreaforoAutorizado = false;
        if (ocupacionActiva >= capacidadMaxima) {
            if (tieneSobreaforoVigente(operario.getId(), atraccionEstacion.getId())) {
                sobreaforoAutorizado = true;
            } else {
                registrarLogEscaneo(ticketUuid, false, atraccionEstacion, operario, clientIp,
                        request.timestamp(), "RECHAZADO_AFORO_COMPLETO",
                        "Aforo completo (" + ocupacionActiva + "/" + capacidadMaxima + ")");
                throw new AforoCompletoException(
                        "La atracción '" + atraccionEstacion.getNombre() + "' ha alcanzado el 100% de su capacidad máxima (" +
                                ocupacionActiva + "/" + capacidadMaxima + " niños). No se permiten nuevos ingresos."
                );
            }
        }

        // 3. Validación algorítmica de existencia y estado PAGADO (HU-26.03 / CA-01 -> HTTP 409 si CONSUMIDO/ANULADO)
        if (ticket == null) {
            registrarLogEscaneo(ticketUuid, false, atraccionEstacion, operario, clientIp,
                    request.timestamp(), "RECHAZADO_NO_ENCONTRADO", "Ticket inexistente");
            throw new RecursoNoEncontradoException("Ticket no encontrado con el código o UUID: " + request.uuid());
        }

        if (!EstadoTicket.PAGADO.equals(ticket.getEstadoTicket())) {
            String motivo = "El ticket no se encuentra en estado PAGADO (estado actual: " + ticket.getEstadoTicket() + ").";
            registrarLogEscaneo(ticketUuid, true, atraccionEstacion, operario, clientIp,
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
            registrarLogEscaneo(ticketUuid, true, atraccionEstacion, operario, clientIp,
                    request.timestamp(), "RECHAZADO_ATRACCION_DISTINTA", motivo);
            throw new TicketInvalidoOperacionException(motivo, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // 5. Validación automática del rango de edad del menor (HU-26.05 / CA-03 -> HTTP 422)
        Menor menor = ticket.getMenor() != null ? ticket.getMenor() : (detalle != null ? detalle.getMenor() : null);
        validarRangoEdadMenor(menor, atraccionEstacion, ticketUuid, operario, clientIp, request.timestamp());

        // 6. Resolver minutos o intentos contratados
        Integer minutosAutorizados = resolverMinutosAutorizados(detalle, atraccionEstacion);
        Integer intentosAutorizados = resolverIntentosAutorizados(detalle, atraccionEstacion);

        // 7. Registro de auditoría exitoso en base de datos (HU-25.03)
        registrarLogEscaneo(ticketUuid, true, atraccionEstacion, operario, clientIp,
                request.timestamp(),
                sobreaforoAutorizado ? "VALIDADO_SOBREAFORO" : "VALIDADO_EXITOSO",
                sobreaforoAutorizado
                        ? "Validación completada con sobreaforo autorizado por supervisor"
                        : "Validación completada satisfactoriamente");

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

        if (atraccion.getEstado() != null && atraccion.getEstado() != EstadoAtraccion.ACTIVO) {
            throw new TicketInvalidoOperacionException(
                    "La atracción '" + atraccion.getNombre() + "' se encuentra " +
                            atraccion.getEstado().name().replace("_", " ").toLowerCase() +
                            " y no permite nuevos ingresos.",
                    HttpStatus.CONFLICT
            );
        }

        // Control de aforo (con soporte a permiso de sobreaforo de un solo uso autorizado por supervisor)
        int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
        int ocupacionActiva = calcularOcupacionActiva(atraccion.getId());
        boolean ingresoConSobreaforo = false;
        if (ocupacionActiva >= capacidad) {
            if (consumirSobreaforoVigente(operario.getId(), atraccion.getId())) {
                ingresoConSobreaforo = true;
            } else {
                throw new AforoCompletoException(
                        "Aforo completo (" + ocupacionActiva + "/" + capacidad + "). No es posible ingresar más participantes a " + atraccion.getNombre()
                );
            }
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
                ingresoConSobreaforo
                        ? "Inicio de sesión con sobreaforo autorizado en atracción: " + atraccion.getNombre()
                        : "Inicio de sesión en atracción: " + atraccion.getNombre(),
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

        // Difusión en tiempo real por WebSocket tras confirmar transacción (HU-27.06, HU-28.03)
        final Long atrId = atraccion.getId();
        ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarNuevoIngreso(atrId, sesionDTO, colaDTO));

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

        final Long atrId = sesion.getAtraccion().getId();
        final String motivo = request.motivo();
        ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarPausa(atrId, sesionId, motivo, sesionDTO, colaDTO));

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

        final Long atrId = sesion.getAtraccion().getId();
        ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarReanudacion(atrId, sesionId, sesionDTO, colaDTO));

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
        final String motivo = request.motivo();
        ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarPausaGeneral(atraccionId, motivo, colaDTO));

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

        // Notificación WebSocket tras commit (HU-32.05) y liberación de aforo (HU-32.04)
        final Long atrId = sesion.getAtraccion().getId();
        ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarSesionFinalizada(atrId, sesionDTO, colaDTO));

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

        final Long atrId = sesion.getAtraccion().getId();
        if (nuevosRestantes == 0) {
            ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarSesionFinalizada(atrId, sesionDTO, colaDTO));
        } else {
            ejecutarDespuesDeCommit(() -> webSocketPublisher.publicarColaActualizada(atrId, colaDTO));
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

    @Override
    @Transactional
    public EstacionTrabajoDTO vincularEstacion(Long atraccionId, Authentication auth) {
        if (atraccionId == null) {
            throw new ReglaNegocioException("El ID de la atracción es obligatorio para la vinculación.");
        }

        Usuario usuario = resolverUsuarioAutenticado(auth);
        if (usuario == null) {
            throw new ReglaNegocioException("Usuario no autenticado o sesión no válida.");
        }

        // Lock pesimista para serializar vinculaciones concurrentes sobre la misma atracción (evita TOCTOU)
        Atraccion atraccion = atraccionRepository.findByIdForUpdate(atraccionId)
                .or(() -> atraccionRepository.findById(atraccionId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + atraccionId));

        if (atraccion.getEstado() != EstadoAtraccion.ACTIVO) {
            throw new ConflictoException("La atracción '" + atraccion.getNombre() + "' no se encuentra activa");
        }

        // Coordinador y Administrador tienen acceso libre a cualquier atracción activa sin desplazar al operario titular
        NombreRol rolUsuario = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        if (NombreRol.COORDINADOR.equals(rolUsuario) || NombreRol.ADMINISTRADOR.equals(rolUsuario)) {
            log.info("Acceso libre operativo concedido a {} ({}) en atracción {}", usuario.getCorreo(), rolUsuario, atraccion.getNombre());
            return construirEstacionTrabajoDTO(usuario, atraccion, null);
        }

        // Validar que la atracción no esté ocupada por otro operario en turno activo
        List<AsignacionOperario> activasEnAtraccion = asignacionRepository.findByAtraccionIdAndEstado(atraccionId, EstadoAsignacion.ACTIVA);
        for (AsignacionOperario asig : activasEnAtraccion) {
            if (!asig.getUsuario().getId().equals(usuario.getId())) {
                String nombreOtro = asig.getUsuario().getNombre();
                throw new ConflictoException("La atracción '" + atraccion.getNombre() + "' ya tiene asignado al operario " + nombreOtro + " en turno.");
            }
        }

        // Revisar asignación previa del operario actual
        AsignacionOperario miAsignacionActiva = resolverAsignacionActiva(usuario);
        if (miAsignacionActiva != null) {
            if (miAsignacionActiva.getAtraccion().getId().equals(atraccionId)) {
                return construirEstacionTrabajoDTO(usuario, atraccion, miAsignacionActiva);
            }
            miAsignacionActiva.setEstado(EstadoAsignacion.REASIGNADA);
            miAsignacionActiva.setHoraFin(LocalDateTime.now());
            // Flush explícito para no violar ux_asignacion_operario_activa antes de insertar la nueva asignación
            asignacionRepository.saveAndFlush(miAsignacionActiva);
            log.info("Operario {} reasignado desde {} a {}", usuario.getCorreo(), miAsignacionActiva.getAtraccion().getNombre(), atraccion.getNombre());
        }

        AsignacionOperario nuevaAsignacion = AsignacionOperario.builder()
                .usuario(usuario)
                .atraccion(atraccion)
                .fechaAsignacion(LocalDate.now())
                .horaInicio(LocalDateTime.now())
                .estado(EstadoAsignacion.ACTIVA)
                .observaciones("Vinculación confirmada por operario en consola de campo")
                .build();
        AsignacionOperario guardada = asignacionRepository.save(nuevaAsignacion);

        log.info("Operario {} vinculado exitosamente a atracción {}", usuario.getCorreo(), atraccion.getNombre());
        return construirEstacionTrabajoDTO(usuario, atraccion, guardada != null ? guardada : nuevaAsignacion);
    }

    @Override
    @Transactional
    public void desvincularEstacion(Authentication auth) {
        Usuario usuario = resolverUsuarioAutenticado(auth);
        if (usuario == null) {
            return;
        }

        AsignacionOperario asignacion = resolverAsignacionActiva(usuario);
        if (asignacion != null) {
            asignacion.setEstado(EstadoAsignacion.FINALIZADA);
            asignacion.setHoraFin(LocalDateTime.now());
            asignacionRepository.save(asignacion);
            log.info("Operario {} desvinculó su estación activa de {}", usuario.getCorreo(), asignacion.getAtraccion().getNombre());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void autorizarSobreaforo(AutorizarSobreaforoRequestDTO request, Authentication auth) {
        if (request == null || request.atraccionId() == null
                || request.claveSupervisor() == null || request.claveSupervisor().isBlank()) {
            throw new ReglaNegocioException("El ID de la atracción y la clave de supervisor son obligatorios.");
        }

        Usuario usuario = resolverUsuarioAutenticado(auth);
        if (usuario == null) {
            throw new ReglaNegocioException("Usuario no autenticado o sesión no válida.");
        }

        Atraccion atraccion = atraccionRepository.findById(request.atraccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + request.atraccionId()));

        if (atraccion.getEstado() != EstadoAtraccion.ACTIVO) {
            throw new ConflictoException("La atracción '" + atraccion.getNombre() + "' no se encuentra activa.");
        }

        String rateLimitKey = "sobreaforo:" + (usuario.getCorreo() != null ? usuario.getCorreo().toLowerCase() : usuario.getId());
        if (loginAttemptService != null && loginAttemptService.isBlocked(rateLimitKey)) {
            long minutos = loginAttemptService.getRemainingLockoutMinutes(rateLimitKey);
            throw new CuentaBloqueadaException(
                    "Demasiados intentos fallidos de autorización de sobreaforo. Intente nuevamente en " + minutos + " minutos."
            );
        }

        String pinIngresado = request.claveSupervisor().trim();
        if (!validarCredencialSupervisor(pinIngresado, usuario)) {
            if (loginAttemptService != null) {
                loginAttemptService.loginFailed(rateLimitKey);
            }
            throw new AccessDeniedException("PIN o credencial de supervisor inválida.");
        }

        if (loginAttemptService != null) {
            loginAttemptService.loginSucceeded(rateLimitKey);
        }

        registrarSobreaforoVigente(usuario.getId(), atraccion.getId());

        String motivo = (request.motivo() != null && !request.motivo().isBlank())
                ? request.motivo().trim()
                : "Override de capacidad máxima autorizado";
        log.warn("Sobreaforo autorizado en atracción '{}' (ID {}) por usuario {} (válido {}s, 1 uso). Motivo: {}",
                atraccion.getNombre(), atraccion.getId(), usuario.getCorreo(), SOBREAFORO_TTL_SECONDS, motivo);
    }

    private String claveSobreaforo(Long usuarioId, Long atraccionId) {
        return usuarioId + ":" + atraccionId;
    }

    private void registrarSobreaforoVigente(Long usuarioId, Long atraccionId) {
        if (usuarioId == null || atraccionId == null) {
            return;
        }
        Instant ahora = Instant.now();
        if (autorizacionesSobreaforo.size() >= MAX_SOBREAFORO_ENTRIES) {
            autorizacionesSobreaforo.entrySet().removeIf(e -> e.getValue().isBefore(ahora));
        }
        autorizacionesSobreaforo.put(claveSobreaforo(usuarioId, atraccionId), ahora.plusSeconds(SOBREAFORO_TTL_SECONDS));
    }

    private boolean tieneSobreaforoVigente(Long usuarioId, Long atraccionId) {
        if (usuarioId == null || atraccionId == null) {
            return false;
        }
        String key = claveSobreaforo(usuarioId, atraccionId);
        Instant expiracion = autorizacionesSobreaforo.get(key);
        if (expiracion == null) {
            return false;
        }
        if (Instant.now().isAfter(expiracion)) {
            autorizacionesSobreaforo.remove(key, expiracion);
            return false;
        }
        return true;
    }

    private boolean consumirSobreaforoVigente(Long usuarioId, Long atraccionId) {
        if (usuarioId == null || atraccionId == null) {
            return false;
        }
        String key = claveSobreaforo(usuarioId, atraccionId);
        Instant expiracion = autorizacionesSobreaforo.remove(key);
        return expiracion != null && !Instant.now().isAfter(expiracion);
    }

    private boolean validarCredencialSupervisor(String pinIngresado, Usuario usuarioActual) {
        if (supervisorPin != null && !supervisorPin.isBlank()) {
            byte[] esperado = supervisorPin.trim().getBytes(StandardCharsets.UTF_8);
            byte[] recibido = pinIngresado.getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(esperado, recibido)) {
                return true;
            }
        }

        if (passwordEncoder == null) {
            return false;
        }

        NombreRol rolActual = usuarioActual.getRol() != null ? usuarioActual.getRol().getNombre() : null;
        if ((NombreRol.ADMINISTRADOR.equals(rolActual) || NombreRol.COORDINADOR.equals(rolActual))
                && usuarioActual.getPassword() != null
                && passwordEncoder.matches(pinIngresado, usuarioActual.getPassword())) {
            return true;
        }

        return coincidePasswordSupervisor(usuarioRepository.findByRolNombre(NombreRol.ADMINISTRADOR), pinIngresado)
                || coincidePasswordSupervisor(usuarioRepository.findByRolNombre(NombreRol.COORDINADOR), pinIngresado);
    }

    private boolean coincidePasswordSupervisor(List<Usuario> supervisores, String pinIngresado) {
        if (supervisores == null) {
            return false;
        }
        for (Usuario sup : supervisores) {
            if (sup != null && sup.estaActivo() && sup.getPassword() != null
                    && passwordEncoder.matches(pinIngresado, sup.getPassword())) {
                return true;
            }
        }
        return false;
    }

    private void ejecutarDespuesDeCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
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
        // Fuente única de aforo: sesiones EN_CURSO/PAUSADA
        return sesionJuegoRepository.countByAtraccionIdAndEstadoSesionIn(
                atraccionId, List.of(EstadoSesionJuego.EN_CURSO, EstadoSesionJuego.PAUSADA)
        );
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
