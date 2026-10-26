package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.entity.PasswordResetToken;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.CredencialesInvalidasException;
import com.software.territoriodeninos.exception.TokenInvalidoException;
import com.software.territoriodeninos.repository.PasswordResetTokenRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
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

    @Mock
    private com.software.territoriodeninos.security.LoginAttemptService loginAttemptService;

    @Mock
    private com.software.territoriodeninos.security.TokenBlacklistService tokenBlacklistService;

    @Mock
    private com.software.territoriodeninos.repository.TurnoCajaRepository turnoCajaRepository;

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

            when(loginAttemptService.isBlocked("admin@test.com")).thenReturn(false);
            when(usuarioRepository.findByCorreo("admin@test.com"))
                    .thenReturn(Optional.of(mockUsuario));
            when(passwordEncoder.matches("Password123", "hashedPass")).thenReturn(true);
            when(jwtUtil.generarToken(mockUsuario)).thenReturn("jwt-token-mock");

            AuthResponseDTO response = authService.login(request);

            assertNotNull(response);
            assertEquals("jwt-token-mock", response.getToken());
            assertEquals("Bearer", response.getTipoToken());
            assertEquals("admin@test.com", response.getCorreo());
            assertEquals("ADMINISTRADOR", response.getRol());

            verify(usuarioRepository).findByCorreo("admin@test.com");
            verify(passwordEncoder).matches("Password123", "hashedPass");
            verify(jwtUtil).generarToken(mockUsuario);
            verify(loginAttemptService).loginSucceeded("admin@test.com");
        }

        @Test
        @DisplayName("Login con usuario inexistente lanza CredencialesInvalidasException")
        void login_UsuarioNoExiste() {
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("noexiste@test.com")
                    .password("Password123")
                    .build();

            when(loginAttemptService.isBlocked("noexiste@test.com")).thenReturn(false);
            when(usuarioRepository.findByCorreo("noexiste@test.com"))
                    .thenReturn(Optional.empty());

            CredencialesInvalidasException ex = assertThrows(
                    CredencialesInvalidasException.class,
                    () -> authService.login(request)
            );

            assertEquals("Correo o contraseña incorrectos", ex.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtUtil, never()).generarToken(any(com.software.territoriodeninos.entity.Usuario.class));
            verify(loginAttemptService).loginFailed("noexiste@test.com");
        }

        @Test
        @DisplayName("Login con contraseña incorrecta lanza CredencialesInvalidasException")
        void login_ContrasenaIncorrecta() {
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@test.com")
                    .password("WrongPassword")
                    .build();

            when(loginAttemptService.isBlocked("admin@test.com")).thenReturn(false);
            when(usuarioRepository.findByCorreo("admin@test.com"))
                    .thenReturn(Optional.of(mockUsuario));
            when(passwordEncoder.matches("WrongPassword", "hashedPass")).thenReturn(false);

            CredencialesInvalidasException ex = assertThrows(
                    CredencialesInvalidasException.class,
                    () -> authService.login(request)
            );

            assertEquals("Correo o contraseña incorrectos", ex.getMessage());
            verify(jwtUtil, never()).generarToken(any(com.software.territoriodeninos.entity.Usuario.class));
            verify(loginAttemptService).loginFailed("admin@test.com");
        }

        @Test
        @DisplayName("Login con cuenta inactiva lanza CuentaInactivaException")
        void login_CuentaInactiva() {
            mockUsuario.setEstado(EstadoUsuario.INACTIVO);
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@test.com")
                    .password("Password123")
                    .build();

            when(loginAttemptService.isBlocked("admin@test.com")).thenReturn(false);
            when(usuarioRepository.findByCorreo("admin@test.com"))
                    .thenReturn(Optional.of(mockUsuario));

            assertThrows(
                    com.software.territoriodeninos.exception.CuentaInactivaException.class,
                    () -> authService.login(request)
            );
        }

        @Test
        @DisplayName("Login con cuenta bloqueada lanza CuentaBloqueadaException")
        void login_CuentaBloqueada() {
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@test.com")
                    .password("Password123")
                    .build();

            when(loginAttemptService.isBlocked("admin@test.com")).thenReturn(true);
            when(loginAttemptService.getRemainingLockoutMinutes("admin@test.com")).thenReturn(15L);

            assertThrows(
                    com.software.territoriodeninos.exception.CuentaBloqueadaException.class,
                    () -> authService.login(request)
            );
        }
    }

    // =========================================================================
    // =========================================================================
    // RECUPERACIÓN DE CONTRASEÑA
    // =========================================================================
    @Nested
    @DisplayName("Generación de token de recuperación")
    class GenerarTokenRecuperacionTests {

        @Test
        @DisplayName("Token generado y email enviado para usuario existente con hash SHA-256")
        void generarToken_UsuarioExiste_EnviaEmail() {
            when(usuarioRepository.findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));

            authService.generarTokenRecuperacion("admin@test.com");

            verify(tokenRepository).deleteByUsuarioId(1L);

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviarCorreoRecuperacion(eq("admin@test.com"), urlCaptor.capture());

            String resetUrl = urlCaptor.getValue();
            assertTrue(resetUrl.startsWith("http://localhost:4200/reset-password?token="));
            String tokenPlano = resetUrl.substring("http://localhost:4200/reset-password?token=".length());
            assertFalse(tokenPlano.isBlank());

            PasswordResetToken savedToken = tokenCaptor.getValue();
            assertNotNull(savedToken);
            assertEquals(64, savedToken.getToken().length());
            assertEquals(sha256Hex(tokenPlano), savedToken.getToken());
            assertEquals(mockUsuario, savedToken.getUsuario());
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

        @Test
        @DisplayName("Fallo al enviar correo captura MailException sin propagar error")
        void generarToken_ErrorEnvioCorreo_NoLanzaExcepcion() {
            when(usuarioRepository.findByCorreoAndEstado("admin@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .when(emailService).enviarCorreoRecuperacion(anyString(), anyString());

            assertDoesNotThrow(() -> authService.generarTokenRecuperacion("admin@test.com"));

            verify(tokenRepository).save(any(PasswordResetToken.class));
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
            String tokenPlano = "valid-token";
            String tokenHash = sha256Hex(tokenPlano);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(tokenHash);
            resetToken.setUsuario(mockUsuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));

            when(tokenRepository.findByToken(tokenHash)).thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPass");

            authService.cambiarContrasena(tokenPlano, "NewPassword123");

            assertEquals("newHashedPass", mockUsuario.getPassword());
            verify(tokenRepository).delete(resetToken);
            verify(tokenBlacklistService).revocarTokensEmitidosAntesDeAhora(mockUsuario.getCorreo());
        }

        @Test
        @DisplayName("Token inexistente lanza TokenInvalidoException")
        void cambiarContrasena_TokenNoExiste() {
            String tokenPlano = "invalid-token";
            String tokenHash = sha256Hex(tokenPlano);
            when(tokenRepository.findByToken(tokenHash)).thenReturn(Optional.empty());

            TokenInvalidoException ex = assertThrows(
                    TokenInvalidoException.class,
                    () -> authService.cambiarContrasena(tokenPlano, "NewPassword123")
            );

            assertEquals("Token inválido o no existe", ex.getMessage());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Token expirado lanza TokenInvalidoException y elimina el token")
        void cambiarContrasena_TokenExpirado() {
            String tokenPlano = "expired-token";
            String tokenHash = sha256Hex(tokenPlano);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(tokenHash);
            resetToken.setUsuario(mockUsuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));

            when(tokenRepository.findByToken(tokenHash)).thenReturn(Optional.of(resetToken));

            TokenInvalidoException ex = assertThrows(
                    TokenInvalidoException.class,
                    () -> authService.cambiarContrasena(tokenPlano, "NewPassword123")
            );

            assertEquals("El token ha expirado. Solicite uno nuevo.", ex.getMessage());
            verify(tokenRepository).delete(resetToken);
            verify(usuarioRepository, never()).save(any());
        }
    }

    // =========================================================================
    // PURGAR TOKENS EXPIRADOS
    // =========================================================================
    @Nested
    @DisplayName("Purgar tokens expirados")
    class PurgarTokensExpiradosTests {

        @Test
        @DisplayName("Invoca eliminación de tokens anteriores a la fecha y hora actual")
        void purgarTokensExpirados_EjecutaEliminacion() {
            authService.purgarTokensExpirados();
            verify(tokenRepository).deleteByFechaExpiracionBefore(any(LocalDateTime.class));
        }
    }

    // =========================================================================
    // CERRAR SESIÓN (LOGOUT)
    // =========================================================================
    @Nested
    @DisplayName("Cerrar sesión")
    class CerrarSesionTests {

        @Test
        @DisplayName("Header con Bearer revoca token en blacklist")
        void cerrarSesion_ConBearerToken_Revoca() {
            String header = "Bearer jwt-de-prueba";
            when(jwtUtil.extraerTokenDelHeader(header)).thenReturn("jwt-de-prueba");

            authService.cerrarSesion(header);

            verify(tokenBlacklistService, times(1)).revocar("jwt-de-prueba");
        }

        @Test
        @DisplayName("Header nulo o sin Bearer no interactúa con blacklist")
        void cerrarSesion_HeaderInvalido_NoRevoca() {
            authService.cerrarSesion(null);
            authService.cerrarSesion("Basic 12345");

            verify(tokenBlacklistService, never()).revocar(anyString());
        }

        @Test
        @DisplayName("Taquillero con turno de caja abierto lanza ConflictoException al cerrar sesión")
        void cerrarSesion_TaquilleroConCajaAbierta_LanzaConflictoException() {
            String header = "Bearer jwt-taquillero";
            when(jwtUtil.extraerTokenDelHeader(header)).thenReturn("jwt-taquillero");
            when(jwtUtil.extraerCorreo("jwt-taquillero")).thenReturn("taquillero@test.com");

            Usuario taquillero = new Usuario();
            taquillero.setId(10L);
            taquillero.setCorreo("taquillero@test.com");
            Rol rolTaquillero = new Rol();
            rolTaquillero.setNombre(NombreRol.TAQUILLERO);
            taquillero.setRol(rolTaquillero);

            when(usuarioRepository.findByCorreo("taquillero@test.com")).thenReturn(Optional.of(taquillero));
            when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                    .thenReturn(Optional.of(new TurnoCaja()));

            assertThrows(ConflictoException.class, () -> authService.cerrarSesion(header));
            verify(tokenBlacklistService, never()).revocar(anyString());
        }

        @Test
        @DisplayName("Taquillero sin turno de caja abierto cierra sesión normalmente")
        void cerrarSesion_TaquilleroSinCajaAbierta_Revoca() {
            String header = "Bearer jwt-taquillero";
            when(jwtUtil.extraerTokenDelHeader(header)).thenReturn("jwt-taquillero");
            when(jwtUtil.extraerCorreo("jwt-taquillero")).thenReturn("taquillero@test.com");

            Usuario taquillero = new Usuario();
            taquillero.setId(10L);
            taquillero.setCorreo("taquillero@test.com");
            Rol rolTaquillero = new Rol();
            rolTaquillero.setNombre(NombreRol.TAQUILLERO);
            taquillero.setRol(rolTaquillero);

            when(usuarioRepository.findByCorreo("taquillero@test.com")).thenReturn(Optional.of(taquillero));
            when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                    .thenReturn(Optional.empty());

            authService.cerrarSesion(header);

            verify(tokenBlacklistService, times(1)).revocar("jwt-taquillero");
        }
    }

    private String sha256Hex(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
