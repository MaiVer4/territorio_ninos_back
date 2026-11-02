package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ReimpresionRequestDTO;
import org.springframework.security.core.Authentication;

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

    /**
     * Valida reglas de negocio de turno activo y estado del ticket, registra la auditoría inmutable
     * en log_reimpresion y genera el payload térmico ESC/POS estampado con ** REIMPRESIÓN ** (HU-22.04, HU-22.05).
     *
     * @param uuid Identificador único del ticket.
     * @param request Datos de la solicitud con el motivo obligatorio.
     * @param authentication Contexto de seguridad con el usuario autenticado.
     * @return Cadena con el payload térmico ESC/POS estructurado y estampado.
     */
    String reimprimirTicket(UUID uuid, ReimpresionRequestDTO request, Authentication authentication);
}
