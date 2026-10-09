package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.ChangePasswordRequest;
import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.TokenInvalidoException;
import com.software.territoriodeninos.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController - Pruebas de integración Web con MockMvc")
class UsuarioControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private UsuarioResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockResponseDTO = UsuarioResponseDTO.builder()
                .id(1L)
                .nombre("Carlos Gómez")
                .correo("carlos@parque.com")
                .estado("ACTIVO")
                .rol("ADMINISTRADOR")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/usuarios - Registrar nuevo usuario")
    class RegistrarUsuarioTests {

        @Test
        @DisplayName("201 Created - Registro exitoso de usuario (Happy Path)")
        void testRegistrar_Exito_Created() throws Exception {
            // Arrange
            UsuarioRegistroDTO registroDTO = UsuarioRegistroDTO.builder()
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .password("Password123")
                    .nombreRol("ADMINISTRADOR")
                    .build();

            when(usuarioService.registrar(any(UsuarioRegistroDTO.class))).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registroDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.nombre", is("Carlos Gómez")))
                    .andExpect(jsonPath("$.correo", is("carlos@parque.com")))
                    .andExpect(jsonPath("$.estado", is("ACTIVO")))
                    .andExpect(jsonPath("$.rol", is("ADMINISTRADOR")));

            verify(usuarioService, times(1)).registrar(any(UsuarioRegistroDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por campos requeridos vacíos")
        void testRegistrar_CamposVacios_BadRequest() throws Exception {
            // Arrange: DTO con valores vacíos
            UsuarioRegistroDTO invalido = UsuarioRegistroDTO.builder()
                    .nombre("")
                    .correo("")
                    .password("")
                    .nombreRol("")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Error de validación")))
                    .andExpect(jsonPath("$.errors.nombre").exists())
                    .andExpect(jsonPath("$.errors.correo").exists())
                    .andExpect(jsonPath("$.errors.password").exists())
                    .andExpect(jsonPath("$.errors.nombreRol").exists());

            verify(usuarioService, never()).registrar(any());
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por formato de correo inválido")
        void testRegistrar_CorreoInvalido_BadRequest() throws Exception {
            // Arrange
            UsuarioRegistroDTO invalido = UsuarioRegistroDTO.builder()
                    .nombre("Carlos Gómez")
                    .correo("correo-invalido-sin-arroba")
                    .password("Password123")
                    .nombreRol("TAQUILLERO")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.correo").value("El formato del correo no es válido"));

            verify(usuarioService, never()).registrar(any());
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por complejidad de contraseña débil")
        void testRegistrar_PasswordDebil_BadRequest() throws Exception {
            // Arrange: contraseña sin números ni mayúsculas
            UsuarioRegistroDTO invalido = UsuarioRegistroDTO.builder()
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .password("debil")
                    .nombreRol("OPERARIO")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.password").exists());

            verify(usuarioService, never()).registrar(any());
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando el correo ya está registrado en el sistema")
        void testRegistrar_CorreoDuplicado_Conflict() throws Exception {
            // Arrange
            UsuarioRegistroDTO registroDTO = UsuarioRegistroDTO.builder()
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .password("Password123")
                    .nombreRol("ADMINISTRADOR")
                    .build();

            when(usuarioService.registrar(any(UsuarioRegistroDTO.class)))
                    .thenThrow(new ConflictoException("El correo carlos@parque.com ya está registrado"));

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registroDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.mensaje", is("Conflicto en los datos")))
                    .andExpect(jsonPath("$.detalles", is("El correo carlos@parque.com ya está registrado")));

            verify(usuarioService, times(1)).registrar(any(UsuarioRegistroDTO.class));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el solicitante no tiene rol ADMINISTRADOR")
        void testRegistrar_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            UsuarioRegistroDTO registroDTO = UsuarioRegistroDTO.builder()
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .password("Password123")
                    .nombreRol("ADMINISTRADOR")
                    .build();

            when(usuarioService.registrar(any(UsuarioRegistroDTO.class)))
                    .thenThrow(new AccessDeniedException("Sin permisos para crear usuario"));

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registroDTO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)))
                    .andExpect(jsonPath("$.mensaje", is("Acceso denegado")))
                    .andExpect(jsonPath("$.detalles", is("Sin permisos para crear usuario")));
        }

        @Test
        @DisplayName("401 Unauthorized - Token JWT inválido o expirado en cabecera")
        void testRegistrar_TokenInvalido_Unauthorized() throws Exception {
            // Arrange
            UsuarioRegistroDTO registroDTO = UsuarioRegistroDTO.builder()
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .password("Password123")
                    .nombreRol("ADMINISTRADOR")
                    .build();

            when(usuarioService.registrar(any(UsuarioRegistroDTO.class)))
                    .thenThrow(new TokenInvalidoException("Token JWT expirado"));

            // Act & Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registroDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo", is(401)))
                    .andExpect(jsonPath("$.mensaje", is("Token inválido o expirado")));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios - Obtener todos los usuarios")
    class ObtenerTodosUsuariosTests {

        @Test
        @DisplayName("200 OK - Obtiene la lista completa de usuarios (Happy Path)")
        void testObtenerTodos_Ok() throws Exception {
            // Arrange
            when(usuarioService.obtenerTodos()).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].nombre", is("Carlos Gómez")))
                    .andExpect(jsonPath("$[0].correo", is("carlos@parque.com")));

            verify(usuarioService, times(1)).obtenerTodos();
        }

        @Test
        @DisplayName("403 Forbidden - Usuario sin privilegios no puede listar usuarios")
        void testObtenerTodos_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            when(usuarioService.obtenerTodos())
                    .thenThrow(new AccessDeniedException("No tiene permisos para consultar usuarios"));

            // Act & Assert
            mockMvc.perform(get("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/{id} - Obtener usuario por ID")
    class ObtenerPorIdTests {

        @Test
        @DisplayName("200 OK - Retorna el usuario por su identificador (Happy Path)")
        void testObtenerPorId_Encontrado_Ok() throws Exception {
            // Arrange
            when(usuarioService.obtenerPorId(1L)).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(get("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.nombre", is("Carlos Gómez")))
                    .andExpect(jsonPath("$.correo", is("carlos@parque.com")));

            verify(usuarioService, times(1)).obtenerPorId(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el usuario no existe o está inactivo")
        void testObtenerPorId_NoEncontrado_NotFound() throws Exception {
            // Arrange
            when(usuarioService.obtenerPorId(99L))
                    .thenThrow(new RecursoNoEncontradoException("Usuario con ID 99 no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/usuarios/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)))
                    .andExpect(jsonPath("$.mensaje", is("Recurso no encontrado")))
                    .andExpect(jsonPath("$.detalles", is("Usuario con ID 99 no encontrado")));

            verify(usuarioService, times(1)).obtenerPorId(99L);
        }
    }

    @Nested
    @DisplayName("PUT /api/usuarios/{id} - Actualizar usuario")
    class ActualizarUsuarioTests {

        @Test
        @DisplayName("200 OK - Actualización de datos exitosa (Happy Path)")
        void testActualizar_Exito_Ok() throws Exception {
            // Arrange
            UsuarioUpdateDTO updateDTO = UsuarioUpdateDTO.builder()
                    .nombre("Carlos Gómez Actualizado")
                    .correo("carlos.nuevo@parque.com")
                    .estado("ACTIVO")
                    .rol("ADMINISTRADOR")
                    .build();

            UsuarioResponseDTO actualizadoDTO = UsuarioResponseDTO.builder()
                    .id(1L)
                    .nombre("Carlos Gómez Actualizado")
                    .correo("carlos.nuevo@parque.com")
                    .estado("ACTIVO")
                    .rol("ADMINISTRADOR")
                    .fechaCreacion(LocalDateTime.now())
                    .fechaActualizacion(LocalDateTime.now())
                    .build();

            when(usuarioService.actualizar(eq(1L), any(UsuarioUpdateDTO.class))).thenReturn(actualizadoDTO);

            // Act & Assert
            mockMvc.perform(put("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre", is("Carlos Gómez Actualizado")))
                    .andExpect(jsonPath("$.correo", is("carlos.nuevo@parque.com")));

            verify(usuarioService, times(1)).actualizar(eq(1L), any(UsuarioUpdateDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por formato de correo inválido")
        void testActualizar_CorreoInvalido_BadRequest() throws Exception {
            // Arrange
            UsuarioUpdateDTO invalido = UsuarioUpdateDTO.builder()
                    .correo("formato-invalido")
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.correo").value("El formato del correo no es válido"));

            verify(usuarioService, never()).actualizar(any(), any());
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el usuario a actualizar no existe")
        void testActualizar_NoEncontrado_NotFound() throws Exception {
            // Arrange
            UsuarioUpdateDTO updateDTO = UsuarioUpdateDTO.builder()
                    .nombre("Juan")
                    .build();

            when(usuarioService.actualizar(eq(99L), any(UsuarioUpdateDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Usuario con ID 99 no encontrado"));

            // Act & Assert
            mockMvc.perform(put("/api/usuarios/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)))
                    .andExpect(jsonPath("$.detalles", is("Usuario con ID 99 no encontrado")));
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando el nuevo correo ya está en uso por otro usuario")
        void testActualizar_CorreoDuplicado_Conflict() throws Exception {
            // Arrange
            UsuarioUpdateDTO updateDTO = UsuarioUpdateDTO.builder()
                    .correo("duplicado@parque.com")
                    .build();

            when(usuarioService.actualizar(eq(1L), any(UsuarioUpdateDTO.class)))
                    .thenThrow(new ConflictoException("El correo duplicado@parque.com ya está en uso"));

            // Act & Assert
            mockMvc.perform(put("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("El correo duplicado@parque.com ya está en uso")));
        }

        @Test
        @DisplayName("403 Forbidden - Retorna prohibido cuando un no-administrador intenta modificar rol/estado de otro usuario")
        void testActualizar_SinPrivilegios_Forbidden() throws Exception {
            // Arrange
            UsuarioUpdateDTO updateDTO = UsuarioUpdateDTO.builder()
                    .rol("ADMINISTRADOR")
                    .build();

            when(usuarioService.actualizar(eq(2L), any(UsuarioUpdateDTO.class)))
                    .thenThrow(new AccessDeniedException("Privilegios insuficientes para modificar a otro usuario"));

            // Act & Assert
            mockMvc.perform(put("/api/usuarios/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)))
                    .andExpect(jsonPath("$.detalles", is("Privilegios insuficientes para modificar a otro usuario")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/usuarios/{id}/password - Cambiar contraseña")
    class CambiarPasswordTests {

        @Test
        @DisplayName("200 OK - Cambio de contraseña exitoso (Happy Path)")
        void testCambiarPassword_Exito_Ok() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setPasswordActual("Password123");
            request.setNuevaContrasena("NuevaPassword456");

            doNothing().when(usuarioService).cambiarPassword(eq(1L), any(ChangePasswordRequest.class));

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje", is("Contraseña actualizada exitosamente.")));

            verify(usuarioService, times(1)).cambiarPassword(eq(1L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por contraseña actual vacía")
        void testCambiarPassword_ContrasenaActualVacia_BadRequest() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setPasswordActual("");
            request.setNuevaContrasena("NuevaPassword456");

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.passwordActual").value("La contraseña actual es requerida"));

            verify(usuarioService, never()).cambiarPassword(any(), any());
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por nueva contraseña débil")
        void testCambiarPassword_NuevaContrasenaDebil_BadRequest() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setPasswordActual("Password123");
            request.setNuevaContrasena("debil");

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.nuevaContrasena").exists());

            verify(usuarioService, never()).cambiarPassword(any(), any());
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando la contraseña actual no coincide")
        void testCambiarPassword_ActualIncorrecta_Conflict() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setPasswordActual("Erronea123");
            request.setNuevaContrasena("NuevaPassword456");

            doThrow(new ConflictoException("La contraseña actual es incorrecta"))
                    .when(usuarioService).cambiarPassword(eq(1L), any(ChangePasswordRequest.class));

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("La contraseña actual es incorrecta")));
        }

        @Test
        @DisplayName("403 Forbidden - Retorna prohibido al intentar cambiar la contraseña de otro usuario")
        void testCambiarPassword_OtroUsuario_Forbidden() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setPasswordActual("Password123");
            request.setNuevaContrasena("NuevaPassword456");

            doThrow(new AccessDeniedException("Solo puedes modificar tu propia contraseña"))
                    .when(usuarioService).cambiarPassword(eq(2L), any(ChangePasswordRequest.class));

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/2/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)))
                    .andExpect(jsonPath("$.detalles", is("Solo puedes modificar tu propia contraseña")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/usuarios/{id}/inactivar - Inactivar usuario")
    class InactivarUsuarioTests {

        @Test
        @DisplayName("200 OK - Inactiva el usuario exitosamente (Happy Path)")
        void testInactivar_Exito_Ok() throws Exception {
            // Arrange
            UsuarioResponseDTO inactivadoDTO = UsuarioResponseDTO.builder()
                    .id(1L)
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .estado("INACTIVO")
                    .rol("ADMINISTRADOR")
                    .fechaCreacion(LocalDateTime.now())
                    .fechaActualizacion(LocalDateTime.now())
                    .build();

            when(usuarioService.inactivar(1L)).thenReturn(inactivadoDTO);

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/inactivar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.estado", is("INACTIVO")));

            verify(usuarioService, times(1)).inactivar(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el usuario a inactivar no existe")
        void testInactivar_NoEncontrado_NotFound() throws Exception {
            // Arrange
            when(usuarioService.inactivar(99L))
                    .thenThrow(new RecursoNoEncontradoException("Usuario con ID 99 no encontrado"));

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/99/inactivar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Retorna prohibido cuando el rol no es ADMINISTRADOR")
        void testInactivar_SinPermisos_Forbidden() throws Exception {
            // Arrange
            when(usuarioService.inactivar(1L))
                    .thenThrow(new AccessDeniedException("Solo un administrador puede inactivar usuarios"));

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/inactivar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/usuarios/{id}/activar - Activar usuario")
    class ActivarUsuarioTests {

        @Test
        @DisplayName("200 OK - Activa el usuario exitosamente (Happy Path)")
        void testActivar_Exito_Ok() throws Exception {
            // Arrange
            UsuarioResponseDTO activadoDTO = UsuarioResponseDTO.builder()
                    .id(1L)
                    .nombre("Carlos Gómez")
                    .correo("carlos@parque.com")
                    .estado("ACTIVO")
                    .rol("ADMINISTRADOR")
                    .fechaCreacion(LocalDateTime.now())
                    .fechaActualizacion(LocalDateTime.now())
                    .build();

            when(usuarioService.activar(1L)).thenReturn(activadoDTO);

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/1/activar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.estado", is("ACTIVO")));

            verify(usuarioService, times(1)).activar(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el usuario a activar no existe")
        void testActivar_NoEncontrado_NotFound() throws Exception {
            // Arrange
            when(usuarioService.activar(99L))
                    .thenThrow(new RecursoNoEncontradoException("Usuario con ID 99 no encontrado"));

            // Act & Assert
            mockMvc.perform(patch("/api/usuarios/99/activar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/rol/{rol} - Obtener usuarios por rol")
    class ObtenerPorRolTests {

        @Test
        @DisplayName("200 OK - Retorna lista de usuarios con el rol especificado (Happy Path)")
        void testObtenerPorRol_Exito_Ok() throws Exception {
            // Arrange
            when(usuarioService.obtenerPorRol("ADMINISTRADOR")).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/usuarios/rol/ADMINISTRADOR")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].rol", is("ADMINISTRADOR")));

            verify(usuarioService, times(1)).obtenerPorRol("ADMINISTRADOR");
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando el rol especificado no es válido")
        void testObtenerPorRol_RolInvalido_Conflict() throws Exception {
            // Arrange
            when(usuarioService.obtenerPorRol("INVALIDO"))
                    .thenThrow(new ConflictoException("Rol inválido: INVALIDO"));

            // Act & Assert
            mockMvc.perform(get("/api/usuarios/rol/INVALIDO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("Rol inválido: INVALIDO")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/usuarios/{id} - Eliminar usuario permanentemente")
    class EliminarUsuarioTests {

        @Test
        @DisplayName("204 No Content - Eliminación física exitosa del usuario (Happy Path)")
        void testEliminar_Exito_NoContent() throws Exception {
            // Arrange
            doNothing().when(usuarioService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(usuarioService, times(1)).eliminar(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el usuario a eliminar no existe")
        void testEliminar_NoEncontrado_NotFound() throws Exception {
            // Arrange
            doThrow(new RecursoNoEncontradoException("Usuario con ID 99 no encontrado"))
                    .when(usuarioService).eliminar(99L);

            // Act & Assert
            mockMvc.perform(delete("/api/usuarios/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));

            verify(usuarioService, times(1)).eliminar(99L);
        }

        @Test
        @DisplayName("403 Forbidden - Retorna prohibido cuando el rol no es ADMINISTRADOR")
        void testEliminar_SinPermisos_Forbidden() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("Solo un administrador puede eliminar usuarios"))
                    .when(usuarioService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("Manejo de Errores Semánticos en Controller (GlobalExceptionHandler)")
    class ManejoErroresSemanticosTests {

        @Test
        @DisplayName("400 Bad Request - Formato JSON inválido al registrar usuario")
        void testRegistrar_JsonInvalido_BadRequest() throws Exception {
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ nombre: malformado without quotes }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is(400)))
                    .andExpect(jsonPath("$.mensaje", is("Formato JSON inválido")));
        }

        @Test
        @DisplayName("400 Bad Request - Tipo de parámetro inválido en PathVariable")
        void testObtenerPorId_TipoParametroInvalido_BadRequest() throws Exception {
            mockMvc.perform(get("/api/usuarios/no-es-un-numero")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is(400)))
                    .andExpect(jsonPath("$.mensaje", is("Tipo de parámetro inválido")))
                    .andExpect(jsonPath("$.detalles", is("El parámetro 'id' con valor 'no-es-un-numero' no es válido. Se esperaba un tipo 'Long'.")));
        }
    }
}
