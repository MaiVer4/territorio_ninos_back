package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Override
    @EntityGraph(attributePaths = {"menores"})
    Optional<Cliente> findById(Long id);

    /**
     * Busca un cliente por su número de documento único.
     *
     * @param numeroDocumento Número de documento del cliente
     * @return El cliente si existe
     */
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    /**
     * Verifica si existe un cliente con el número de documento dado.
     *
     * @param numeroDocumento Número de documento
     * @return true si existe, false en caso contrario
     */
    boolean existsByNumeroDocumento(String numeroDocumento);

    /**
     * Busca un cliente por su número de documento cargando de forma ansiosa su lista de menores.
     *
     * @param numeroDocumento Número de documento
     * @return El cliente con sus menores inicializados
     */
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.menores WHERE c.numeroDocumento = :numeroDocumento")
    Optional<Cliente> findByNumeroDocumentoWithMenores(@Param("numeroDocumento") String numeroDocumento);
}
