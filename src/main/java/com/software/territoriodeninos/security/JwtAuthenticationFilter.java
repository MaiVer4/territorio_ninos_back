package com.software.territoriodeninos.security;

import com.software.territoriodeninos.exception.ErrorResponse;
import com.software.territoriodeninos.exception.TokenInvalidoException;
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
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    
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
                
                if (jwtUtil.esValido(token)) {
                    String correo = jwtUtil.extraerCorreo(token);
                    String rol = jwtUtil.extraerRol(token);
                    
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
        String json = String.format("{\"codigo\":%d, \"mensaje\":\"%s\", \"detalles\":\"%s\"}", 
            status.value(), 
            mensaje.replace("\"", "\\\""), 
            detalle.replace("\"", "\\\""));
        response.getWriter().write(json);
    }
}
