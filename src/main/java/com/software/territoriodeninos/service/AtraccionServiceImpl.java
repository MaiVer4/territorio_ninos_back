package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AsignacionOperarioRepository;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.DetalleVentaRepository;
import com.software.territoriodeninos.repository.SesionJuegoRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.util.EstadoAsignacion;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AtraccionServiceImpl implements AtraccionService {

    private final AtraccionRepository atraccionRepository;
    private final AsignacionOperarioRepository asignacionRepository;
    private final TarifaTiempoRepository tarifaTiempoRepository;
    private final TarifaEventoRepository tarifaEventoRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final SesionJuegoRepository sesionJuegoRepository;

    @Setter
    @Value("${app.catalogo.cache-ttl-ms:5000}")
    private long cacheTtlMs = 5000L;

    private record CachedCatalogo(long expiresAt, List<CatalogoAtraccionVentaDTO> data) {}
    private final AtomicReference<CachedCatalogo> catalogoCache = new AtomicReference<>(null);

    @Override
    public AtraccionResponseDTO crear(AtraccionRequestDTO request) {
        if (atraccionRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictoException("Ya existe una atracción con el nombre: " + request.nombre());
        }

        Atraccion.AtraccionBuilder builder = Atraccion.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tarifaMinuto(request.tarifaMinuto());

        aplicarAtributosOpcionales(builder, request);

        Atraccion guardada = atraccionRepository.save(builder.build());
        invalidarCacheCatalogo();
        log.info("Atracción creada: {}", guardada.getNombre());
        return mapToDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtraccionResponseDTO> obtenerTodas() {
        List<Atraccion> atracciones = atraccionRepository.findByEstadoNotOrderByNombreAsc(EstadoAtraccion.INACTIVO);
        Map<Long, Integer> ocupacionPorAtraccion = obtenerOcupacionActivaPorAtraccion(atracciones);
        return atracciones.stream()
                .map(a -> mapToDTO(a, ocupacionPorAtraccion.getOrDefault(a.getId(), 0)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtraccionResponseDTO> obtenerActivas() {
        List<Atraccion> atracciones = atraccionRepository.findByEstado(EstadoAtraccion.ACTIVO);
        Map<Long, Integer> ocupacionPorAtraccion = obtenerOcupacionActivaPorAtraccion(atracciones);
        return atracciones.stream()
                .map(a -> mapToDTO(a, ocupacionPorAtraccion.getOrDefault(a.getId(), 0)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AtraccionResponseDTO obtenerPorId(Long id) {
        Atraccion atraccion = findAtraccionById(id);
        int ocupacion = obtenerOcupacionActivaPorAtraccion(List.of(atraccion)).getOrDefault(atraccion.getId(), 0);
        return mapToDTO(atraccion, ocupacion);
    }

    @Override
    public AtraccionResponseDTO actualizar(Long id, AtraccionRequestDTO request) {
        Atraccion atraccion = findAtraccionById(id);

        if (!atraccion.getNombre().equalsIgnoreCase(request.nombre()) &&
                atraccionRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictoException("Ya existe otra atracción con el nombre: " + request.nombre());
        }

        atraccion.setNombre(request.nombre());
        atraccion.setDescripcion(request.descripcion());
        atraccion.setTarifaMinuto(request.tarifaMinuto());

        aplicarAtributosOpcionales(atraccion, request);

        invalidarCacheCatalogo();
        log.info("Atracción actualizada: {}", atraccion.getNombre());
        return mapToDTO(atraccion);
    }

    @Override
    public AtraccionResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        Atraccion atraccion = findAtraccionById(id);

        try {
            EstadoAtraccion estado = EstadoAtraccion.fromString(nuevoEstado);
            if (estado != EstadoAtraccion.ACTIVO) {
                verificarSinOperariosActivos(atraccion);
            }
            atraccion.setEstado(estado);
            invalidarCacheCatalogo();
            log.info("Estado de atracción {} cambiado a {}", atraccion.getNombre(), estado);
        } catch (IllegalArgumentException e) {
            throw new ConflictoException("Estado de atracción inválido: " + nuevoEstado);
        }

        return mapToDTO(atraccion);
    }

    @Override
    public void eliminar(Long id) {
        Atraccion atraccion = findAtraccionById(id);
        verificarSinOperariosActivos(atraccion);

        atraccion.setEstado(EstadoAtraccion.INACTIVO);
        invalidarCacheCatalogo();
        log.info("Atracción {} marcada como inactiva", atraccion.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoAtraccionVentaDTO> obtenerCatalogoVenta() {
        long now = System.currentTimeMillis();
        CachedCatalogo cached = catalogoCache.get();
        if (cached != null && now < cached.expiresAt()) {
            log.debug("Retornando catálogo de ventas desde caché en memoria");
            return cached.data();
        }

        // 1. Atracciones activas ordenadas por nombre
        List<Atraccion> atraccionesActivas = atraccionRepository.findByEstadoOrderByNombreAsc(EstadoAtraccion.ACTIVO);
        if (atraccionesActivas.isEmpty()) {
            List<CatalogoAtraccionVentaDTO> vacio = Collections.emptyList();
            catalogoCache.set(new CachedCatalogo(now + cacheTtlMs, vacio));
            return vacio;
        }

        List<Long> atraccionIds = atraccionesActivas.stream().map(Atraccion::getId).toList();

        // 2. Batch query: Tarifas de tiempo activas ordenadas por duracionMinutos ASC
        List<TarifaTiempo> tarifasTiempo = tarifaTiempoRepository
                .findByAtraccionIdInAndActivoTrueOrderByDuracionMinutosAsc(atraccionIds);
        Map<Long, List<TarifaTiempoResponseDTO>> tarifasTiempoPorAtraccion = tarifasTiempo.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAtraccion().getId(),
                        Collectors.mapping(this::mapTarifaTiempoToDTO, Collectors.toList())
                ));

        // 3. Batch query: Tarifas de evento activas ordenadas por intentos ASC
        List<TarifaEvento> tarifasEvento = tarifaEventoRepository
                .findByAtraccionIdInAndActivoTrueOrderByIntentosAsc(atraccionIds);
        Map<Long, List<TarifaEventoResponseDTO>> tarifasEventoPorAtraccion = tarifasEvento.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAtraccion().getId(),
                        Collectors.mapping(this::mapTarifaEventoToDTO, Collectors.toList())
                ));

        // 4. Batch query: Atracciones con operario activo asignado
        Set<Long> atraccionesConOperario = asignacionRepository
                .findAtraccionIdsConAsignacionEnEstado(atraccionIds, EstadoAsignacion.ACTIVA);

        // 5. Batch query: Ocupación actual calculada en base a boletas activas (últimos 15 minutos)
        Map<Long, Integer> ocupacionPorAtraccion = new HashMap<>();
        if (detalleVentaRepository != null && !atraccionIds.isEmpty()) {
            java.time.LocalDateTime ventanaBase = java.time.LocalDateTime.now().minusMinutes(15);
            List<Object[]> rows = detalleVentaRepository.countBoletasActivasPorAtraccionIds(atraccionIds, ventanaBase);
            if (rows != null) {
                for (Object[] row : rows) {
                    if (row != null && row.length >= 2 && row[0] instanceof Long atrId && row[1] instanceof Number count) {
                        ocupacionPorAtraccion.put(atrId, count.intValue());
                    }
                }
            }
        }

        // 6. Ensamblar DTOs con cálculo de aforo disponible en tiempo real
        List<CatalogoAtraccionVentaDTO> catalogo = atraccionesActivas.stream().map(atraccion -> {
            Long id = atraccion.getId();
            List<TarifaTiempoResponseDTO> tt = tarifasTiempoPorAtraccion.getOrDefault(id, Collections.emptyList());
            List<TarifaEventoResponseDTO> te = tarifasEventoPorAtraccion.getOrDefault(id, Collections.emptyList());
            boolean tieneOperario = atraccionesConOperario.contains(id);

            int capacidad = atraccion.getCapacidadMaxima() != null ? atraccion.getCapacidadMaxima() : 1;
            int ocupacionActual = ocupacionPorAtraccion.getOrDefault(id, 0);
            int aforoDisponible = Math.max(0, capacidad - ocupacionActual);

            return new CatalogoAtraccionVentaDTO(
                    id,
                    atraccion.getNombre(),
                    atraccion.getDescripcion(),
                    atraccion.getTipo() != null ? atraccion.getTipo().name() : "TEMPORIZADA",
                    atraccion.getEstado() != null ? atraccion.getEstado().name() : "ACTIVO",
                    atraccion.getTarifaMinuto(),
                    atraccion.getDuracionMinutosBase(),
                    atraccion.getToleranciaMinutos(),
                    atraccion.getCaidasLimite(),
                    capacidad,
                    ocupacionActual,
                    aforoDisponible,
                    tieneOperario,
                    tt,
                    te
            );
        }).toList();

        catalogoCache.set(new CachedCatalogo(now + cacheTtlMs, catalogo));
        return catalogo;
    }

    @Override
    public void invalidarCacheCatalogo() {
        catalogoCache.set(null);
        log.debug("Caché de catálogo de ventas invalidada");
    }

    // --- Métodos Privados Auxiliares ---

    private void verificarSinOperariosActivos(Atraccion atraccion) {
        if (asignacionRepository.existsByAtraccionIdAndEstado(atraccion.getId(), EstadoAsignacion.ACTIVA)) {
            throw new ConflictoException("La atracción '" + atraccion.getNombre()
                    + "' tiene operarios asignados. Reasígnelos o finalice sus asignaciones primero.");
        }
    }

    private Atraccion findAtraccionById(Long id) {
        return atraccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + id));
    }

    private void aplicarAtributosOpcionales(Atraccion.AtraccionBuilder builder, AtraccionRequestDTO request) {
        if (request.tipo() != null) builder.tipo(request.tipo());
        if (request.capacidadMaxima() != null) builder.capacidadMaxima(request.capacidadMaxima());
        if (request.duracionMinutosBase() != null) builder.duracionMinutosBase(request.duracionMinutosBase());
        if (request.toleranciaMinutos() != null) builder.toleranciaMinutos(request.toleranciaMinutos());
        if (request.caidasLimite() != null) builder.caidasLimite(request.caidasLimite());
        if (request.edadMinima() != null) builder.edadMinima(request.edadMinima());
        if (request.edadMaxima() != null) builder.edadMaxima(request.edadMaxima());
    }

    private void aplicarAtributosOpcionales(Atraccion atraccion, AtraccionRequestDTO request) {
        if (request.tipo() != null) atraccion.setTipo(request.tipo());
        if (request.capacidadMaxima() != null) atraccion.setCapacidadMaxima(request.capacidadMaxima());
        if (request.duracionMinutosBase() != null) atraccion.setDuracionMinutosBase(request.duracionMinutosBase());
        if (request.toleranciaMinutos() != null) atraccion.setToleranciaMinutos(request.toleranciaMinutos());
        if (request.caidasLimite() != null) atraccion.setCaidasLimite(request.caidasLimite());
        atraccion.setEdadMinima(request.edadMinima());
        atraccion.setEdadMaxima(request.edadMaxima());
    }

    private Map<Long, Integer> obtenerOcupacionActivaPorAtraccion(List<Atraccion> atracciones) {
        if (sesionJuegoRepository == null || atracciones == null || atracciones.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = atracciones.stream()
                .map(Atraccion::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = sesionJuegoRepository.countActivasPorAtraccionIds(
                ids,
                List.of(EstadoSesionJuego.EN_CURSO, EstadoSesionJuego.PAUSADA)
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] instanceof Long atrId && row[1] instanceof Number count) {
                result.put(atrId, count.intValue());
            }
        }
        return result;
    }

    private AtraccionResponseDTO mapToDTO(Atraccion atraccion) {
        return mapToDTO(atraccion, 0);
    }

    private AtraccionResponseDTO mapToDTO(Atraccion atraccion, Integer ocupacionActiva) {
        return new AtraccionResponseDTO(
                atraccion.getId(),
                atraccion.getNombre(),
                atraccion.getDescripcion(),
                atraccion.getTarifaMinuto(),
                atraccion.getEstado() != null ? atraccion.getEstado().name() : "ACTIVO",
                atraccion.getFechaCreacion(),
                atraccion.getTipo() != null ? atraccion.getTipo().name() : "TEMPORIZADA",
                atraccion.getCapacidadMaxima(),
                atraccion.getDuracionMinutosBase(),
                atraccion.getToleranciaMinutos(),
                atraccion.getCaidasLimite(),
                atraccion.getEdadMinima(),
                atraccion.getEdadMaxima(),
                ocupacionActiva != null ? ocupacionActiva : 0
        );
    }

    private TarifaTiempoResponseDTO mapTarifaTiempoToDTO(TarifaTiempo tarifa) {
        return new TarifaTiempoResponseDTO(
                tarifa.getId(),
                tarifa.getAtraccion().getId(),
                tarifa.getAtraccion().getNombre(),
                tarifa.getDuracionMinutos(),
                tarifa.getPrecio(),
                tarifa.getActivo(),
                tarifa.getFechaCreacion()
        );
    }

    private TarifaEventoResponseDTO mapTarifaEventoToDTO(TarifaEvento tarifa) {
        return new TarifaEventoResponseDTO(
                tarifa.getId(),
                tarifa.getAtraccion().getId(),
                tarifa.getAtraccion().getNombre(),
                tarifa.getIntentos(),
                tarifa.getPrecio(),
                tarifa.getActivo(),
                tarifa.getFechaCreacion()
        );
    }
}
