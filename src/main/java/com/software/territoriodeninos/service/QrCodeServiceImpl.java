package com.software.territoriodeninos.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    public static final int QR_WIDTH = 200;
    public static final int QR_HEIGHT = 200;
    public static final String IMAGE_FORMAT = "PNG";

    private final TicketRepository ticketRepository;

    @Override
    public byte[] generarQrPng(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new ReglaNegocioException("El contenido para generar el código QR no puede ser nulo ni vacío");
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    contenido.trim(),
                    BarcodeFormat.QR_CODE,
                    QR_WIDTH,
                    QR_HEIGHT,
                    hints
            );

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);
                return outputStream.toByteArray();
            }
        } catch (WriterException | IOException ex) {
            log.error("Error al generar imagen QR en memoria RAM para el contenido '{}': {}", contenido, ex.getMessage(), ex);
            throw new ReglaNegocioException("No fue posible generar el código QR en memoria");
        }
    }

    @Override
    public String generarQrBase64(String contenido) {
        byte[] pngBytes = generarQrPng(contenido);
        return Base64.getEncoder().encodeToString(pngBytes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarQrTicketPng(UUID uuid) {
        validarExistenciaTicket(uuid);
        return generarQrPng(uuid.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public String generarQrTicketBase64(UUID uuid) {
        validarExistenciaTicket(uuid);
        return generarQrBase64(uuid.toString());
    }

    private void validarExistenciaTicket(UUID uuid) {
        if (uuid == null) {
            throw new ReglaNegocioException("El UUID del ticket es obligatorio");
        }
        if (!ticketRepository.existsByUuidCodigo(uuid)) {
            throw new RecursoNoEncontradoException("Ticket no encontrado con UUID: " + uuid);
        }
    }
}
