package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.ReporteAtraccion;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReporteAtraccionRepository extends JpaRepository<ReporteAtraccion, Long> {

    @EntityGraph(attributePaths = {"atraccion", "usuarioReporta", "usuarioResuelve"})
    @Query("""
        SELECT r FROM ReporteAtraccion r
        WHERE (:atraccionId IS NULL OR r.atraccion.id = :atraccionId)
          AND (:tipoReporte IS NULL OR r.tipoReporte = :tipoReporte)
          AND (:estado IS NULL OR r.estado = :estado)
        ORDER BY r.fechaReporte DESC
    """)
    List<ReporteAtraccion> buscarConFiltros(
            @Param("atraccionId") Long atraccionId,
            @Param("tipoReporte") TipoReporteAtraccion tipoReporte,
            @Param("estado") EstadoReporteAtraccion estado
    );

    @EntityGraph(attributePaths = {"atraccion", "usuarioReporta", "usuarioResuelve"})
    Optional<ReporteAtraccion> findWithRelationsById(Long id);

    long countByAtraccionIdAndEstadoNotAndIdNot(Long atraccionId, EstadoReporteAtraccion estado, Long id);
}
