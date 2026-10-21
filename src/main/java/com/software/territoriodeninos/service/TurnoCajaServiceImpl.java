package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.exception.TurnoConflictoException;
import com.software.territoriodeninos.repository.TurnoCajaRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.NombreRol;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TurnoCajaServiceImpl implements TurnoCajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public TurnoCajaResponseDTO abrirTurno(AperturaCajaDTO dto, Authentication auth) {
        Usuario usuario = obtenerUsuarioYValidarRol(auth);

        Optional<TurnoCaja> turnoActivo = turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), EstadoTurnoCaja.ABIERTO);

        if (turnoActivo.isPresent()) {
            throw new TurnoConflictoException("El usuario ya tiene un turno de caja abierto en simultáneo.");
        }

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

        return turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), EstadoTurnoCaja.ABIERTO)
                .map(turno -> TurnoCajaResponseDTO.fromEntity(turno, usuario.getNombre()));
    }

    @Override
    @Transactional
    public TurnoCajaResponseDTO cerrarTurno(CierreCajaDTO dto, Authentication auth) {
        Usuario usuario = obtenerUsuarioYValidarRol(auth);

        TurnoCaja turno = turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), EstadoTurnoCaja.ABIERTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró ningún turno de caja abierto para el usuario en sesión."));

        BigDecimal montoBase = turno.getMontoBase() != null ? turno.getMontoBase() : BigDecimal.ZERO;
        BigDecimal ventasSistema = turno.getVentasEfectivoSistema() != null ? turno.getVentasEfectivoSistema() : BigDecimal.ZERO;

        // Notion CA-02: totalEsperado = montoBase + ventasEfectivoSistema
        BigDecimal totalEsperado = montoBase.add(ventasSistema);

        // Notion CA-02: diferencia = montoEfectivoReportado - totalEsperado
        BigDecimal montoReportado = dto.montoEfectivoReportado();
        BigDecimal diferencia = montoReportado.subtract(totalEsperado);

        // Notion CA-04: Exigir justificación obligatoria en observaciones si diferencia != 0
        if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
            if (!StringUtils.hasText(dto.observaciones())) {
                String tipoDiferencia = diferencia.compareTo(BigDecimal.ZERO) > 0 ? "sobrante" : "faltante";
                throw new ReglaNegocioException(String.format(
                        "Existe una diferencia de caja (%s de %s). Es obligatorio ingresar una justificación en las observaciones.",
                        tipoDiferencia, diferencia.abs()
                ));
            }
        }

        turno.setFechaCierre(LocalDateTime.now());
        turno.setMontoEfectivoReportado(montoReportado);
        turno.setDiferencia(diferencia);
        turno.setEstado(EstadoTurnoCaja.CERRADO);

        if (StringUtils.hasText(dto.observaciones())) {
            String obsActuales = turno.getObservaciones();
            if (StringUtils.hasText(obsActuales)) {
                turno.setObservaciones(obsActuales + " | Cierre: " + dto.observaciones().trim());
            } else {
                turno.setObservaciones("Cierre: " + dto.observaciones().trim());
            }
        }

        TurnoCaja guardado = turnoCajaRepository.save(turno);
        return TurnoCajaResponseDTO.fromEntity(guardado, usuario.getNombre());
    }

    private Usuario obtenerUsuarioYValidarRol(Authentication auth) {
        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        String correo = auth.getName();
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correo));

        NombreRol rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        if (rolNombre != NombreRol.TAQUILLERO && rolNombre != NombreRol.ADMINISTRADOR) {
            throw new AccessDeniedException("El usuario no tiene permisos de cajero (requiere rol TAQUILLERO o ADMINISTRADOR)");
        }

        return usuario;
    }
}
