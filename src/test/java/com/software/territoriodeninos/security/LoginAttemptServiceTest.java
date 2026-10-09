package com.software.territoriodeninos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginAttemptService - Tests unitarios de seguridad contra fuerza bruta (RNF7)")
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Nested
    @DisplayName("Gestión de Intentos y Bloqueo")
    class IntentosYBloqueoTests {

        @Test
        @DisplayName("Menos de 5 intentos fallidos no deben bloquear al usuario")
        void testIntentosFallidosMenoresAlLimite_NoBloquean() {
            // Arrange
            String correo = "usuario@parque.com";

            // Act
            for (int i = 1; i <= 4; i++) {
                loginAttemptService.loginFailed(correo);
                // Assert
                assertFalse(loginAttemptService.isBlocked(correo), "No debe bloquearse en el intento " + i);
                assertEquals(0, loginAttemptService.getRemainingLockoutMinutes(correo));
            }
        }

        @Test
        @DisplayName("Exactamente 5 intentos fallidos consecutivos deben bloquear la cuenta")
        void testCincoIntentosFallidos_BloqueanCuenta() {
            // Arrange
            String correo = "admin@parque.com";

            // Act
            for (int i = 1; i <= 5; i++) {
                loginAttemptService.loginFailed(correo);
            }

            // Assert
            assertTrue(loginAttemptService.isBlocked(correo), "Debe bloquearse al 5to intento consecutivo");
            assertTrue(loginAttemptService.getRemainingLockoutMinutes(correo) > 0, "Debe tener minutos de bloqueo restantes");
            assertTrue(loginAttemptService.getRemainingLockoutMinutes(correo) <= 15, "El tiempo máximo de bloqueo inicial es 15 minutos");
        }

        @Test
        @DisplayName("Intentos adicionales después del bloqueo mantienen el bloqueo")
        void testIntentosDespuesDeBloqueado_MantienenBloqueo() {
            // Arrange
            String correo = "hacker@test.com";
            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correo);
            }

            // Act
            loginAttemptService.loginFailed(correo); // 6to intento

            // Assert
            assertTrue(loginAttemptService.isBlocked(correo));
        }

        @Test
        @DisplayName("Las claves deben normalizarse (ignorar mayúsculas y espacios en blanco)")
        void testNormalizacionDeClave() {
            // Arrange
            String correoEspaciado = "  Operario@Parque.com  ";
            String correoLimpio = "operario@parque.com";

            // Act
            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correoEspaciado);
            }

            // Assert
            assertTrue(loginAttemptService.isBlocked(correoLimpio), "Debe detectar el bloqueo con la clave limpia");
            assertTrue(loginAttemptService.isBlocked(correoEspaciado), "Debe detectar el bloqueo con espacios");
        }
    }

    @Nested
    @DisplayName("Desbloqueo y Restablecimiento")
    class DesbloqueoYResetTests {

        @Test
        @DisplayName("loginSucceeded() debe limpiar los intentos fallidos acumulados")
        void testLoginSucceeded_LimpiaIntentos() {
            // Arrange
            String correo = "taquillero@parque.com";
            for (int i = 0; i < 4; i++) {
                loginAttemptService.loginFailed(correo);
            }

            // Act
            loginAttemptService.loginSucceeded(correo);

            // Assert
            assertFalse(loginAttemptService.isBlocked(correo));
            assertEquals(0, loginAttemptService.getRemainingLockoutMinutes(correo));

            // Si falla 4 veces más tras login exitoso, no debe bloquearse (el contador empezó de cero)
            for (int i = 0; i < 4; i++) {
                loginAttemptService.loginFailed(correo);
            }
            assertFalse(loginAttemptService.isBlocked(correo));
        }

        @Test
        @DisplayName("loginSucceeded() debe desbloquear inmediatamente si ya estaba bloqueado")
        void testLoginSucceeded_DesbloqueaCuentaBloqueada() {
            // Arrange
            String correo = "admin@parque.com";
            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correo);
            }
            assertTrue(loginAttemptService.isBlocked(correo));

            // Act
            loginAttemptService.loginSucceeded(correo);

            // Assert
            assertFalse(loginAttemptService.isBlocked(correo));
            assertEquals(0, loginAttemptService.getRemainingLockoutMinutes(correo));
        }

        @Test
        @DisplayName("reset() debe remover el usuario de la caché de intentos")
        void testReset_RemueveUsuario() {
            // Arrange
            String correo = "usuario@test.com";
            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correo);
            }
            assertTrue(loginAttemptService.isBlocked(correo));

            // Act
            loginAttemptService.reset(correo);

            // Assert
            assertFalse(loginAttemptService.isBlocked(correo));
            assertEquals(0, loginAttemptService.getRemainingLockoutMinutes(correo));
        }
    }

    @Nested
    @DisplayName("Expiración Temporal y Limpieza de Caché")
    class ExpiracionYLimpiezaTests {

        @Test
        @DisplayName("isBlocked() debe retornar false y limpiar la entrada cuando el tiempo de bloqueo ha expirado")
        @SuppressWarnings("unchecked")
        void testIsBlocked_Expirado() throws Exception {
            // Arrange
            String correo = "expirado@parque.com";
            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correo);
            }
            assertTrue(loginAttemptService.isBlocked(correo));

            // Simular expiración de tiempo modificando el registro vía reflexión
            Field cacheField = LoginAttemptService.class.getDeclaredField("attemptsCache");
            cacheField.setAccessible(true);
            Map<String, Object> cache = (Map<String, Object>) cacheField.get(loginAttemptService);
            Object record = cache.get(correo);

            Field lockedUntilField = record.getClass().getDeclaredField("lockedUntil");
            lockedUntilField.setAccessible(true);
            lockedUntilField.set(record, Instant.now().minus(1, ChronoUnit.MINUTES));

            // Act
            boolean estaBloqueado = loginAttemptService.isBlocked(correo);

            // Assert
            assertFalse(estaBloqueado, "Debe estar desbloqueado una vez transcurrido el tiempo");
            assertNull(cache.get(correo), "Debe removerse de la caché automáticamente");
        }

        @Test
        @DisplayName("loginFailed() debe reiniciar los intentos si el bloqueo anterior ya expiró")
        @SuppressWarnings("unchecked")
        void testLoginFailed_TrasExpiracionReiniciaContador() throws Exception {
            // Arrange
            String correo = "reintento@parque.com";
            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correo);
            }

            Field cacheField = LoginAttemptService.class.getDeclaredField("attemptsCache");
            cacheField.setAccessible(true);
            Map<String, Object> cache = (Map<String, Object>) cacheField.get(loginAttemptService);
            Object record = cache.get(correo);

            Field lockedUntilField = record.getClass().getDeclaredField("lockedUntil");
            lockedUntilField.setAccessible(true);
            lockedUntilField.set(record, Instant.now().minus(1, ChronoUnit.MINUTES));

            // Act: Un nuevo intento fallido después de expirar
            loginAttemptService.loginFailed(correo);

            // Assert
            assertFalse(loginAttemptService.isBlocked(correo), "No debe bloquearse inmediatamente con 1 nuevo intento");
            Field attemptsField = record.getClass().getDeclaredField("attempts");
            attemptsField.setAccessible(true);
            int attempts = (int) attemptsField.get(record);
            assertEquals(1, attempts, "El contador de intentos debe haberse reiniciado a 1");
        }

        @Test
        @DisplayName("limpiarRegistrosExpirados() debe purgar registros inactivos y bloqueos pasados")
        @SuppressWarnings("unchecked")
        void testLimpiarRegistrosExpirados() throws Exception {
            // Arrange
            String correoInactivo = "inactivo@parque.com";
            String correoBloqueadoExpirado = "bloqueo.expirado@parque.com";
            String correoActivoReciente = "reciente@parque.com";

            loginAttemptService.loginFailed(correoInactivo);
            loginAttemptService.loginFailed(correoActivoReciente);

            for (int i = 0; i < 5; i++) {
                loginAttemptService.loginFailed(correoBloqueadoExpirado);
            }

            Field cacheField = LoginAttemptService.class.getDeclaredField("attemptsCache");
            cacheField.setAccessible(true);
            Map<String, Object> cache = (Map<String, Object>) cacheField.get(loginAttemptService);

            // Configurar correoInactivo como inactivo hace 20 minutos
            Object recordInactivo = cache.get(correoInactivo);
            Field lastAttemptField = recordInactivo.getClass().getDeclaredField("lastAttempt");
            lastAttemptField.setAccessible(true);
            lastAttemptField.set(recordInactivo, Instant.now().minus(20, ChronoUnit.MINUTES));

            // Configurar correoBloqueadoExpirado como expirado hace 5 minutos
            Object recordBloqueado = cache.get(correoBloqueadoExpirado);
            Field lockedUntilField = recordBloqueado.getClass().getDeclaredField("lockedUntil");
            lockedUntilField.setAccessible(true);
            lockedUntilField.set(recordBloqueado, Instant.now().minus(5, ChronoUnit.MINUTES));

            // Act
            loginAttemptService.limpiarRegistrosExpirados();

            // Assert
            assertFalse(cache.containsKey(correoInactivo), "Debe haber eliminado el registro inactivo");
            assertFalse(cache.containsKey(correoBloqueadoExpirado), "Debe haber eliminado el bloqueo expirado");
            assertTrue(cache.containsKey(correoActivoReciente), "Debe conservar el registro activo reciente");
        }
    }
}
