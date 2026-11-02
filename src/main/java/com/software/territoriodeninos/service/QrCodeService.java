package com.software.territoriodeninos.service;

import java.util.UUID;

public interface QrCodeService {

    /**
     * Genera una imagen QR en formato PNG (200x200 px, nivel de corrección 'M')
     * directamente en memoria RAM a partir de un contenido de texto.
     *
     * @param contenido Texto a codificar en el QR.
     * @return Arreglo de bytes de la imagen PNG generada.
     */
    byte[] generarQrPng(String contenido);

    /**
     * Genera una imagen QR codificada en Base64 (200x200 px, nivel de corrección 'M')
     * directamente en memoria RAM a partir de un contenido de texto.
     *
     * @param contenido Texto a codificar en el QR.
     * @return Cadena Base64 de la imagen PNG.
     */
    String generarQrBase64(String contenido);

    /**
     * Verifica la existencia del ticket por su UUID y genera su imagen QR en formato PNG (200x200 px, nivel 'M') en RAM.
     *
     * @param uuid Identificador único del ticket.
     * @return Arreglo de bytes de la imagen PNG.
     */
    byte[] generarQrTicketPng(UUID uuid);

    /**
     * Verifica la existencia del ticket por su UUID y genera su imagen QR en Base64 (200x200 px, nivel 'M') en RAM.
     *
     * @param uuid Identificador único del ticket.
     * @return Cadena Base64 de la imagen PNG.
     */
    String generarQrTicketBase64(UUID uuid);
}
