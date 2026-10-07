package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.TarifaTiempo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaTiempoRepository extends JpaRepository<TarifaTiempo, Long> {
    List<TarifaTiempo> findByAtraccionId(Long atraccionId);
    List<TarifaTiempo> findByAtraccionIdAndActivoTrue(Long atraccionId);
    boolean existsByAtraccionIdAndDuracionMinutos(Long atraccionId, Integer duracionMinutos);
    Optional<TarifaTiempo> findByAtraccionIdAndDuracionMinutos(Long atraccionId, Integer duracionMinutos);
}
