package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.TarifaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaEventoRepository extends JpaRepository<TarifaEvento, Long> {
    List<TarifaEvento> findByAtraccionId(Long atraccionId);
    List<TarifaEvento> findByAtraccionIdAndActivoTrue(Long atraccionId);
    boolean existsByAtraccionIdAndIntentos(Long atraccionId, Integer intentos);
    Optional<TarifaEvento> findByAtraccionIdAndIntentos(Long atraccionId, Integer intentos);
}
