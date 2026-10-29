package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.DetalleVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /**
     * Obtiene todos los detalles asociados a una venta específica.
     */
    @EntityGraph(attributePaths = {"atraccion", "menor", "venta"})
    List<DetalleVenta> findByVentaId(Long ventaId);

    /**
     * Consulta el histórico de detalles de venta registrados en un turno de caja específico,
     * ordenados descendentemente por la fecha de la venta.
     */
    @EntityGraph(attributePaths = {"atraccion", "menor", "venta", "venta.cliente"})
    List<DetalleVenta> findByVentaTurnoCajaIdOrderByVentaFechaVentaDesc(Long turnoCajaId);

    /**
     * Consulta los detalles de venta asociados a una atracción específica.
     */
    @EntityGraph(attributePaths = {"atraccion", "menor", "venta"})
    List<DetalleVenta> findByAtraccionId(Long atraccionId);

    /**
     * Consulta el historial de atracciones visitadas/adquiridas para un menor específico.
     */
    @EntityGraph(attributePaths = {"atraccion", "menor", "venta"})
    List<DetalleVenta> findByMenorIdOrderByVentaFechaVentaDesc(Long menorId);
}
