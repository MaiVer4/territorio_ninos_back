package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.LogReimpresion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para almacenamiento inmutable de auditoría de reimpresiones (HU-22.02 / SCRUM-149).
 */
@Repository
public interface LogReimpresionRepository extends JpaRepository<LogReimpresion, Long> {

    @EntityGraph(attributePaths = {"ticket", "usuario"})
    List<LogReimpresion> findByTicketUuidCodigoOrderByFechaReimpresionDesc(UUID ticketUuid);

    long countByTicketUuidCodigo(UUID ticketUuid);
}
