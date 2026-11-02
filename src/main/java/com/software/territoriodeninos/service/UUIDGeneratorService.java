package com.software.territoriodeninos.service;

import java.util.List;
import java.util.UUID;

public interface UUIDGeneratorService {

    /**
     * Genera un identificador único criptoseguro (RFC 4122 UUID v4)
     * comprobando la no colisión en TicketRepository antes de su asignación.
     *
     * @return UUID criptoseguro único garantizado.
     */
    UUID generarUuidUnico();

    /**
     * Genera una lista de identificadores criptoseguros garantizando no colisión
     * tanto en memoria entre el lote como en TicketRepository en base de datos.
     *
     * @param cantidad Número de UUIDs a generar (debe ser > 0).
     * @return Lista con la cantidad solicitada de UUIDs únicos.
     */
    List<UUID> generarUuidsUnicos(int cantidad);
}
