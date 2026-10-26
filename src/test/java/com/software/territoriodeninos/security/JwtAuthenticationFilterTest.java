package com.software.territoriodeninos.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter - Tests unitarios")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private ByteArrayOutputStream responseBuffer;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        responseBuffer = new ByteArrayOutputStream();
        lenient().when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(responseBuffer));
    }

    @Test
    @DisplayName("shouldNotFilter retorna true para endpoints públicos")
    void testShouldNotFilter_PublicEndpoints() {
        when(request.getServletPath()).thenReturn("/api/auth/login");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/v3/api-docs");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/atracciones");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    @DisplayName("doFilterInternal con token válido y sesión activa autentica en SecurityContext")
    void testDoFilterInternal_TokenValido_Autentica() throws Exception {
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extraerTokenDelHeader("Bearer " + token)).thenReturn(token);
        when(tokenBlacklistService.estaRevocado(token)).thenReturn(false);

        when(jwtUtil.extraerClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(claims.getSubject()).thenReturn("usuario@test.com");
        Date issuedAt = new Date();
        when(claims.getIssuedAt()).thenReturn(issuedAt);
        when(claims.get("rol")).thenReturn("ADMINISTRADOR");

        when(tokenBlacklistService.esSesionUsuarioRevocada("usuario@test.com", issuedAt)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("usuario@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("doFilterInternal con sesión revocada escribe error 401")
    void testDoFilterInternal_SesionRevocada_Retorna401() throws Exception {
        String token = "revoked-session-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extraerTokenDelHeader("Bearer " + token)).thenReturn(token);
        when(tokenBlacklistService.estaRevocado(token)).thenReturn(false);

        when(jwtUtil.extraerClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(claims.getSubject()).thenReturn("bloqueado@test.com");
        Date issuedAt = new Date();
        when(claims.getIssuedAt()).thenReturn(issuedAt);

        when(tokenBlacklistService.esSesionUsuarioRevocada("bloqueado@test.com", issuedAt)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
