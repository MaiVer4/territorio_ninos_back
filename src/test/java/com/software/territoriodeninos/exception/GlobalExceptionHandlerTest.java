package com.software.territoriodeninos.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - Tests unitarios")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable retorna 400 con mensaje semántico claro")
    void handleHttpMessageNotReadable_Retorna400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Malformed JSON",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertEquals("Formato JSON inválido", response.getBody().getMensaje());
        assertTrue(response.getBody().getDetalles().toString().contains("formato JSON válido"));
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch retorna 400 con detalle del parámetro inválido")
    void handleMethodArgumentTypeMismatch_Retorna400() {
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "id",
                methodParameter,
                new IllegalArgumentException("For input string: \"abc\"")
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertEquals("Tipo de parámetro inválido", response.getBody().getMensaje());
        assertTrue(response.getBody().getDetalles().toString().contains("id"));
        assertTrue(response.getBody().getDetalles().toString().contains("abc"));
        assertTrue(response.getBody().getDetalles().toString().contains("Long"));
    }

    @Test
    @DisplayName("handleRecursoNoEncontrado retorna 404")
    void handleRecursoNoEncontrado_Retorna404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Usuario no encontrado con id 1");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRecursoNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Recurso no encontrado", response.getBody().getMensaje());
        assertEquals("Usuario no encontrado con id 1", response.getBody().getDetalles());
    }

    @Test
    @DisplayName("handleConflicto retorna 409")
    void handleConflicto_Retorna409() {
        ConflictoException ex = new ConflictoException("El correo ya existe");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflicto(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflicto en los datos", response.getBody().getMensaje());
        assertEquals("El correo ya existe", response.getBody().getDetalles());
    }

    @Test
    @DisplayName("handleCredencialesInvalidas retorna 401")
    void handleCredencialesInvalidas_Retorna401() {
        CredencialesInvalidasException ex = new CredencialesInvalidasException("Credenciales incorrectas");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCredencialesInvalidas(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("handleCuentaInactiva retorna 403")
    void handleCuentaInactiva_Retorna403() {
        CuentaInactivaException ex = new CuentaInactivaException("Cuenta inactiva");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCuentaInactiva(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Cuenta inactiva", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("handleCuentaBloqueada retorna 423")
    void handleCuentaBloqueada_Retorna423() {
        CuentaBloqueadaException ex = new CuentaBloqueadaException("Cuenta bloqueada temporalmente");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCuentaBloqueada(ex);

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        assertEquals("Cuenta bloqueada", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("handleAccessDenied retorna 403")
    void handleAccessDenied_Retorna403() {
        AccessDeniedException ex = new AccessDeniedException("No tiene permisos");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Acceso denegado", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("handleTokenInvalido retorna 401")
    void handleTokenInvalido_Retorna401() {
        TokenInvalidoException ex = new TokenInvalidoException("Token vencido");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTokenInvalido(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token inválido o expirado", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("handleValidationExceptions retorna 400 con mapa de errores")
    void handleValidationExceptions_Retorna400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "correo", "El correo es requerido");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ValidationErrorResponse> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error de validación", response.getBody().message());
        assertEquals("El correo es requerido", response.getBody().errors().get("correo"));
    }

    @Test
    @DisplayName("handleIllegalArgument retorna 400")
    void handleIllegalArgument_Retorna400() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error en la solicitud", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("handleDateTimeParseException retorna 400")
    void handleDateTimeParseException_Retorna400() {
        java.time.format.DateTimeParseException ex = new java.time.format.DateTimeParseException("Text could not be parsed", "invalida", 0);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDateTimeParseException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Formato de fecha inválido", response.getBody().getMensaje());
        assertEquals("El formato de la fecha especificada es inválido. Use el formato YYYY-MM-DD.", response.getBody().getDetalles());
    }

    @Test
    @DisplayName("handleGlobalException retorna 500")
    void handleGlobalException_Retorna500() {
        Exception ex = new RuntimeException("Error inesperado");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", response.getBody().getMensaje());
    }
}
