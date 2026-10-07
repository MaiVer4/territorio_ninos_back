package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.AsignacionOperario;
import com.software.territoriodeninos.util.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionOperarioRepository extends JpaRepository<AsignacionOperario, Long> {
    
    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);
    
    Optional<AsignacionOperario> findByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);
    
    List<AsignacionOperario> findByEstado(EstadoAsignacion estado);
    
    List<AsignacionOperario> findByAtraccionIdAndEstado(Long atraccionId, EstadoAsignacion estado);
    
    List<AsignacionOperario> findByUsuarioId(Long usuarioId);
}
