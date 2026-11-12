package com.software.territoriodeninos.config;

import com.software.territoriodeninos.exception.TokenInvalidoException;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketConfigTest - Pruebas de seguridad STOMP (Autenticación y Autorización de Tópicos)")
class WebSocketConfigTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketConfig webSocketConfig;
    private ChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(jwtUtil, tokenBlacklistService);
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(captor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        interceptor = captor.getValue();
    }

    @Test
    @DisplayName("CONNECT sin encabezado Authorization debe ser rechazado")
    void testConnect_SinAuthorization_LanzaExcepcion() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        TokenInvalidoException ex = assertThrows(TokenInvalidoException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertTrue(ex.getMessage().contains("Encabezado de autorización ausente o inválido"));
    }

    @Test
    @DisplayName("CONNECT con token revocado debe ser rechazado")
    void testConnect_TokenRevocado_LanzaExcepcion() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-revocado");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.extraerTokenDelHeader("Bearer token-revocado")).thenReturn("token-revocado");
        when(tokenBlacklistService.estaRevocado("token-revocado")).thenReturn(true);

        TokenInvalidoException ex = assertThrows(TokenInvalidoException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertTrue(ex.getMessage().contains("Sesión WebSocket revocada"));
    }

    @Test
    @DisplayName("CONNECT con token válido autentica correctamente al usuario")
    void testConnect_TokenValido_AutenticaUsuario() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer token-valido");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("operario@parque.com");
        when(claims.getIssuedAt()).thenReturn(new Date());
        when(claims.get("rol")).thenReturn("OPERARIO");

        when(jwtUtil.extraerTokenDelHeader("Bearer token-valido")).thenReturn("token-valido");
        when(tokenBlacklistService.estaRevocado("token-valido")).thenReturn(false);
        when(jwtUtil.extraerClaims("token-valido")).thenReturn(claims);
        when(tokenBlacklistService.esSesionUsuarioRevocada(eq("operario@parque.com"), any())).thenReturn(false);

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("operario@parque.com", resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("SUBSCRIBE a /topic/admin/** por rol no administrador debe ser denegado")
    void testSubscribe_CanalAdminSinRolAdmin_DeniegaAcceso() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/admin/alertas");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "taquillero@parque.com", null, List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        ));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        TokenInvalidoException ex = assertThrows(TokenInvalidoException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertTrue(ex.getMessage().contains("requiere rol ADMINISTRADOR"));
    }

    @Test
    @DisplayName("SUBSCRIBE a /topic/atraccion/** por rol no autorizado debe ser denegado")
    void testSubscribe_CanalAtraccionSinRolAdecuado_DeniegaAcceso() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/atraccion/5");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "taquillero@parque.com", null, List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        ));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        TokenInvalidoException ex = assertThrows(TokenInvalidoException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertTrue(ex.getMessage().contains("requiere rol OPERARIO o ADMINISTRADOR"));
    }

    @Test
    @DisplayName("SUBSCRIBE a /topic/admin/** por rol ADMINISTRADOR es permitido")
    void testSubscribe_CanalAdminConRolAdmin_Permitido() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/admin/alertas");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "admin@parque.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        ));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }
}
