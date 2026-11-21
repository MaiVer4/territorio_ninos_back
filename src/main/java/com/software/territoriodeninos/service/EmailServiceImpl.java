package com.software.territoriodeninos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de envío de correos electrónicos.
 * Clase de alta cohesión: su única responsabilidad es construir y enviar emails.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCorreoRecuperacion(String destinatario, String enlaceRecuperacion) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("Recuperación de Contraseña - Territorio de Niños");
            mensaje.setText("Hola,\n\nHas solicitado restablecer tu contraseña. " +
                    "Haz clic en el siguiente enlace para continuar (válido por 15 minutos):\n\n" +
                    enlaceRecuperacion + "\n\nSi no fuiste tú, ignora este mensaje.");

            mailSender.send(mensaje);
            log.info("Correo de recuperación enviado exitosamente a: {}", destinatario);
        } catch (MailException e) {
            log.error("Error al enviar correo de recuperación a {}: {}", destinatario, e.getMessage());
            throw e;
        }
    }

    @Override
    public void enviarBoletasCompraOnline(String destinatario, String nombreCliente, String codigoOrden, java.util.List<com.software.territoriodeninos.dto.tienda.TicketOnlineDTO> tickets) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("¡Tus Boletos para Territorio de Niños! - Orden #" + codigoOrden);

            StringBuilder sb = new StringBuilder();
            sb.append("¡Hola, ").append(nombreCliente).append("!\n\n");
            sb.append("Tu compra ha sido confirmada con éxito. Aquí tienes el resumen de tus boletos para ingresar al parque:\n\n");
            sb.append("Código de Orden: ").append(codigoOrden).append("\n\n");
            sb.append("--- BOLETOS DIGITALES ADQUIRIDOS ---\n");

            for (com.software.territoriodeninos.dto.tienda.TicketOnlineDTO t : tickets) {
                sb.append("• ").append(t.atraccionNombre())
                  .append(" - Para: ").append(t.menorNombre()).append(" (").append(t.menorEdad()).append(" años)\n")
                  .append("  Modalidad: ").append(t.modalidadTexto()).append("\n")
                  .append("  Código de Redención: ").append(t.uuidCodigo()).append("\n\n");
            }

            sb.append("Para redimirlos, simplemente presenta tu celular con los códigos QR abiertos en la pantalla ante el operario de cada atracción.\n");
            sb.append("También puedes consultar tus códigos en la web con tu número de documento.\n\n");
            sb.append("¡Que disfruten un día inolvidable en Territorio de Niños!");

            mensaje.setText(sb.toString());
            mailSender.send(mensaje);
            log.info("Correo de boletos de compra online enviado exitosamente a: {} (Orden #{})", destinatario, codigoOrden);
        } catch (MailException e) {
            log.warn("Aviso: No se pudo enviar el correo de boletos a {} (Orden #{}): {}. La compra se mantiene exitosa.",
                    destinatario, codigoOrden, e.getMessage());
        }
    }
}
