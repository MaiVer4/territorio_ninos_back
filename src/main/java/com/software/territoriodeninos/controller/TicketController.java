package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ReimpresionRequestDTO;
import com.software.territoriodeninos.dto.TicketConsultaDTO;
import com.software.territoriodeninos.service.QrCodeService;
import com.software.territoriodeninos.service.TicketPrintService;
import com.software.territoriodeninos.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

    private final QrCodeService qrCodeService;
    private final TicketPrintService ticketPrintService;
    private final TicketService ticketService;

    @Override
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<List<TicketConsultaDTO>> buscarTickets(
            @RequestParam(name = "criterio", required = false) String criterio
    ) {
        return ResponseEntity.ok(ticketService.buscarTickets(criterio));
    }

    @Override
    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<TicketConsultaDTO> obtenerTicketPorUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ticketService.obtenerTicketPorUuid(uuid));
    }

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

    @Override
    @PostMapping(value = "/{uuid}/reimprimir", produces = "text/plain;charset=UTF-8")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<String> reimprimirTicket(
            @PathVariable UUID uuid,
            @Valid @RequestBody ReimpresionRequestDTO request,
            Authentication authentication
    ) {
        String payload = ticketPrintService.reimprimirTicket(uuid, request, authentication);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(payload);
    }
}
