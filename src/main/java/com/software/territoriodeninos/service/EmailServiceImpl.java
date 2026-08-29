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
}
