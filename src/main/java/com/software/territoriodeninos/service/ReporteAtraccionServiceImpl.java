package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ReporteAtraccionEstadoDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionRequestDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.ReporteAtraccion;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.ReporteAtraccionRepository;
import com.software.territoriodeninos.security.AuthenticatedUserProvider;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.SeveridadReporte;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReporteAtraccionServiceImpl implements ReporteAtraccionService {

    private final ReporteAtraccionRepository reporteAtraccionRepository;
    private final AtraccionRepository atraccionRepository;
    private final AtraccionService atraccionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    public ReporteAtraccionResponseDTO crear(ReporteAtraccionRequestDTO request, Authentication auth) {
        Usuario reportador = authenticatedUserProvider.resolverUsuario(auth)
                .orElseThrow(() -> new ReglaNegocioException("Usuario autenticado no válido para crear el reporte"));

        Atraccion atraccion = atraccionRepository.findById(request.atraccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + request.atraccionId()));

        boolean desactivar = Boolean.TRUE.equals(request.desactivarAtraccion());
        if (desactivar && atraccion.getEstado() != EstadoAtraccion.FUERA_DE_SERVICIO) {
            atraccionService.cambiarEstado(atraccion.getId(), EstadoAtraccion.FUERA_DE_SERVICIO.name());
            atraccion.setEstado(EstadoAtraccion.FUERA_DE_SERVICIO);
        }

        ReporteAtraccion reporte = ReporteAtraccion.builder()
                .atraccion(atraccion)
                .usuarioReporta(reportador)
                .tipoReporte(request.tipoReporte())
                .severidad(request.severidad() != null ? request.severidad() : SeveridadReporte.MEDIA)
                .descripcion(request.descripcion().trim())
                .estado(EstadoReporteAtraccion.ABIERTO)
                .atraccionDesactivada(desactivar)
                .fechaReporte(LocalDateTime.now())
                .build();

        ReporteAtraccion guardado = reporteAtraccionRepository.save(reporte);
        log.info("Reporte de atracción #{} creado para '{}' por {} (tipo={}, desactivar={})",
                guardado.getId(), atraccion.getNombre(), reportador.getCorreo(), request.tipoReporte(), desactivar);
        return ReporteAtraccionResponseDTO.fromEntity(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteAtraccionResponseDTO> listar(
            Long atraccionId,
            TipoReporteAtraccion tipoReporte,
            EstadoReporteAtraccion estado
    ) {
        return reporteAtraccionRepository.buscarConFiltros(atraccionId, tipoReporte, estado)
                .stream()
                .map(ReporteAtraccionResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteAtraccionResponseDTO obtenerPorId(Long id) {
        ReporteAtraccion reporte = reporteAtraccionRepository.findWithRelationsById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte de atracción no encontrado con ID: " + id));
        return ReporteAtraccionResponseDTO.fromEntity(reporte);
    }

    @Override
    public ReporteAtraccionResponseDTO actualizarEstado(Long id, ReporteAtraccionEstadoDTO request, Authentication auth) {
        Usuario usuario = authenticatedUserProvider.resolverUsuario(auth)
                .orElseThrow(() -> new ReglaNegocioException("Usuario autenticado no válido para actualizar el reporte"));

        ReporteAtraccion reporte = reporteAtraccionRepository.findWithRelationsById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte de atracción no encontrado con ID: " + id));

        reporte.setEstado(request.estado());
        if (request.observacionesResolucion() != null && !request.observacionesResolucion().isBlank()) {
            reporte.setObservacionesResolucion(request.observacionesResolucion().trim());
        }

        if (request.estado() == EstadoReporteAtraccion.RESUELTO) {
            reporte.setFechaResolucion(LocalDateTime.now());
            reporte.setUsuarioResuelve(usuario);
            if (Boolean.TRUE.equals(request.reactivarAtraccion()) && reporte.getAtraccion() != null) {
                long otrosPendientes = reporteAtraccionRepository.countByAtraccionIdAndEstadoNotAndIdNot(
                        reporte.getAtraccion().getId(),
                        EstadoReporteAtraccion.RESUELTO,
                        reporte.getId()
                );
                if (otrosPendientes > 0) {
                    throw new ConflictoException(
                            "No se puede reactivar la atracción porque aún tiene " + otrosPendientes + " reporte(s) pendiente(s) de resolución."
                    );
                }
                atraccionService.cambiarEstado(reporte.getAtraccion().getId(), EstadoAtraccion.ACTIVO.name());
                reporte.getAtraccion().setEstado(EstadoAtraccion.ACTIVO);
            }
        }

        ReporteAtraccion actualizado = reporteAtraccionRepository.save(reporte);
        log.info("Reporte de atracción #{} actualizado a {} por {}", id, request.estado(), usuario.getCorreo());
        return ReporteAtraccionResponseDTO.fromEntity(actualizado);
    }
}
