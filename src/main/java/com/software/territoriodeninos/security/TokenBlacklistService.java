package com.software.territoriodeninos.security;

import io.jsonwebtoken.Claims;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista negra en memoria de JWT revocados (logout).
 * Almacena el hash SHA-256 del token hasta su expiración natural.
 */
@Service
public class TokenBlacklistService {

    private final JwtUtil jwtUtil;
    private final Map<String, Instant> revocados = new ConcurrentHashMap<>();

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
            revocados.put(hash(token), expira);
        } catch (Exception e) {
            // Si el token ya es inválido o no parseable, no requiere revocación
        }
    }

    public boolean estaRevocado(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String tokenHash = hash(token);
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

    @Scheduled(fixedRate = 900_000)
    public void purgarExpirados() {
        Instant ahora = Instant.now();
        revocados.entrySet().removeIf(e -> ahora.isAfter(e.getValue()));
    }

    private String hash(String token) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
