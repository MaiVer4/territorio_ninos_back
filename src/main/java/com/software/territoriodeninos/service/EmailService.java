package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.tienda.TicketOnlineDTO;
import com.software.territoriodeninos.util.MetodoPago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interfaz para el servicio de envío de correos electrónicos.
 * Aplica el Principio de Inversión de Dependencias (DIP):
 * los módulos de alto nivel (AuthService) dependen de esta abstracción,
 * no de la implementación concreta de envío de correos.
 */
public interface EmailService {

    /**
     * Envía un correo electrónico de recuperación de contraseña.
     *
     * @param destinatario dirección de correo del usuario
     * @param enlaceRecuperacion URL completa con el token para restablecer la contraseña
     */
    void enviarCorreoRecuperacion(String destinatario, String enlaceRecuperacion);

    /**
     * Envía un correo con la confirmación de compra y los boletos digitales adquiridos.
     *
     * @param destinatario dirección de correo del acudiente
     * @param nombreCliente nombre del acudiente
     * @param codigoOrden código de orden de compra
     * @param tickets lista de tickets digitales emitidos
     */
    void enviarBoletasCompraOnline(String destinatario, String nombreCliente, String codigoOrden, List<TicketOnlineDTO> tickets);

    /**
     * Envía un correo con el resumen de factura/comprobante de pago y los boletos digitales adquiridos.
     */
    default void enviarBoletasCompraOnline(
            String destinatario,
            String nombreCliente,
            String codigoOrden,
            BigDecimal subtotal,
            BigDecimal impuestos,
            BigDecimal total,
            MetodoPago metodoPago,
            List<TicketOnlineDTO> tickets
    ) {
        enviarBoletasCompraOnline(destinatario, nombreCliente, codigoOrden, tickets);
    }
}
