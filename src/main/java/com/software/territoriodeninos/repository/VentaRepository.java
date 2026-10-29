package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "turnoCaja", "detalles", "detalles.atraccion", "detalles.menor"})
    Optional<Venta> findById(Long id);

    /**
     * Consulta el histórico de ventas de un turno de caja específico ordenado de la más reciente a la más antigua.
     */
    @EntityGraph(attributePaths = {"cliente", "turnoCaja", "detalles", "detalles.atraccion", "detalles.menor"})
    List<Venta> findByTurnoCajaIdOrderByFechaVentaDesc(Long turnoCajaId);

    /**
     * Consulta el histórico de ventas de un turno de caja filtrado por estado (ej. COMPLETADA).
     */
    @EntityGraph(attributePaths = {"cliente", "turnoCaja", "detalles", "detalles.atraccion", "detalles.menor"})
    List<Venta> findByTurnoCajaIdAndEstadoVentaOrderByFechaVentaDesc(Long turnoCajaId, EstadoVenta estadoVenta);

    /**
     * Consulta el histórico de ventas de un turno de caja por método de pago y estado.
     */
    @EntityGraph(attributePaths = {"cliente", "turnoCaja"})
    List<Venta> findByTurnoCajaIdAndMetodoPagoAndEstadoVentaOrderByFechaVentaDesc(
            Long turnoCajaId,
            MetodoPago metodoPago,
            EstadoVenta estadoVenta
    );

    /**
     * Consulta el histórico de compras realizadas por un cliente.
     */
    @EntityGraph(attributePaths = {"cliente", "turnoCaja", "detalles", "detalles.atraccion", "detalles.menor"})
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);

    /**
     * Consulta ventas dentro de un rango de fechas.
     */
    @EntityGraph(attributePaths = {"cliente", "turnoCaja"})
    List<Venta> findByFechaVentaBetweenOrderByFechaVentaDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Calcula la suma total de ventas para un turno de caja según estado y método de pago.
     */
    @Query("SELECT COALESCE(SUM(v.montoTotal), 0) FROM Venta v " +
           "WHERE v.turnoCaja.id = :turnoCajaId AND v.metodoPago = :metodoPago AND v.estadoVenta = :estadoVenta")
    BigDecimal sumMontoTotalByTurnoCajaAndMetodoPagoAndEstado(
            @Param("turnoCajaId") Long turnoCajaId,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("estadoVenta") EstadoVenta estadoVenta
    );
}
