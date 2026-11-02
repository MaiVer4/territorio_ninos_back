package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.service.QrCodeService;
import com.software.territoriodeninos.service.TicketPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

    private final QrCodeService qrCodeService;
    private final TicketPrintService ticketPrintService;

    @Override
    @GetMapping(value = "/{uuid}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<byte[]> obtenerQrTicket(@PathVariable UUID uuid) {
        byte[] qrPng = qrCodeService.generarQrTicketPng(uuid);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ticket-" + uuid + ".png\"")
                .body(qrPng);
    }

    @Override
    @GetMapping(value = "/{uuid}/print-payload", produces = "text/plain;charset=UTF-8")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<String> obtenerPrintPayload(@PathVariable UUID uuid) {
        String payload = ticketPrintService.generarPayloadImpresion(uuid);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(payload);
    }
}
