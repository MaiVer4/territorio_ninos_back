package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @EntityGraph(attributePaths = {"detalleVenta", "detalleVenta.atraccion", "detalleVenta.venta", "menor"})
    Optional<Ticket> findByUuidCodigo(UUID uuidCodigo);

    boolean existsByUuidCodigo(UUID uuidCodigo);

    List<Ticket> findByDetalleVentaId(Long detalleVentaId);

    @EntityGraph(attributePaths = {"detalleVenta", "detalleVenta.atraccion", "detalleVenta.venta", "menor"})
    List<Ticket> findByDetalleVentaVentaIdOrderByFechaEmisionAsc(Long ventaId);

    @EntityGraph(attributePaths = {"detalleVenta", "detalleVenta.atraccion", "detalleVenta.venta", "menor"})
    List<Ticket> findByDetalleVentaVentaIdInOrderByFechaEmisionAsc(Collection<Long> ventaIds);
}
