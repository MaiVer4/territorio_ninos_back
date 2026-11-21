package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.tienda.TicketOnlineDTO;
import com.software.territoriodeninos.util.IngresoTextoFormatter;
import com.software.territoriodeninos.util.MetodoPago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Implementación del servicio de envío de correos electrónicos.
 * Clase de alta cohesión: su única responsabilidad es construir y enviar emails.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final Locale LOCALE_CO = new Locale.Builder().setLanguage("es").setRegion("CO").build();

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
    @Async
    public void enviarBoletasCompraOnline(String destinatario, String nombreCliente, String codigoOrden, List<TicketOnlineDTO> tickets) {
        enviarBoletasCompraOnline(destinatario, nombreCliente, codigoOrden, null, null, null, null, tickets);
    }

    @Override
    @Async
    public void enviarBoletasCompraOnline(
            String destinatario,
            String nombreCliente,
            String codigoOrden,
            BigDecimal subtotal,
            BigDecimal impuestos,
            BigDecimal total,
            MetodoPago metodoPago,
            List<TicketOnlineDTO> tickets
    ) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("¡Tus Boletos para Territorio de Niños! - Orden #" + codigoOrden);

            StringBuilder sb = new StringBuilder();
            sb.append("¡Hola, ").append(nombreCliente).append("!\n\n");
            sb.append("Tu compra ha sido confirmada con éxito. Aquí tienes el resumen de tu factura y tus boletos para ingresar al parque:\n\n");
            sb.append("Código de Orden: ").append(codigoOrden).append("\n\n");

            if (total != null || subtotal != null || impuestos != null || metodoPago != null) {
                sb.append("--- RESUMEN DE FACTURA / COMPROBANTE ---\n");
                if (subtotal != null) {
                    sb.append("Subtotal: ").append(formatearMoneda(subtotal)).append(" COP\n");
                }
                if (impuestos != null) {
                    sb.append("Impuestos (IVA): ").append(formatearMoneda(impuestos)).append(" COP\n");
                }
                if (total != null) {
                    sb.append("Total Pagado: ").append(formatearMoneda(total)).append(" COP\n");
                }
                if (metodoPago != null) {
                    sb.append("Medio de Pago: ").append(metodoPago.name()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("--- BOLETOS DIGITALES ADQUIRIDOS ---\n");
            boolean hayDiferidos = false;

            if (tickets != null) {
                for (TicketOnlineDTO t : tickets) {
                    String ingreso = resolverIngresoTexto(t);
                    if (!IngresoTextoFormatter.AHORA.equalsIgnoreCase(ingreso)) {
                        hayDiferidos = true;
                    }
                    sb.append("• ").append(t.atraccionNombre())
                      .append(" - Para: ").append(t.menorNombre());
                    if (t.menorEdad() != null) {
                        sb.append(" (").append(t.menorEdad()).append(" años)");
                    }
                    sb.append("\n")
                      .append("  Modalidad: ").append(t.modalidadTexto()).append("\n");
                    if (t.precioUnitario() != null) {
                        sb.append("  Valor: ").append(formatearMoneda(t.precioUnitario())).append(" COP\n");
                    }
                    sb.append("  Ingreso estimado: ").append(ingreso).append("\n")
                      .append("  Código de Redención: ").append(t.uuidCodigo()).append("\n\n");
                }
            }

            if (hayDiferidos) {
                sb.append("* Nota de aforo: Alguna(s) atracción(es) se encuentran con aforo completo; por favor preséntate cerca de la hora aproximada de ingreso indicada en tu comprobante y boletos.\n\n");
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

    private String resolverIngresoTexto(TicketOnlineDTO t) {
        if (t == null) {
            return IngresoTextoFormatter.AHORA;
        }
        if (StringUtils.hasText(t.ingresoTexto())) {
            return t.ingresoTexto().trim();
        }
        if (t.horaIngresoEstimada() != null) {
            return IngresoTextoFormatter.texto(t.horaIngresoEstimada(), null);
        }
        return IngresoTextoFormatter.AHORA;
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) {
            return "$ 0";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_CO);
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return "$ " + nf.format(valor);
    }
}
