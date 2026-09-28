package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.entity.PasswordResetToken;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.CredencialesInvalidasException;
import com.software.territoriodeninos.exception.TokenInvalidoException;
import com.software.territoriodeninos.repository.PasswordResetTokenRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.util.EstadoUsuario;
import com.software.territoriodeninos.util.NombreRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Tests unitarios")
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
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario mockUsuario;
    private Rol mockRol;

    @BeforeEach
    void setUp() {
        mockRol = new Rol();
        mockRol.setId(1L);
        mockRol.setNombre(NombreRol.ADMINISTRADOR);

        mockUsuario = new Usuario();
        mockUsuario.setId(1L);
        mockUsuario.setNombre("Admin Test");
        mockUsuario.setCorreo("admin@test.com");
        mockUsuario.setPassword("hashedPass");
        mockUsuario.setEstado(EstadoUsuario.ACTIVO);
        mockUsuario.setRol(mockRol);

        // Inyectar la propiedad @Value manualmente en tests
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:4200");
    }

    // =========================================================================
    // LOGIN
    // =========================================================================
    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Login exitoso devuelve AuthResponseDTO con token")
        void login_Exitoso() {
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@test.com")
                    .password("Password123")
                    .build();

            when(usuarioRepository.findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));
            when(passwordEncoder.matches("Password123", "hashedPass")).thenReturn(true);
            when(jwtUtil.generarToken(mockUsuario)).thenReturn("jwt-token-mock");

            AuthResponseDTO response = authService.login(request);

            assertNotNull(response);
            assertEquals("jwt-token-mock", response.getToken());
            assertEquals("Bearer", response.getTipoToken());
            assertEquals("admin@test.com", response.getCorreo());
            assertEquals("ADMINISTRADOR", response.getRol());

            verify(usuarioRepository).findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO);
            verify(passwordEncoder).matches("Password123", "hashedPass");
            verify(jwtUtil).generarToken(mockUsuario);
        }

        @Test
        @DisplayName("Login con usuario inexistente lanza CredencialesInvalidasException")
        void login_UsuarioNoExiste() {
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("noexiste@test.com")
                    .password("Password123")
                    .build();

            when(usuarioRepository.findByCorreoAndEstado("noexiste@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.empty());

            CredencialesInvalidasException ex = assertThrows(
                    CredencialesInvalidasException.class,
                    () -> authService.login(request)
            );

            assertEquals("Correo o contraseña incorrectos", ex.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtUtil, never()).generarToken(any(com.software.territoriodeninos.entity.Usuario.class));
        }

        @Test
        @DisplayName("Login con contraseña incorrecta lanza CredencialesInvalidasException")
        void login_ContrasenaIncorrecta() {
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@test.com")
                    .password("WrongPassword")
                    .build();

            when(usuarioRepository.findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));
            when(passwordEncoder.matches("WrongPassword", "hashedPass")).thenReturn(false);

            CredencialesInvalidasException ex = assertThrows(
                    CredencialesInvalidasException.class,
                    () -> authService.login(request)
            );

            assertEquals("Correo o contraseña incorrectos", ex.getMessage());
            verify(jwtUtil, never()).generarToken(any(com.software.territoriodeninos.entity.Usuario.class));
        }
    }

    // =========================================================================
    // RECUPERACIÓN DE CONTRASEÑA
    // =========================================================================
    @Nested
    @DisplayName("Generación de token de recuperación")
    class GenerarTokenRecuperacionTests {

        @Test
        @DisplayName("Token generado y email enviado para usuario existente")
        void generarToken_UsuarioExiste_EnviaEmail() {
            when(usuarioRepository.findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));

            authService.generarTokenRecuperacion("admin@test.com");

            verify(tokenRepository).deleteByUsuarioId(1L);
            verify(tokenRepository).save(any(PasswordResetToken.class));

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviarCorreoRecuperacion(eq("admin@test.com"), urlCaptor.capture());

            String resetUrl = urlCaptor.getValue();
            assertTrue(resetUrl.startsWith("http://localhost:4200/reset-password?token="));
            assertTrue(resetUrl.length() > "http://localhost:4200/reset-password?token=".length());
        }

        @Test
        @DisplayName("No se envía email si usuario no existe (prevención de enumeración)")
        void generarToken_UsuarioNoExiste_NoEnviaEmail() {
            when(usuarioRepository.findByCorreoAndEstado("noexiste@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.empty());

            authService.generarTokenRecuperacion("noexiste@test.com");

            verify(tokenRepository, never()).deleteByUsuarioId(anyLong());
            verify(tokenRepository, never()).save(any(PasswordResetToken.class));
            verify(emailService, never()).enviarCorreoRecuperacion(anyString(), anyString());
        }

        @Test
        @DisplayName("Token anterior se elimina antes de generar uno nuevo")
        void generarToken_EliminaTokenAnterior() {
            when(usuarioRepository.findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));

            authService.generarTokenRecuperacion("admin@test.com");

            // Se verifica el orden: primero delete, luego save
            var inOrder = inOrder(tokenRepository);
            inOrder.verify(tokenRepository).deleteByUsuarioId(1L);
            inOrder.verify(tokenRepository).save(any(PasswordResetToken.class));
        }
    }

    // =========================================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================================
    @Nested
    @DisplayName("Cambiar contraseña con token")
    class CambiarContrasenaTests {

        @Test
        @DisplayName("Contraseña cambiada exitosamente con token válido")
        void cambiarContrasena_TokenValido() {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("valid-token");
            resetToken.setUsuario(mockUsuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));

            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPass");

            authService.cambiarContrasena("valid-token", "NewPassword123");

            assertEquals("newHashedPass", mockUsuario.getPassword());
            verify(tokenRepository).delete(resetToken);
        }

        @Test
        @DisplayName("Token inexistente lanza TokenInvalidoException")
        void cambiarContrasena_TokenNoExiste() {
            when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            TokenInvalidoException ex = assertThrows(
                    TokenInvalidoException.class,
                    () -> authService.cambiarContrasena("invalid-token", "NewPassword123")
            );

            assertEquals("Token inválido o no existe", ex.getMessage());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Token expirado lanza TokenInvalidoException y elimina el token")
        void cambiarContrasena_TokenExpirado() {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("expired-token");
            resetToken.setUsuario(mockUsuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));

            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

            TokenInvalidoException ex = assertThrows(
                    TokenInvalidoException.class,
                    () -> authService.cambiarContrasena("expired-token", "NewPassword123")
            );

            assertEquals("El token ha expirado. Solicite uno nuevo.", ex.getMessage());
            verify(tokenRepository).delete(resetToken);
            verify(usuarioRepository, never()).save(any());
        }
    }
}
