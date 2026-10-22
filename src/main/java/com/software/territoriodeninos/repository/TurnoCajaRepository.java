package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    /**
     * Consulta turnos de caja en un rango de fechas de apertura ordenados descendentemente.
     *
     * @param inicio Fecha/hora inicial del rango
     * @param fin Fecha/hora final del rango
     * @return Lista de turnos en el rango
     */
    List<TurnoCaja> findByFechaAperturaBetweenOrderByFechaAperturaDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Consulta todos los turnos ordenados descendentemente por fecha de apertura.
     *
     * @return Lista de todos los turnos
     */
    List<TurnoCaja> findAllByOrderByFechaAperturaDesc();

    /**
     * Identifica el turno más reciente de un cajero para un estado determinado,
     * ordenado descendentemente por fecha de apertura.
     *
     * @param usuarioId ID del cajero/usuario
     * @param estado Estado del turno (ej. ABIERTO o CERRADO)
     * @return El turno más reciente coincidente o Optional.empty()
     */
    Optional<TurnoCaja> findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(
            Long usuarioId,
            EstadoTurnoCaja estado
    );

    /**
     * Determina si existe algún registro de turno de caja asociado al usuario.
     *
     * @param usuarioId ID del usuario
     * @return true si tiene turnos de caja registrados
     */
    boolean existsByUsuarioId(Long usuarioId);
}
