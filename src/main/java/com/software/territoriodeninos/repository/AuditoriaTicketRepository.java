package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.AuditoriaTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaTicketRepository extends JpaRepository<AuditoriaTicket, Long> {

    /**
     * Consulta el historial inmutable de transiciones ordenado cronológicamente por ticket (HU-38.02).
     */
    List<AuditoriaTicket> findByTicketUuidOrderByFechaEventoAsc(UUID ticketUuid);

    /**
     * Consulta eventos de auditoría para una colección de boletas en lote.
     */
    List<AuditoriaTicket> findByTicketUuidInOrderByFechaEventoAsc(Collection<UUID> ticketUuids);

    /**
     * Consulta eventos en un rango de fechas ordenados descendentemente.
     */
    List<AuditoriaTicket> findByFechaEventoBetweenOrderByFechaEventoDesc(LocalDateTime inicio, LocalDateTime fin);
}
