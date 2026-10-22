package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Menor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenorRepository extends JpaRepository<Menor, Long> {

    /**
     * Busca un menor por su número de documento.
     *
     * @param numeroDocumento Número de documento
     * @return El menor si existe
     */
    Optional<Menor> findByNumeroDocumento(String numeroDocumento);

    /**
     * Lista todos los menores asociados a un cliente acudiente.
     *
     * @param clienteId ID del cliente acudiente
     * @return Lista de menores asociados
     */
    List<Menor> findByClienteId(Long clienteId);
}
