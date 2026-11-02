package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TicketConsultaDTO;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    /**
     * Busca tickets de forma indexada (< 500ms) por código UUID exacto
     * o por número de documento (cédula) del acudiente comprador.
     *
     * @param criterio UUID del ticket o número de documento del acudiente.
     * @return Lista de tickets encontrados con su estado actual, minutos contratados, menor y atracción.
     */
    List<TicketConsultaDTO> buscarTickets(String criterio);

    /**
     * Obtiene el detalle de consulta de un ticket específico por su UUID.
     *
     * @param uuid Identificador único del ticket.
     * @return Datos detallados de consulta del ticket.
     */
    TicketConsultaDTO obtenerTicketPorUuid(UUID uuid);
}
