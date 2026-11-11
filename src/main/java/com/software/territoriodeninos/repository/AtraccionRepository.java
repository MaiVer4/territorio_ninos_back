package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.util.EstadoAtraccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtraccionRepository extends JpaRepository<Atraccion, Long> {
    List<Atraccion> findByEstado(EstadoAtraccion estado);
    List<Atraccion> findByEstadoOrderByNombreAsc(EstadoAtraccion estado);
    Optional<Atraccion> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Atraccion a WHERE a.id = :id")
    Optional<Atraccion> findByIdForUpdate(@Param("id") Long id);
}
