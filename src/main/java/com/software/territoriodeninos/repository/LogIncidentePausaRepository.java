package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.LogIncidentePausa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogIncidentePausaRepository extends JpaRepository<LogIncidentePausa, Long> {

    List<LogIncidentePausa> findBySesionJuegoIdOrderByFechaInicioPausaDesc(Long sesionJuegoId);

    Optional<LogIncidentePausa> findFirstBySesionJuegoIdAndFechaFinPausaIsNullOrderByFechaInicioPausaDesc(Long sesionJuegoId);

    /**
     * Consulta incidentes/pausas con filtros opcionales por atracción, tipo de pausa y rango de fechas (HU-41.01, HU-41.04).
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT l FROM LogIncidentePausa l
        LEFT JOIN FETCH l.atraccion
        LEFT JOIN FETCH l.operario
        LEFT JOIN FETCH l.sesionJuego
        WHERE l.fechaInicioPausa >= :inicio
          AND l.fechaInicioPausa <= :fin
          AND (:atraccionId IS NULL OR l.atraccion.id = :atraccionId OR l.sesionJuego.atraccion.id = :atraccionId)
          AND (:tipoPausa IS NULL OR UPPER(l.tipoPausa) = UPPER(:tipoPausa))
        ORDER BY l.fechaInicioPausa DESC
    """)
    List<LogIncidentePausa> findIncidentesConFiltros(
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
            @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin,
            @org.springframework.data.repository.query.Param("atraccionId") Long atraccionId,
            @org.springframework.data.repository.query.Param("tipoPausa") String tipoPausa
    );

    /**
     * Suma acumulada de tiempo de inactividad agrupada por tipo de pausa (HU-41.03).
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(l.tipoPausa, 'OPERATIVA'), COALESCE(SUM(l.duracionSegundos), 0)
        FROM LogIncidentePausa l
        WHERE l.fechaInicioPausa >= :inicio AND l.fechaInicioPausa <= :fin
          AND (:atraccionId IS NULL OR l.atraccion.id = :atraccionId OR l.sesionJuego.atraccion.id = :atraccionId)
        GROUP BY COALESCE(l.tipoPausa, 'OPERATIVA')
    """)
    List<Object[]> sumInactividadPorTipoPausa(
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
            @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin,
            @org.springframework.data.repository.query.Param("atraccionId") Long atraccionId
    );
}
