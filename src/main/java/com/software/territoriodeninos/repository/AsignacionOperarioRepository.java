package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.AsignacionOperario;
import com.software.territoriodeninos.util.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AsignacionOperarioRepository extends JpaRepository<AsignacionOperario, Long> {
    
    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByAtraccionIdAndEstado(Long atraccionId, EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    Optional<AsignacionOperario> findByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    List<AsignacionOperario> findByEstado(EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    List<AsignacionOperario> findByAtraccionIdAndEstado(Long atraccionId, EstadoAsignacion estado);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "usuario.rol", "atraccion"})
    List<AsignacionOperario> findByUsuarioId(Long usuarioId);

    @Query("SELECT DISTINCT a.atraccion.id FROM AsignacionOperario a WHERE a.atraccion.id IN :atraccionIds AND a.estado = :estado")
    Set<Long> findAtraccionIdsConAsignacionEnEstado(@Param("atraccionIds") Collection<Long> atraccionIds, @Param("estado") EstadoAsignacion estado);
}
