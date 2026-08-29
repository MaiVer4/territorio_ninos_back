package com.software.territoriodeninos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl - Tests unitarios")
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    @DisplayName("Correo de recuperación se construye y envía correctamente")
    void enviarCorreoRecuperacion_Exitoso() {
        String destinatario = "user@test.com";
        String enlace = "http://localhost:4200/reset-password?token=abc-123";

        emailService.enviarCorreoRecuperacion(destinatario, enlace);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage mensaje = captor.getValue();
        assertNotNull(mensaje.getTo());
        assertEquals(destinatario, mensaje.getTo()[0]);
        assertEquals("Recuperación de Contraseña - Territorio de Niños", mensaje.getSubject());
        assertNotNull(mensaje.getText());
        assertTrue(mensaje.getText().contains(enlace));
        assertTrue(mensaje.getText().contains("15 minutos"));
    }

    @Test
    @DisplayName("Si el envío falla, la excepción MailException se propaga")
    void enviarCorreoRecuperacion_ErrorDeEnvio_PropagaExcepcion() {
        String destinatario = "user@test.com";
        String enlace = "http://localhost:4200/reset-password?token=abc-123";

        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class,
                () -> emailService.enviarCorreoRecuperacion(destinatario, enlace));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
