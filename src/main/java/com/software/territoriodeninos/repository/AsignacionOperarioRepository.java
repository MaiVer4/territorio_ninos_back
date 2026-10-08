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
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    Optional<AsignacionOperario> findByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    List<AsignacionOperario> findByEstado(EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    List<AsignacionOperario> findByAtraccionIdAndEstado(Long atraccionId, EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    List<AsignacionOperario> findByUsuarioId(Long usuarioId);
}
