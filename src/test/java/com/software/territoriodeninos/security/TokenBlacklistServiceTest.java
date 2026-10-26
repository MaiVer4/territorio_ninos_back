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

    @Test
    @DisplayName("Bloquear y desbloquear usuario invalida o habilita su sesión")
    void bloquearYDesbloquearUsuario() {
        String correo = "usuario@test.com";
        Date issuedAt = new Date();

        assertFalse(blacklistService.esSesionUsuarioRevocada(correo, issuedAt));

        blacklistService.bloquearUsuario(correo);
        assertTrue(blacklistService.esSesionUsuarioRevocada(correo, issuedAt));

        blacklistService.desbloquearUsuario(correo);
        assertFalse(blacklistService.esSesionUsuarioRevocada(correo, issuedAt));
    }

    @Test
    @DisplayName("Revocar tokens emitidos antes de ahora invalida tokens viejos pero no nuevos")
    void revocarTokensEmitidosAntesDeAhora() throws InterruptedException {
        String correo = "cambio@test.com";
        Date tokenViejo = new Date(System.currentTimeMillis() - 5000);

        Thread.sleep(10);
        blacklistService.revocarTokensEmitidosAntesDeAhora(correo);
        Thread.sleep(10);

        Date tokenNuevo = new Date(System.currentTimeMillis() + 5000);

        assertTrue(blacklistService.esSesionUsuarioRevocada(correo, tokenViejo));
        assertFalse(blacklistService.esSesionUsuarioRevocada(correo, tokenNuevo));
    }

    @Test
    @DisplayName("esSesionUsuarioRevocada con correo nulo o en blanco retorna false")
    void esSesionUsuarioRevocada_CorreoInvalido() {
        assertFalse(blacklistService.esSesionUsuarioRevocada(null, new Date()));
        assertFalse(blacklistService.esSesionUsuarioRevocada("   ", new Date()));
    }
}
