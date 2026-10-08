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
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = jwtUtil.extraerTokenDelHeader(authHeader);
                
                Claims claims = jwtUtil.extraerClaims(token);
                if (claims != null && claims.getExpiration() != null && !claims.getExpiration().before(new Date())) {
                    String correo = claims.getSubject();
                    String rol = (String) claims.get("rol");
                    
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + rol)
                    );
                    
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
