package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.ArqueoCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.exception.TurnoConflictoException;
import com.software.territoriodeninos.repository.TurnoCajaRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.AuthenticatedUserProvider;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.NombreRol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación de la gestión transaccional de turnos de caja y arqueos.
 * Aplica principios SOLID: Inversión de Dependencias (DIP) con AuthenticatedUserProvider,
 * Responsabilidad Única (SRP) mediante helpers atómicos y eliminación de redundancias (DRY).
 */
@Service
public class TurnoCajaServiceImpl implements TurnoCajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Autowired
    public TurnoCajaServiceImpl(
            TurnoCajaRepository turnoCajaRepository,
            UsuarioRepository usuarioRepository,
            @Autowired(required = false) AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.authenticatedUserProvider = authenticatedUserProvider != null
                ? authenticatedUserProvider
                : new AuthenticatedUserProvider(usuarioRepository);
    }

    public TurnoCajaServiceImpl(TurnoCajaRepository turnoCajaRepository, UsuarioRepository usuarioRepository) {
        this(turnoCajaRepository, usuarioRepository, new AuthenticatedUserProvider(usuarioRepository));
    }

    @Override
    @Transactional
    public TurnoCajaResponseDTO abrirTurno(AperturaCajaDTO dto, Authentication auth) {
        Usuario usuario = obtenerUsuarioYValidarRol(auth);
        validarSinTurnoActivoPrevio(usuario.getId());

        TurnoCaja nuevoTurno = TurnoCaja.builder()
                .usuarioId(usuario.getId())
                .montoBase(dto.montoBase())
                .fechaApertura(LocalDateTime.now())
                .ventasEfectivoSistema(BigDecimal.ZERO)
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones(dto.observaciones())
                .build();

        TurnoCaja guardado = turnoCajaRepository.save(nuevoTurno);
        return TurnoCajaResponseDTO.fromEntity(guardado, usuario.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoCajaResponseDTO> obtenerTurnoActivo(Authentication auth) {
        Usuario usuario = obtenerUsuarioYValidarRol(auth);
        boolean esTaquillero = esUsuarioTaquillero(usuario);

        return turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), EstadoTurnoCaja.ABIERTO)
                .map(turno -> esTaquillero
                        ? TurnoCajaResponseDTO.fromEntityCiego(turno, usuario.getNombre())
                        : TurnoCajaResponseDTO.fromEntity(turno, usuario.getNombre()));
    }

    @Override
    @Transactional
    public TurnoCajaResponseDTO cerrarTurno(CierreCajaDTO dto, Authentication auth) {
        Usuario usuario = obtenerUsuarioYValidarRol(auth);
        TurnoCaja turno = obtenerTurnoAbiertoPorUsuario(usuario.getId());

        BigDecimal totalEsperado = calcularTotalEsperadoEfectivo(turno);
        BigDecimal montoReportado = dto.montoEfectivoReportado();
        BigDecimal diferencia = montoReportado.subtract(totalEsperado);

        validarJustificacionDiferencia(diferencia, dto.observaciones());

        turno.setFechaCierre(LocalDateTime.now());
        turno.setMontoEfectivoReportado(montoReportado);
        turno.setDiferencia(diferencia);
        turno.setEstado(EstadoTurnoCaja.CERRADO);

        if (StringUtils.hasText(dto.observaciones())) {
            turno.setObservaciones(adjuntarObservacion(turno.getObservaciones(), "Cierre", dto.observaciones().trim()));
        }

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        return TurnoCajaResponseDTO.fromEntity(guardado, usuario.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoCajaResponseDTO> obtenerHistorialTurnos(String fecha) {
        return obtenerHistorialTurnos(fecha, SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoCajaResponseDTO> obtenerHistorialTurnos(String fecha, Authentication auth) {
        Usuario usuario = resolverUsuarioEnSesion(auth);
        boolean soloPropios = esUsuarioTaquillero(usuario);

        List<TurnoCaja> turnos = consultarTurnosSegunFiltros(fecha, soloPropios, usuario != null ? usuario.getId() : null);
        Map<Long, String> nombresUsuarios = resolverNombresCajeros(turnos);

        return turnos.stream()
                .map(t -> {
                    String nombreCajero = nombresUsuarios.getOrDefault(t.getUsuarioId(), "Cajero #" + t.getUsuarioId());
                    return TurnoCajaResponseDTO.fromEntity(t, nombreCajero);
                })
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> registrarArqueo(Long idTurno, ArqueoCajaDTO dto, Authentication auth) {
        if (idTurno == null) {
            throw new ReglaNegocioException("El ID del turno es obligatorio");
        }
        Usuario usuario = obtenerUsuarioYValidarRol(auth);
        TurnoCaja turno = turnoCajaRepository.findById(idTurno)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja con ID " + idTurno + " no encontrado"));

        validarTurnoParaArqueo(turno, usuario);

        BigDecimal montoRecontado = (dto != null && dto.montoEfectivoRecontado() != null)
                ? dto.montoEfectivoRecontado()
                : BigDecimal.ZERO;

        String detalleArqueo = String.format("Arqueo intermedio: monto recontado $%s", montoRecontado);
        if (dto != null && StringUtils.hasText(dto.observaciones())) {
            detalleArqueo += " | " + dto.observaciones().trim();
        }

        turno.setObservaciones(adjuntarObservacion(turno.getObservaciones(), null, detalleArqueo));
        turnoCajaRepository.save(turno);

        return Map.of(
                "mensaje", "Arqueo registrado exitosamente",
                "idTurno", turno.getId(),
                "montoRecontado", montoRecontado
        );
    }

    // ==========================================
    // HELPERS PRIVADOS DE LÓGICA Y SEGURIDAD (SRP)
    // ==========================================

    private Usuario resolverUsuarioEnSesion(Authentication auth) {
        Usuario usuario = authenticatedUserProvider != null
                ? authenticatedUserProvider.resolverUsuario(auth).orElse(null)
                : null;
        if (usuario == null) {
            Authentication effectiveAuth = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
            if (effectiveAuth != null && effectiveAuth.isAuthenticated() && !"anonymousUser".equals(effectiveAuth.getPrincipal())) {
                usuario = usuarioRepository.findByCorreo(effectiveAuth.getName()).orElse(null);
            }
        }
        return usuario;
    }

    private Usuario obtenerUsuarioYValidarRol(Authentication auth) {
        Authentication effectiveAuth = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
        if (effectiveAuth == null || !effectiveAuth.isAuthenticated() || "anonymousUser".equals(effectiveAuth.getPrincipal())) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        Usuario usuario = resolverUsuarioEnSesion(effectiveAuth);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("Usuario no encontrado con correo: " + effectiveAuth.getName());
        }

        NombreRol rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        if (rolNombre != NombreRol.TAQUILLERO && rolNombre != NombreRol.ADMINISTRADOR) {
            throw new AccessDeniedException("El usuario no tiene permisos de cajero (requiere rol TAQUILLERO o ADMINISTRADOR)");
        }

        return usuario;
    }

    private void validarSinTurnoActivoPrevio(Long usuarioId) {
        Optional<TurnoCaja> turnoActivo = turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoTurnoCaja.ABIERTO);
        if (turnoActivo.isPresent()) {
            throw new TurnoConflictoException("El usuario ya tiene un turno de caja abierto en simultáneo.");
        }
    }

    private TurnoCaja obtenerTurnoAbiertoPorUsuario(Long usuarioId) {
        return turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoTurnoCaja.ABIERTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró ningún turno de caja abierto para el usuario en sesión."));
    }

    private BigDecimal calcularTotalEsperadoEfectivo(TurnoCaja turno) {
        BigDecimal montoBase = turno.getMontoBase() != null ? turno.getMontoBase() : BigDecimal.ZERO;
        BigDecimal ventasSistema = turno.getVentasEfectivoSistema() != null ? turno.getVentasEfectivoSistema() : BigDecimal.ZERO;
        return montoBase.add(ventasSistema);
    }

    private void validarJustificacionDiferencia(BigDecimal diferencia, String observaciones) {
        if (diferencia.compareTo(BigDecimal.ZERO) != 0 && !StringUtils.hasText(observaciones)) {
            throw new ReglaNegocioException(
                    "Existe una diferencia entre el efectivo reportado y el sistema. Ingrese una justificación obligatoria en las observaciones."
            );
        }
    }

    private void validarTurnoParaArqueo(TurnoCaja turno, Usuario usuario) {
        if (!EstadoTurnoCaja.ABIERTO.equals(turno.getEstado())) {
            throw new ReglaNegocioException("No se pueden registrar arqueos en un turno que no se encuentra ABIERTO (estado actual: " + turno.getEstado() + ")");
        }
        if (esUsuarioTaquillero(usuario) && !usuario.getId().equals(turno.getUsuarioId())) {
            throw new AccessDeniedException("No tiene permisos para registrar arqueos en el turno de otro cajero.");
        }
    }

    private boolean esUsuarioTaquillero(Usuario usuario) {
        return usuario != null && usuario.getRol() != null && NombreRol.TAQUILLERO.equals(usuario.getRol().getNombre());
    }

    private String adjuntarObservacion(String actual, String prefijo, String nuevoTexto) {
        String formateado = StringUtils.hasText(prefijo) ? prefijo + ": " + nuevoTexto : nuevoTexto;
        if (StringUtils.hasText(actual)) {
            return actual + " | " + formateado;
        }
        return formateado;
    }

    private List<TurnoCaja> consultarTurnosSegunFiltros(String fecha, boolean soloPropios, Long usuarioId) {
        if (StringUtils.hasText(fecha)) {
            LocalDate localDate;
            try {
                localDate = LocalDate.parse(fecha.trim());
            } catch (DateTimeParseException e) {
                throw new ReglaNegocioException("Formato de fecha inválido: '" + fecha + "'. Use el formato YYYY-MM-DD.");
            }
            LocalDateTime inicio = localDate.atStartOfDay();
            LocalDateTime fin = localDate.atTime(LocalTime.MAX);

            if (soloPropios && usuarioId != null) {
                return turnoCajaRepository.findByUsuarioIdAndFechaAperturaBetweenOrderByFechaAperturaDesc(usuarioId, inicio, fin);
            }
            return turnoCajaRepository.findByFechaAperturaBetweenOrderByFechaAperturaDesc(inicio, fin);
        }

        if (soloPropios && usuarioId != null) {
            return turnoCajaRepository.findByUsuarioIdOrderByFechaAperturaDesc(usuarioId);
        }
        return turnoCajaRepository.findAllByOrderByFechaAperturaDesc();
    }

    private Map<Long, String> resolverNombresCajeros(List<TurnoCaja> turnos) {
        Set<Long> usuarioIds = turnos.stream()
                .map(TurnoCaja::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(
                        Usuario::getId,
                        Usuario::getNombre,
                        (existente, reemplazo) -> existente
                ));
    }
}