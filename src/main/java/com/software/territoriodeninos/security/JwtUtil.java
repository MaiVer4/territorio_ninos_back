package com.software.territoriodeninos.security;

import com.software.territoriodeninos.exception.TokenInvalidoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration.hours}")
    private long expirationHours;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    private long getExpirationTime() {
        return expirationHours * 60 * 60 * 1000L;
    }
    
    public String generarToken(com.software.territoriodeninos.entity.Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().getNombre().name());
        claims.put("userId", usuario.getId());
        claims.put("nombre", usuario.getNombre());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + getExpirationTime());
        
        return Jwts.builder()
                .claims(claims)
                .subject(usuario.getCorreo())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    private Claims extraerClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("Token JWT inválido o expirado", e);
        }
    }
    
    public String extraerCorreo(String token) {
        return extraerClaims(token).getSubject();
    }
    
    public String extraerRol(String token) {
        return (String) extraerClaims(token).get("rol");
    }
    
    public boolean esValido(String token) {
        try {
            Claims claims = extraerClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (TokenInvalidoException e) {
            return false;
        }
    }
    
    public String extraerTokenDelHeader(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            return tokenHeader.substring(7);
        }
        return tokenHeader;
    }
}
