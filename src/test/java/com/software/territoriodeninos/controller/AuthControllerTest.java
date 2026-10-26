package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.ForgotPasswordRequest;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.dto.ResetPasswordRequest;
import com.software.territoriodeninos.exception.*;
import com.software.territoriodeninos.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Pruebas de integración Web con MockMvc")
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/login - Inicio de sesión")
    class LoginEndpointTests {

        @Test
        @DisplayName("200 OK - Login exitoso retorna token y datos de usuario (Happy Path)")
        void testLogin_Exito_Ok() throws Exception {
            // Arrange
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@parque.com")
                    .password("Password123")
                    .build();

            AuthResponseDTO responseDTO = new AuthResponseDTO();
            responseDTO.setToken("jwt.mock.token");
            responseDTO.setNombre("Administrador");
            responseDTO.setRol("ADMINISTRADOR");

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", is("jwt.mock.token")))
                    .andExpect(jsonPath("$.nombre", is("Administrador")))
                    .andExpect(jsonPath("$.rol", is("ADMINISTRADOR")));

            verify(authService, times(1)).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por formato de correo inválido o contraseña vacía")
        void testLogin_DatosInvalidos_BadRequest() throws Exception {
            // Arrange
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("correo-sin-formato")
                    .password("")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Error de validación")))
                    .andExpect(jsonPath("$.errors.correo").exists())
                    .andExpect(jsonPath("$.errors.password").exists());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("401 Unauthorized - Falla de autenticación por credenciales inválidas")
        void testLogin_CredencialesInvalidas_Unauthorized() throws Exception {
            // Arrange
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("admin@parque.com")
                    .password("PasswordIncorrecta")
                    .build();

            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo", is(401)))
                    .andExpect(jsonPath("$.mensaje", is("Credenciales inválidas")))
                    .andExpect(jsonPath("$.detalles", is("Correo o contraseña incorrectos")));
        }

        @Test
        @DisplayName("403 Forbidden - Cuenta de usuario inactiva")
        void testLogin_CuentaInactiva_Forbidden() throws Exception {
            // Arrange
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("inactivo@parque.com")
                    .password("Password123")
                    .build();

            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new CuentaInactivaException("La cuenta se encuentra inactiva. Contacte al administrador."));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)))
                    .andExpect(jsonPath("$.mensaje", is("Cuenta inactiva")))
                    .andExpect(jsonPath("$.detalles", is("La cuenta se encuentra inactiva. Contacte al administrador.")));
        }

        @Test
        @DisplayName("423 Locked - Cuenta bloqueada temporalmente por exceso de intentos fallidos (RNF7)")
        void testLogin_CuentaBloqueada_Locked() throws Exception {
            // Arrange
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .correo("bloqueado@parque.com")
                    .password("Password123")
                    .build();

            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new CuentaBloqueadaException("Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intente de nuevo en 15 minutos."));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.codigo", is(423)))
                    .andExpect(jsonPath("$.mensaje", is("Cuenta bloqueada")))
                    .andExpect(jsonPath("$.detalles", is("Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intente de nuevo en 15 minutos.")));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password - Solicitud de recuperación")
    class ForgotPasswordEndpointTests {

        @Test
        @DisplayName("200 OK - Envío de solicitud retorna respuesta genérica segura (Happy Path)")
        void testForgotPassword_Exito_Ok() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("usuario@parque.com");

            doNothing().when(authService).generarTokenRecuperacion("usuario@parque.com");

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje", is("Si el correo existe, se enviará un enlace de recuperación.")));

            verify(authService, times(1)).generarTokenRecuperacion("usuario@parque.com");
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla si el correo no tiene formato válido")
        void testForgotPassword_CorreoInvalido_BadRequest() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("invalido");

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.correo").exists());

            verify(authService, never()).generarTokenRecuperacion(any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/auth/reset-password - Restablecimiento de contraseña")
    class ResetPasswordEndpointTests {

        @Test
        @DisplayName("200 OK - Cambio de contraseña con token exitoso (Happy Path)")
        void testResetPassword_Exito_Ok() throws Exception {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-uuid-token");
            request.setNuevaContrasena("NuevaPassword123");

            doNothing().when(authService).cambiarContrasena("valid-uuid-token", "NuevaPassword123");

            // Act & Assert
            mockMvc.perform(patch("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje", is("Contraseña actualizada exitosamente.")));

            verify(authService, times(1)).cambiarContrasena("valid-uuid-token", "NuevaPassword123");
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por contraseña que no cumple complejidad")
        void testResetPassword_PasswordDebil_BadRequest() throws Exception {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-token");
            request.setNuevaContrasena("debil");

            // Act & Assert
            mockMvc.perform(patch("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.nuevaContrasena").exists());

            verify(authService, never()).cambiarContrasena(any(), any());
        }

        @Test
        @DisplayName("401 Unauthorized - Token de recuperación inválido o expirado")
        void testResetPassword_TokenInvalido_Unauthorized() throws Exception {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("token-expirado");
            request.setNuevaContrasena("NuevaPassword123");

            doThrow(new TokenInvalidoException("El enlace de recuperación es inválido o ha expirado"))
                    .when(authService).cambiarContrasena(eq("token-expirado"), eq("NuevaPassword123"));

            // Act & Assert
            mockMvc.perform(patch("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo", is(401)))
                    .andExpect(jsonPath("$.mensaje", is("Token inválido o expirado")));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout - Cierre de sesión")
    class LogoutEndpointTests {

        @Test
        @DisplayName("200 OK - Cierre de sesión exitoso sin header")
        void testLogout_Exito_Ok() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje", is("Sesión cerrada exitosamente.")));
        }

        @Test
        @DisplayName("200 OK - Cierre de sesión con token revoca token en authService")
        void testLogout_ConToken_RevocaExitosamente() throws Exception {
            String token = "Bearer token-de-prueba";
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje", is("Sesión cerrada exitosamente.")));

            verify(authService, times(1)).cerrarSesion(token);
        }

        @Test
        @DisplayName("POST /api/auth/logout - 409 Conflict cuando taquillero tiene caja abierta")
        void testLogout_TaquilleroConCajaAbierta_Conflict() throws Exception {
            String token = "Bearer token-de-prueba";
            doThrow(new com.software.territoriodeninos.exception.ConflictoException("No puedes cerrar sesión teniendo una caja activa sin cerrar."))
                    .when(authService).cerrarSesion(token);

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("No puedes cerrar sesión teniendo una caja activa sin cerrar.")));
        }
    }
}
