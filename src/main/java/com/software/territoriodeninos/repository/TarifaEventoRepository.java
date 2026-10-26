package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.TarifaEvento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaEventoRepository extends JpaRepository<TarifaEvento, Long> {
    
    @Override
    @EntityGraph(attributePaths = {"atraccion"})
    Optional<TarifaEvento> findById(Long id);

    @EntityGraph(attributePaths = {"atraccion"})
    List<TarifaEvento> findByAtraccionId(Long atraccionId);

    @EntityGraph(attributePaths = {"atraccion"})
    List<TarifaEvento> findByAtraccionIdAndActivoTrue(Long atraccionId);

    boolean existsByAtraccionIdAndIntentos(Long atraccionId, Integer intentos);
    Optional<TarifaEvento> findByAtraccionIdAndIntentos(Long atraccionId, Integer intentos);
}
