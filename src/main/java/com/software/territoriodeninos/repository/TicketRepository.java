package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @EntityGraph(attributePaths = {
        "detalleVenta",
        "detalleVenta.atraccion",
        "detalleVenta.venta",
        "detalleVenta.venta.cliente",
        "detalleVenta.venta.turnoCaja",
        "menor"
    })
    Optional<Ticket> findByUuidCodigo(UUID uuidCodigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "detalleVenta",
        "detalleVenta.atraccion",
        "detalleVenta.venta",
        "detalleVenta.venta.cliente",
        "detalleVenta.venta.turnoCaja",
        "menor"
    })
    @Query("SELECT t FROM Ticket t WHERE t.uuidCodigo = :uuidCodigo")
    Optional<Ticket> findByUuidCodigoForUpdate(@Param("uuidCodigo") UUID uuidCodigo);

    boolean existsByUuidCodigo(UUID uuidCodigo);

    @EntityGraph(attributePaths = {
        "detalleVenta",
        "detalleVenta.atraccion",
        "detalleVenta.venta",
        "detalleVenta.venta.cliente",
        "detalleVenta.venta.turnoCaja",
        "menor"
    })
    @Query("SELECT t FROM Ticket t WHERE CAST(t.uuidCodigo AS string) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<Ticket> findByUuidCodigoStartingWith(@Param("prefix") String prefix);

    @EntityGraph(attributePaths = {
        "detalleVenta",
        "detalleVenta.atraccion",
        "detalleVenta.venta",
        "detalleVenta.venta.cliente",
        "detalleVenta.venta.turnoCaja",
        "menor"
    })
    @Query("SELECT t FROM Ticket t WHERE t.detalleVenta.id = :detalleVentaId")
    List<Ticket> findByDetalleVentaIdConRelaciones(@Param("detalleVentaId") Long detalleVentaId);

    List<Ticket> findByDetalleVentaId(Long detalleVentaId);

    @EntityGraph(attributePaths = {"detalleVenta", "detalleVenta.atraccion", "detalleVenta.venta", "detalleVenta.venta.cliente", "menor"})
    List<Ticket> findByDetalleVentaVentaIdOrderByFechaEmisionAsc(Long ventaId);

    @EntityGraph(attributePaths = {"detalleVenta", "detalleVenta.atraccion", "detalleVenta.venta", "detalleVenta.venta.cliente", "menor"})
    List<Ticket> findByDetalleVentaVentaIdInOrderByFechaEmisionAsc(Collection<Long> ventaIds);

    @EntityGraph(attributePaths = {
        "detalleVenta",
        "detalleVenta.atraccion",
        "detalleVenta.venta",
        "detalleVenta.venta.cliente",
        "menor"
    })
    List<Ticket> findByDetalleVentaVentaClienteNumeroDocumentoOrderByFechaEmisionDesc(String numeroDocumento);

    @Query("""
        SELECT t FROM Ticket t
        JOIN FETCH t.detalleVenta dv
        JOIN FETCH dv.atraccion a
        JOIN FETCH dv.venta v
        JOIN FETCH v.cliente c
        LEFT JOIN FETCH t.menor m
        WHERE c.numeroDocumento = :documento
        ORDER BY t.fechaEmision DESC
        """)
    List<Ticket> buscarPorDocumentoAcudiente(@Param("documento") String documento);

    @Query("SELECT t.uuidCodigo FROM Ticket t WHERE t.uuidCodigo IN :uuids")
    Set<UUID> findExistingUuids(@Param("uuids") Collection<UUID> uuids);

    @Query("SELECT t FROM Ticket t WHERE t.detalleVenta.atraccion.id = :atraccionId AND t.estadoTicket = :estado AND t.fechaEmision BETWEEN :inicio AND :fin")
    List<Ticket> findByAtraccionIdAndEstadoTicketAndFechaEmisionBetween(
            @Param("atraccionId") Long atraccionId,
            @Param("estado") com.software.territoriodeninos.util.EstadoTicket estado,
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin
    );

    /** Boletas en espera (cola FIFO) de una atracción emitidas desde {@code desde}, ordenadas por emisión. */
    @Query("SELECT t FROM Ticket t JOIN FETCH t.detalleVenta d WHERE d.atraccion.id = :atraccionId AND t.estadoTicket = :estado AND t.fechaEmision >= :desde ORDER BY t.fechaEmision ASC")
    List<Ticket> findEnEsperaPorAtraccion(
            @Param("atraccionId") Long atraccionId,
            @Param("estado") com.software.territoriodeninos.util.EstadoTicket estado,
            @Param("desde") java.time.LocalDateTime desde
    );

    /** Boletas en espera (cola FIFO) para múltiples atracciones emitidas desde {@code desde}. */
    @Query("SELECT t FROM Ticket t JOIN FETCH t.detalleVenta d WHERE d.atraccion.id IN :atraccionIds AND t.estadoTicket = :estado AND t.fechaEmision >= :desde ORDER BY t.fechaEmision ASC")
    List<Ticket> findEnEsperaPorAtracciones(
            @Param("atraccionIds") Collection<Long> atraccionIds,
            @Param("estado") com.software.territoriodeninos.util.EstadoTicket estado,
            @Param("desde") java.time.LocalDateTime desde
    );
}
