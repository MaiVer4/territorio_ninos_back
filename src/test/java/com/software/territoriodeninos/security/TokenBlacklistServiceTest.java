package com.software.territoriodeninos.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService - Tests unitarios")
class TokenBlacklistServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Claims claims;

    @InjectMocks
    private TokenBlacklistService blacklistService;

    private final String sampleToken = "jwt.sample.token";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Revocar token y verificar que está revocado")
    void revocar_Y_VerificarEstaRevocado() {
        when(jwtUtil.extraerClaims(sampleToken)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));

        assertFalse(blacklistService.estaRevocado(sampleToken));

        blacklistService.revocar(sampleToken);

        assertTrue(blacklistService.estaRevocado(sampleToken));
    }

    @Test
    @DisplayName("Token no revocado retorna false")
    void estaRevocado_TokenNoRevocado_RetornaFalse() {
        assertFalse(blacklistService.estaRevocado("token-no-en-lista"));
    }

    @Test
    @DisplayName("Token nulo o en blanco retorna false y no falla")
    void estaRevocado_TokenNuloOBlanco_RetornaFalse() {
        assertFalse(blacklistService.estaRevocado(null));
        assertFalse(blacklistService.estaRevocado("   "));
        blacklistService.revocar(null);
        blacklistService.revocar("   ");
    }

    @Test
    @DisplayName("Token expirado se purga y no se considera revocado")
    void estaRevocado_TokenExpirado_SePurga() {
        when(jwtUtil.extraerClaims(sampleToken)).thenReturn(claims);
        // Expirado hace 1 segundo
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 1000));

        blacklistService.revocar(sampleToken);

        assertFalse(blacklistService.estaRevocado(sampleToken));
    }
}
