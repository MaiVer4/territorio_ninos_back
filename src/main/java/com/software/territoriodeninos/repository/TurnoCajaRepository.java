package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

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
}
