package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.service.OperacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operacion")
@RequiredArgsConstructor
public class OperacionController implements OperacionApi {

    private final OperacionService operacionService;

    @Override
    @PostMapping("/escanear")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<ValidacionTicketResponseDTO> escanearTicket(
            @Valid @RequestBody EscaneoQrRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        String clientIp = extractClientIp(servletRequest);
        return ResponseEntity.ok(operacionService.escanearTicket(request, authentication, clientIp));
    }

    @Override
    @PostMapping("/iniciar-sesion")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<SesionJuegoResponseDTO> iniciarSesion(
            @Valid @RequestBody InicioSesionRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.iniciarSesion(request, authentication));
    }

    @Override
    @GetMapping("/cola/{atraccionId}")
    @PreAuthorize("hasAnyRole('OPERARIO', 'TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<ColaAtraccionResponseDTO> obtenerCola(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(operacionService.obtenerCola(atraccionId));
    }

    @Override
    @PostMapping("/sesiones/{id}/pausar")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<SesionJuegoResponseDTO> pausarSesion(
            @PathVariable Long id,
            @Valid @RequestBody PausaRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.pausarSesion(id, request, authentication));
    }

    @Override
    @PostMapping("/sesiones/{id}/reanudar")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<SesionJuegoResponseDTO> reanudarSesion(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.reanudarSesion(id, authentication));
    }

    @Override
    @PostMapping("/atraccion/{id}/pausa-general")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<ColaAtraccionResponseDTO> pausaGeneral(
            @PathVariable Long id,
            @Valid @RequestBody PausaGeneralRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.pausaGeneral(id, request, authentication));
    }

    @Override
    @PostMapping("/sesiones/{id}/salida")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<SesionJuegoResponseDTO> registrarSalida(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.registrarSalida(id, authentication));
    }

    @Override
    @PostMapping("/sesiones/{id}/descontar-intento")
    @PreAuthorize("hasAnyRole('OPERARIO', 'ADMINISTRADOR')")
    public ResponseEntity<SesionJuegoResponseDTO> descontarIntento(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RegistroIntentoEventoDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.descontarIntento(id, request, authentication));
    }

    @Override
    @GetMapping("/sincronizar/{atraccionId}")
    @PreAuthorize("hasAnyRole('OPERARIO', 'TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<SincronizacionRelojResponseDTO> sincronizarReloj(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(operacionService.sincronizarReloj(atraccionId));
    }

    @Override
    @GetMapping("/atraccion/{id}/aforo-actual")
    @PreAuthorize("hasAnyRole('OPERARIO', 'TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<AforoActualDTO> obtenerAforoActual(@PathVariable Long id) {
        return ResponseEntity.ok(operacionService.obtenerAforoActual(id));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
