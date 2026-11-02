package com.software.territoriodeninos.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrCodeServiceTest - Pruebas unitarias de generación de códigos QR en RAM (HU-20.02 / CA-01, CA-02, CA-03)")
class QrCodeServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private QrCodeServiceImpl qrCodeService;

    private UUID ticketUuid;

    @BeforeEach
    void setUp() {
        ticketUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("generarQrPng - Genera imagen PNG en memoria RAM con matriz exacta de 200x200 píxeles (CA-01, CA-03)")
    void generarQrPng_contenidoValido_generaImagenPng200x200EnRam() throws Exception {
        String contenido = "TICKET-TEST-12345";

        byte[] pngBytes = qrCodeService.generarQrPng(contenido);

        assertNotNull(pngBytes, "Los bytes generados no deben ser nulos");
        assertTrue(pngBytes.length > 0, "El arreglo de bytes debe contener información de la imagen");

        // Validar que los bytes corresponden a una imagen PNG válida en memoria
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertNotNull(imagen, "Debe decodificarse en una imagen válida");
        assertEquals(200, imagen.getWidth(), "El ancho debe ser exactamente 200 píxeles");
        assertEquals(200, imagen.getHeight(), "El alto debe ser exactamente 200 píxeles");
    }

    @Test
    @DisplayName("generarQrPng - Decodifica correctamente el contenido y verifica nivel de corrección 'M' (15% tolerancia) (CA-02)")
    void generarQrPng_decodificacionYErrorCorrectionLevelM() throws Exception {
        String contenido = ticketUuid.toString();

        byte[] pngBytes = qrCodeService.generarQrPng(contenido);

        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(pngBytes));
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(imagen);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        QRCodeReader reader = new QRCodeReader();
        Result resultado = reader.decode(bitmap);

        assertNotNull(resultado);
        assertEquals(contenido, resultado.getText(), "El contenido decodificado debe coincidir exactamente con el UUID");

        Object nivelCorreccion = resultado.getResultMetadata().get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
        assertNotNull(nivelCorreccion, "Los metadatos deben incluir el nivel de corrección");
        assertTrue(nivelCorreccion.toString().contains("M"), "El nivel de corrección de error debe ser 'M' (15% tolerancia)");
    }

    @Test
    @DisplayName("generarQrBase64 - Genera cadena Base64 válida que decodifica en imagen PNG 200x200")
    void generarQrBase64_contenidoValido_retornaBase64ValidoDecodificable() throws Exception {
        String contenido = "https://parque.com/validar/" + ticketUuid;

        String base64 = qrCodeService.generarQrBase64(contenido);

        assertNotNull(base64);
        assertFalse(base64.isBlank());

        byte[] decodificado = Base64.getDecoder().decode(base64);
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(decodificado));
        assertNotNull(imagen);
        assertEquals(200, imagen.getWidth());
        assertEquals(200, imagen.getHeight());
    }

    @Test
    @DisplayName("generarQrPng - Lanza ReglaNegocioException si el contenido es nulo o vacío")
    void generarQrPng_contenidoInvalido_lanzaReglaNegocioException() {
        assertThrows(ReglaNegocioException.class, () -> qrCodeService.generarQrPng(null));
        assertThrows(ReglaNegocioException.class, () -> qrCodeService.generarQrPng(""));
        assertThrows(ReglaNegocioException.class, () -> qrCodeService.generarQrPng("   "));
    }

    @Test
    @DisplayName("generarQrTicketPng - Genera PNG si el ticket existe en TicketRepository")
    void generarQrTicketPng_ticketExistente_generaPng() throws Exception {
        when(ticketRepository.existsByUuidCodigo(ticketUuid)).thenReturn(true);

        byte[] pngBytes = qrCodeService.generarQrTicketPng(ticketUuid);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);
        verify(ticketRepository).existsByUuidCodigo(ticketUuid);

        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertEquals(200, imagen.getWidth());
        assertEquals(200, imagen.getHeight());
    }

    @Test
    @DisplayName("generarQrTicketPng - Lanza RecursoNoEncontradoException si el ticket no existe en base de datos")
    void generarQrTicketPng_ticketInexistente_lanzaRecursoNoEncontradoException() {
        when(ticketRepository.existsByUuidCodigo(ticketUuid)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> qrCodeService.generarQrTicketPng(ticketUuid)
        );

        assertTrue(ex.getMessage().contains(ticketUuid.toString()));
        verify(ticketRepository).existsByUuidCodigo(ticketUuid);
    }

    @Test
    @DisplayName("generarQrTicketPng - Lanza ReglaNegocioException si el UUID es nulo")
    void generarQrTicketPng_uuidNulo_lanzaReglaNegocioException() {
        assertThrows(ReglaNegocioException.class, () -> qrCodeService.generarQrTicketPng(null));
        verifyNoInteractions(ticketRepository);
    }

    @Test
    @DisplayName("generarQrTicketBase64 - Genera Base64 si el ticket existe en TicketRepository")
    void generarQrTicketBase64_ticketExistente_retornaBase64() {
        when(ticketRepository.existsByUuidCodigo(ticketUuid)).thenReturn(true);

        String base64 = qrCodeService.generarQrTicketBase64(ticketUuid);

        assertNotNull(base64);
        assertFalse(base64.isBlank());
        verify(ticketRepository).existsByUuidCodigo(ticketUuid);
    }
}
