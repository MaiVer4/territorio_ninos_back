package com.software.territoriodeninos.config;

import com.software.territoriodeninos.exception.TokenInvalidoException;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-operacion")
                .setAllowedOriginPatterns(allowedOrigin, "http://localhost:*")
                .withSockJS();

        registry.addEndpoint("/ws-operacion")
                .setAllowedOriginPatterns(allowedOrigin, "http://localhost:*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    autenticarConexion(accessor);
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    validarSuscripcion(accessor);
                }

                return message;
            }

            private void autenticarConexion(StompHeaderAccessor accessor) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new TokenInvalidoException("Encabezado de autorización ausente o inválido para conexión WebSocket.");
                }
                UsernamePasswordAuthenticationToken authentication = autenticarToken(authHeader);
                accessor.setUser(authentication);
            }

            private void validarSuscripcion(StompHeaderAccessor accessor) {
                if (accessor.getUser() == null) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        accessor.setUser(autenticarToken(authHeader));
                    } else {
                        throw new TokenInvalidoException("Suscripción rechazada: Sesión WebSocket no autenticada.");
                    }
                }

                String destination = accessor.getDestination();
                if (destination != null) {
                    if (destination.startsWith("/topic/admin")) {
                        boolean esAdmin = tieneRol(accessor.getUser(), "ROLE_ADMINISTRADOR");
                        if (!esAdmin) {
                            throw new TokenInvalidoException("Acceso denegado: suscripción a canal administrativo requiere rol ADMINISTRADOR.");
                        }
                    } else if (destination.startsWith("/topic/atraccion/")) {
                        boolean autorizado = tieneRol(accessor.getUser(), "ROLE_ADMINISTRADOR")
                                || tieneRol(accessor.getUser(), "ROLE_OPERARIO");
                        if (!autorizado) {
                            throw new TokenInvalidoException("Acceso denegado: suscripción a canal de atracción requiere rol OPERARIO o ADMINISTRADOR.");
                        }
                    }
                }
            }

            private UsernamePasswordAuthenticationToken autenticarToken(String authHeader) {
                String token = jwtUtil.extraerTokenDelHeader(authHeader);
                if (tokenBlacklistService.estaRevocado(token)) {
                    throw new TokenInvalidoException("Sesión WebSocket revocada.");
                }
                Claims claims = jwtUtil.extraerClaims(token);
                String correo = claims.getSubject();
                if (tokenBlacklistService.esSesionUsuarioRevocada(correo, claims.getIssuedAt())) {
                    throw new TokenInvalidoException("Sesión de usuario invalidada.");
                }
                String rol = (String) claims.get("rol");
                List<SimpleGrantedAuthority> authorities = (rol != null && !rol.isBlank())
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                        : List.of();
                return new UsernamePasswordAuthenticationToken(correo, null, authorities);
            }

            private boolean tieneRol(java.security.Principal principal, String roleName) {
                if (principal instanceof UsernamePasswordAuthenticationToken auth) {
                    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(roleName));
                }
                return false;
            }
        });
    }
}
