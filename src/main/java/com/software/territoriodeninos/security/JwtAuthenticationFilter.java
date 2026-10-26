package com.software.territoriodeninos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.exception.TokenInvalidoException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return path.equals("/api/auth/login") ||
               path.equals("/api/auth/forgot-password") ||
               path.equals("/api/auth/reset-password") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars") ||
               path.endsWith("/api/auth/login") ||
               path.endsWith("/api/auth/forgot-password") ||
               path.endsWith("/api/auth/reset-password");
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = jwtUtil.extraerTokenDelHeader(authHeader);

                if (tokenBlacklistService.estaRevocado(token)) {
                    throw new TokenInvalidoException("La sesión fue cerrada. Inicie sesión nuevamente.");
                }
                
                Claims claims = jwtUtil.extraerClaims(token);
                if (claims != null && claims.getExpiration() != null && !claims.getExpiration().before(new Date())) {
                    String correo = claims.getSubject();
                    if (tokenBlacklistService.esSesionUsuarioRevocada(correo, claims.getIssuedAt())) {
                        throw new TokenInvalidoException("La sesión ha sido invalidada. Inicie sesión nuevamente.");
                    }
                    String rol = (String) claims.get("rol");
                    
                    List<SimpleGrantedAuthority> authorities = (rol != null && !rol.isBlank())
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                            : List.of();
                    
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(correo, null, authorities);
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        } catch (TokenInvalidoException e) {
            logger.warn("Token JWT inválido: " + e.getMessage());
            escribirRespuestaError(response, HttpStatus.UNAUTHORIZED, "Token inválido o expirado", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado en filtro JWT", e);
            escribirRespuestaError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error al procesar el token");
        }
    }

    private void escribirRespuestaError(HttpServletResponse response, HttpStatus status, String mensaje, String detalle) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> body = Map.of(
                "codigo", status.value(),
                "mensaje", mensaje,
                "detalles", detalle
        );
        
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
