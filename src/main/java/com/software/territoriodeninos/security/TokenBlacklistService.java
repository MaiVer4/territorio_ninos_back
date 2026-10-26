package com.software.territoriodeninos.security;

import com.software.territoriodeninos.util.HashUtil;
import io.jsonwebtoken.Claims;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista negra en memoria de JWT revocados (logout) y gestión de revocación de sesiones por usuario.
 * Almacena el hash SHA-256 del token hasta su expiración natural.
 */
@Service
public class TokenBlacklistService {

    private final JwtUtil jwtUtil;
    private final Map<String, Instant> revocados = new ConcurrentHashMap<>();
    private final Set<String> usuariosBloqueados = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> revocadoAntesDePorUsuario = new ConcurrentHashMap<>();

    public TokenBlacklistService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public void revocar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtUtil.extraerClaims(token);
            Instant expira = claims.getExpiration() != null
                    ? claims.getExpiration().toInstant()
                    : Instant.now().plusSeconds(86_400);
            revocados.put(HashUtil.sha256Hex(token), expira);
        } catch (Exception e) {
            // Si el token ya es inválido o no parseable, no requiere revocación
        }
    }

    public boolean estaRevocado(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String tokenHash = HashUtil.sha256Hex(token);
        Instant expira = revocados.get(tokenHash);
        if (expira == null) {
            return false;
        }
        if (Instant.now().isAfter(expira)) {
            revocados.remove(tokenHash);
            return false;
        }
        return true;
    }

    public void bloquearUsuario(String correo) {
        if (correo != null && !correo.isBlank()) {
            usuariosBloqueados.add(correo.trim().toLowerCase());
        }
    }

    public void desbloquearUsuario(String correo) {
        if (correo != null && !correo.isBlank()) {
            usuariosBloqueados.remove(correo.trim().toLowerCase());
        }
    }

    public void revocarTokensEmitidosAntesDeAhora(String correo) {
        if (correo != null && !correo.isBlank()) {
            revocadoAntesDePorUsuario.put(correo.trim().toLowerCase(), Instant.now());
        }
    }

    public boolean esSesionUsuarioRevocada(String correo, Date issuedAt) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        String key = correo.trim().toLowerCase();
        if (usuariosBloqueados.contains(key)) {
            return true;
        }
        Instant corte = revocadoAntesDePorUsuario.get(key);
        if (corte != null) {
            if (issuedAt == null) {
                return true;
            }
            return !issuedAt.toInstant().isAfter(corte);
        }
        return false;
    }

    @Scheduled(fixedRate = 900_000)
    public void purgarExpirados() {
        Instant ahora = Instant.now();
        revocados.entrySet().removeIf(e -> ahora.isAfter(e.getValue()));
        Instant limiteHistorico = ahora.minusSeconds(172_800);
        revocadoAntesDePorUsuario.entrySet().removeIf(e -> e.getValue().isBefore(limiteHistorico));
    }
}
