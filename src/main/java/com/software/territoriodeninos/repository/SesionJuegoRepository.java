package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.SesionJuego;
import com.software.territoriodeninos.util.EstadoSesionJuego;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SesionJuegoRepository extends JpaRepository<SesionJuego, Long> {

    @Query("SELECT COUNT(s) FROM SesionJuego s WHERE s.atraccion.id = :atraccionId AND s.estadoSesion IN :estados")
    int countByAtraccionIdAndEstadoSesionIn(
            @Param("atraccionId") Long atraccionId,
            @Param("estados") Collection<EstadoSesionJuego> estados
    );

    @EntityGraph(attributePaths = {
        "ticket",
        "ticket.detalleVenta",
        "ticket.detalleVenta.venta",
        "ticket.detalleVenta.venta.cliente",
        "atraccion",
        "menor",
        "operario"
    })
    @Query("SELECT s FROM SesionJuego s WHERE s.atraccion.id = :atraccionId AND s.estadoSesion IN :estados ORDER BY s.horaFin ASC NULLS LAST, s.horaInicio ASC")
    List<SesionJuego> findByAtraccionIdAndEstadoSesionIn(
            @Param("atraccionId") Long atraccionId,
            @Param("estados") Collection<EstadoSesionJuego> estados
    );

    @EntityGraph(attributePaths = {"ticket", "atraccion", "menor", "operario"})
    Optional<SesionJuego> findByTicketUuidCodigo(UUID uuidCodigo);

    @EntityGraph(attributePaths = {
        "ticket",
        "ticket.detalleVenta",
        "ticket.detalleVenta.venta",
        "ticket.detalleVenta.venta.cliente",
        "atraccion",
        "menor",
        "operario"
    })
    List<SesionJuego> findByEstadoSesion(EstadoSesionJuego estadoSesion);

    /**
     * Métricas agregadas de uso (conteo y duración promedio en segundos) por atracción en un periodo (HU-37.02).
     */
    @Query("""
        SELECT s.atraccion.id, COUNT(s), COALESCE(AVG(s.duracionSegundos), 0.0)
        FROM SesionJuego s
        WHERE s.horaInicio >= :inicio AND s.horaInicio <= :fin
        GROUP BY s.atraccion.id
    """)
    List<Object[]> findMetricasUsoPorAtraccion(
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin
    );

    /**
     * Recupera horas de inicio de sesiones en un periodo para cálculo de afluencia horaria (HU-37.02).
     */
    @Query("""
        SELECT s.atraccion.id, s.horaInicio
        FROM SesionJuego s
        WHERE s.horaInicio >= :inicio AND s.horaInicio <= :fin
    """)
    List<Object[]> findHorasInicioPorPeriodo(
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin
    );
}
