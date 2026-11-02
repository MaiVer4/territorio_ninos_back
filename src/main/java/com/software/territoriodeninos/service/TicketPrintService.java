package com.software.territoriodeninos.service;

import java.util.UUID;

public interface TicketPrintService {

    /**
     * Resuelve los datos del ticket y la configuración institucional del parque,
     * y genera el texto estructurado con comandos ESC/POS listo para enviar al spooler térmico (HU-21.03).
     *
     * @param uuid Identificador único criptoseguro del ticket.
     * @return Cadena con el payload térmico ESC/POS estructurado.
     */
    String generarPayloadImpresion(UUID uuid);
}
