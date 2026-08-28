package com.software.territoriodeninos.service;

import com.software.territoriodeninos.entity.PasswordResetToken;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.repository.PasswordResetTokenRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.util.EstadoUsuario;
import com.software.territoriodeninos.util.NombreRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario mockUsuario;

    @BeforeEach
    void setUp() {
        Rol rol = new Rol();
        rol.setNombre(NombreRol.ADMINISTRADOR);

        mockUsuario = new Usuario();
        mockUsuario.setId(1L);
        mockUsuario.setCorreo("test@test.com");
        mockUsuario.setPassword("hashedPass");
        mockUsuario.setEstado(EstadoUsuario.ACTIVO);
        mockUsuario.setRol(rol);
    }

    @Test
    void generarTokenRecuperacion_EnvioExitoso() {
        when(usuarioRepository.findByCorreoAndEstado("test@test.com", EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(mockUsuario));

        authService.generarTokenRecuperacion("test@test.com");

        verify(tokenRepository, times(1)).deleteByUsuarioId(1L);
        
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(1)).save(tokenCaptor.capture());
        
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken());
        assertEquals(1L, savedToken.getUsuario().getId());
        assertTrue(savedToken.getFechaExpiracion().isAfter(LocalDateTime.now().plusMinutes(10)));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void generarTokenRecuperacion_UsuarioNoExiste_Silencioso() {
        when(usuarioRepository.findByCorreoAndEstado("inexistente@test.com", EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        authService.generarTokenRecuperacion("inexistente@test.com");

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void cambiarContrasena_Exitoso() {
        String mockTokenStr = "uuid-token";
        PasswordResetToken mockToken = new PasswordResetToken();
        mockToken.setToken(mockTokenStr);
        mockToken.setUsuario(mockUsuario);
        mockToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findByToken(mockTokenStr)).thenReturn(Optional.of(mockToken));
        when(passwordEncoder.encode("Nueva123*")).thenReturn("hashedNueva123*");

        authService.cambiarContrasena(mockTokenStr, "Nueva123*");

        verify(usuarioRepository, times(1)).save(mockUsuario);
        assertEquals("hashedNueva123*", mockUsuario.getPassword());
        verify(tokenRepository, times(1)).delete(mockToken);
    }

    @Test
    void cambiarContrasena_TokenExpirado() {
        String mockTokenStr = "uuid-token-expirado";
        PasswordResetToken mockToken = new PasswordResetToken();
        mockToken.setToken(mockTokenStr);
        mockToken.setUsuario(mockUsuario);
        mockToken.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByToken(mockTokenStr)).thenReturn(Optional.of(mockToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.cambiarContrasena(mockTokenStr, "Nueva123*");
        });

        assertEquals("El token ha expirado. Solicite uno nuevo.", exception.getMessage());
        verify(tokenRepository, times(1)).delete(mockToken);
        verify(usuarioRepository, never()).save(any());
    }
}
