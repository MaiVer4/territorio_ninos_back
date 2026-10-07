package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.util.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByCorreo(String correo);
    
    Optional<Usuario> findByCorreoAndEstado(String correo, EstadoUsuario estado);
    
    List<Usuario> findByEstado(EstadoUsuario estado);
    
    List<Usuario> findByRolNombre(com.software.territoriodeninos.util.NombreRol nombreRol);

    List<Usuario> findByRolNombreAndEstado(com.software.territoriodeninos.util.NombreRol nombreRol, EstadoUsuario estado);
    
    boolean existsByCorreo(String correo);
}
