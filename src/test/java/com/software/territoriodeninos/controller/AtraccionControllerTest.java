package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.AtraccionService;
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

import java.math.BigDecimal;
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
@DisplayName("AtraccionController - Pruebas de integración Web con MockMvc")
class AtraccionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AtraccionService atraccionService;

    @InjectMocks
    private AtraccionController atraccionController;

    private AtraccionResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(atraccionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockResponseDTO = new AtraccionResponseDTO(
                1L, "Inflable Castillo", "Para niños", new BigDecimal("50.00"), "ACTIVO", LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/atracciones - Crear atracción")
    class CrearAtraccionTests {

        @Test
        @DisplayName("201 Created - Creación exitosa de atracción (Happy Path)")
        void testCrear_Exito_Created() throws Exception {
            // Arrange
            AtraccionRequestDTO request = new AtraccionRequestDTO("Inflable Castillo", "Para niños", new BigDecimal("50.00"));
            when(atraccionService.crear(any(AtraccionRequestDTO.class))).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/atracciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.nombre", is("Inflable Castillo")))
                    .andExpect(jsonPath("$.estado", is("ACTIVO")));

            verify(atraccionService, times(1)).crear(any(AtraccionRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por nombre vacío o tarifa inválida")
        void testCrear_DatosInvalidos_BadRequest() throws Exception {
            // Arrange: nombre vacío y tarifa cero
            AtraccionRequestDTO request = new AtraccionRequestDTO("", "Para niños", new BigDecimal("0.00"));

            // Act & Assert
            mockMvc.perform(post("/api/atracciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Error de validación")))
                    .andExpect(jsonPath("$.errors.nombre").exists())
                    .andExpect(jsonPath("$.errors.tarifaMinuto").exists());

            verify(atraccionService, never()).crear(any());
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando el nombre de la atracción ya existe")
        void testCrear_NombreDuplicado_Conflict() throws Exception {
            // Arrange
            AtraccionRequestDTO request = new AtraccionRequestDTO("Inflable Castillo", "Para niños", new BigDecimal("50.00"));
            when(atraccionService.crear(any(AtraccionRequestDTO.class)))
                    .thenThrow(new ConflictoException("Ya existe una atracción con el nombre Inflable Castillo"));

            // Act & Assert
            mockMvc.perform(post("/api/atracciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("Ya existe una atracción con el nombre Inflable Castillo")));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el usuario no tiene permisos")
        void testCrear_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            AtraccionRequestDTO request = new AtraccionRequestDTO("Inflable Castillo", "Para niños", new BigDecimal("50.00"));
            when(atraccionService.crear(any(AtraccionRequestDTO.class)))
                    .thenThrow(new AccessDeniedException("No tiene permisos para crear atracciones"));

            // Act & Assert
            mockMvc.perform(post("/api/atracciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("GET /api/atracciones - Obtener atracciones")
    class ObtenerAtraccionesTests {

        @Test
        @DisplayName("200 OK - Obtiene todas las atracciones registradas")
        void testObtenerTodas_Ok() throws Exception {
            // Arrange
            when(atraccionService.obtenerTodas()).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/atracciones")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombre", is("Inflable Castillo")));

            verify(atraccionService, times(1)).obtenerTodas();
        }

        @Test
        @DisplayName("200 OK - Obtiene solo atracciones con estado ACTIVO")
        void testObtenerActivas_Ok() throws Exception {
            // Arrange
            when(atraccionService.obtenerActivas()).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/atracciones/activas")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].estado", is("ACTIVO")));

            verify(atraccionService, times(1)).obtenerActivas();
        }

        @Test
        @DisplayName("200 OK - Obtiene atracción por su ID")
        void testObtenerPorId_Ok() throws Exception {
            // Arrange
            when(atraccionService.obtenerPorId(1L)).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(get("/api/atracciones/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.nombre", is("Inflable Castillo")));

            verify(atraccionService, times(1)).obtenerPorId(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la atracción no existe")
        void testObtenerPorId_NoEncontrada_NotFound() throws Exception {
            // Arrange
            when(atraccionService.obtenerPorId(99L))
                    .thenThrow(new RecursoNoEncontradoException("Atracción con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(get("/api/atracciones/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)))
                    .andExpect(jsonPath("$.detalles", is("Atracción con ID 99 no encontrada")));
        }
    }

    @Nested
    @DisplayName("PUT /api/atracciones/{id} - Actualizar atracción")
    class ActualizarAtraccionTests {

        @Test
        @DisplayName("200 OK - Actualización exitosa de atracción (Happy Path)")
        void testActualizar_Exito_Ok() throws Exception {
            // Arrange
            AtraccionRequestDTO request = new AtraccionRequestDTO("Inflable Renovado", "Nueva descripción", new BigDecimal("60.00"));
            AtraccionResponseDTO actualizado = new AtraccionResponseDTO(
                    1L, "Inflable Renovado", "Nueva descripción", new BigDecimal("60.00"), "ACTIVO", LocalDateTime.now()
            );

            when(atraccionService.actualizar(eq(1L), any(AtraccionRequestDTO.class))).thenReturn(actualizado);

            // Act & Assert
            mockMvc.perform(put("/api/atracciones/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre", is("Inflable Renovado")))
                    .andExpect(jsonPath("$.tarifaMinuto", is(60.00)));

            verify(atraccionService, times(1)).actualizar(eq(1L), any(AtraccionRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por campos inválidos en actualización")
        void testActualizar_DatosInvalidos_BadRequest() throws Exception {
            // Arrange
            AtraccionRequestDTO request = new AtraccionRequestDTO("", "Descripción", new BigDecimal("-1.00"));

            // Act & Assert
            mockMvc.perform(put("/api/atracciones/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.nombre").exists())
                    .andExpect(jsonPath("$.errors.tarifaMinuto").exists());

            verify(atraccionService, never()).actualizar(any(), any());
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la atracción a actualizar no existe")
        void testActualizar_NoEncontrada_NotFound() throws Exception {
            // Arrange
            AtraccionRequestDTO request = new AtraccionRequestDTO("Inflable", "Desc", new BigDecimal("50.00"));
            when(atraccionService.actualizar(eq(99L), any(AtraccionRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Atracción con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(put("/api/atracciones/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/atracciones/{id}/estado - Cambiar estado de atracción")
    class CambiarEstadoTests {

        @Test
        @DisplayName("200 OK - Cambio de estado a MANTENIMIENTO exitoso (Happy Path)")
        void testCambiarEstado_Exito_Ok() throws Exception {
            // Arrange
            AtraccionResponseDTO enMantenimiento = new AtraccionResponseDTO(
                    1L, "Inflable Castillo", "Para niños", new BigDecimal("50.00"), "MANTENIMIENTO", LocalDateTime.now()
            );

            when(atraccionService.cambiarEstado(1L, "MANTENIMIENTO")).thenReturn(enMantenimiento);

            // Act & Assert
            mockMvc.perform(patch("/api/atracciones/1/estado")
                            .param("estado", "MANTENIMIENTO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.estado", is("MANTENIMIENTO")));

            verify(atraccionService, times(1)).cambiarEstado(1L, "MANTENIMIENTO");
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando el estado proporcionado es inválido")
        void testCambiarEstado_EstadoInvalido_Conflict() throws Exception {
            // Arrange
            when(atraccionService.cambiarEstado(1L, "ESTADO_DESCONOCIDO"))
                    .thenThrow(new ConflictoException("Estado inválido: ESTADO_DESCONOCIDO"));

            // Act & Assert
            mockMvc.perform(patch("/api/atracciones/1/estado")
                            .param("estado", "ESTADO_DESCONOCIDO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("Estado inválido: ESTADO_DESCONOCIDO")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/atracciones/{id} - Eliminar o inactivar atracción")
    class EliminarAtraccionTests {

        @Test
        @DisplayName("204 No Content - Eliminación exitosa de atracción (Happy Path)")
        void testEliminar_Exito_NoContent() throws Exception {
            // Arrange
            doNothing().when(atraccionService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/atracciones/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(atraccionService, times(1)).eliminar(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la atracción a eliminar no existe")
        void testEliminar_NoEncontrada_NotFound() throws Exception {
            // Arrange
            doThrow(new RecursoNoEncontradoException("Atracción con ID 99 no encontrada"))
                    .when(atraccionService).eliminar(99L);

            // Act & Assert
            mockMvc.perform(delete("/api/atracciones/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el usuario no tiene permisos")
        void testEliminar_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("No tiene permisos para eliminar atracciones"))
                    .when(atraccionService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/atracciones/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }
}
