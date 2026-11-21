package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.tienda.TicketOnlineDTO;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.MetodoPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImplTest - Pruebas unitarias de envío de correo con comprobante y hora de ingreso")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    @DisplayName("enviarBoletasCompraOnline - Incluye resumen de factura y hora estimada de ingreso cuando hay aforo lleno")
    void enviarBoletasCompraOnline_ConTotalesYHoraDiferida_IncluyeFacturaYIngreso() {
        UUID uuid = UUID.randomUUID();
        TicketOnlineDTO ticketDiferido = new TicketOnlineDTO(
                uuid,
                1L,
                "Laberinto Mágico",
                "Sofía Gómez",
                7,
                "TIEMPO",
                "30 min",
                BigDecimal.valueOf(15000),
                EstadoTicket.PAGADO,
                LocalDateTime.of(2026, 11, 20, 15, 0),
                uuid.toString(),
                LocalDateTime.of(2026, 11, 20, 15, 30),
                "15:30 (aprox.)"
        );

        emailService.enviarBoletasCompraOnline(
                "laura@example.com",
                "Laura Gómez",
                "ORD-999-TEST",
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(2850),
                BigDecimal.valueOf(17850),
                MetodoPago.NEQUI,
                List.of(ticketDiferido)
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage enviado = captor.getValue();
        String cuerpo = enviado.getText();
        assertNotNull(cuerpo);
        assertTrue(cuerpo.contains("RESUMEN DE FACTURA / COMPROBANTE"));
        assertTrue(cuerpo.contains("Subtotal: $ 15.000 COP"));
        assertTrue(cuerpo.contains("Impuestos (IVA): $ 2.850 COP"));
        assertTrue(cuerpo.contains("Total Pagado: $ 17.850 COP"));
        assertTrue(cuerpo.contains("Medio de Pago: NEQUI"));
        assertTrue(cuerpo.contains("Ingreso estimado: 15:30 (aprox.)"));
        assertTrue(cuerpo.contains("aforo completo"));
    }
}
