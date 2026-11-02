package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.LogEscaneoQr;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LogEscaneoQrRepository extends JpaRepository<LogEscaneoQr, Long> {

    @EntityGraph(attributePaths = {"atraccion", "operario"})
    List<LogEscaneoQr> findByUuidLeidoOrderByFechaEscaneoDesc(UUID uuidLeido);

    @EntityGraph(attributePaths = {"atraccion", "operario"})
    List<LogEscaneoQr> findByOperarioIdOrderByFechaEscaneoDesc(Long operarioId);

    @EntityGraph(attributePaths = {"atraccion", "operario"})
    List<LogEscaneoQr> findByAtraccionIdOrderByFechaEscaneoDesc(Long atraccionId);
}
