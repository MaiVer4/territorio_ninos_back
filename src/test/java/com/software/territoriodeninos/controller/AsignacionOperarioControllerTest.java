package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.AsignacionOperarioService;
import com.software.territoriodeninos.util.EstadoAsignacion;
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

import java.time.LocalDate;
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
@DisplayName("AsignacionOperarioController - Pruebas de integración Web con MockMvc")
class AsignacionOperarioControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AsignacionOperarioService asignacionOperarioService;

    @InjectMocks
    private AsignacionOperarioController asignacionOperarioController;

    private AsignacionResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(asignacionOperarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockResponseDTO = new AsignacionResponseDTO(
                10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                LocalDate.now(), LocalDateTime.now(), null, EstadoAsignacion.ACTIVA, "Turno tarde"
        );
    }

    @Nested
    @DisplayName("POST /api/asignaciones - Asignar operario a atracción")
    class AsignarOperarioTests {

        @Test
        @DisplayName("201 Created - Asignación exitosa de operario (Happy Path)")
        void testAsignar_Exito_Created() throws Exception {
            // Arrange
            AsignacionRequestDTO request = new AsignacionRequestDTO(5L, 1L, "Turno tarde");
            when(asignacionOperarioService.asignar(any(AsignacionRequestDTO.class))).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.usuarioId", is(5)))
                    .andExpect(jsonPath("$.operarioId", is(5)))
                    .andExpect(jsonPath("$.nombreOperario", is("Juan Perez")))
                    .andExpect(jsonPath("$.operarioNombre", is("Juan Perez")))
                    .andExpect(jsonPath("$.operarioCorreo", is("juan@test.com")))
                    .andExpect(jsonPath("$.nombreAtraccion", is("Inflable")))
                    .andExpect(jsonPath("$.atraccionNombre", is("Inflable")))
                    .andExpect(jsonPath("$.estado", is("ACTIVA")))
                    .andExpect(jsonPath("$.activo", is(true)));

            verify(asignacionOperarioService, times(1)).asignar(any(AsignacionRequestDTO.class));
        }

        @Test
        @DisplayName("201 Created - Deserialización de alias frontend (operarioId) y forzarReasignacion")
        void testAsignar_ConAliasFrontendYForzarReasignacion_Exito() throws Exception {
            // Arrange
            String jsonPayload = """
                    {
                        "operarioId": 5,
                        "atraccionId": 1,
                        "forzarReasignacion": true,
                        "observaciones": "Reubicación rápida"
                    }
                    """;
            when(asignacionOperarioService.asignar(argThat(dto ->
                    dto.usuarioId().equals(5L) &&
                    dto.atraccionId().equals(1L) &&
                    Boolean.TRUE.equals(dto.forzarReasignacion())
            ))).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.operarioId", is(5)))
                    .andExpect(jsonPath("$.activo", is(true)));

            verify(asignacionOperarioService, times(1)).asignar(any(AsignacionRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por IDs nulos")
        void testAsignar_CamposNulos_BadRequest() throws Exception {
            // Arrange
            AsignacionRequestDTO request = new AsignacionRequestDTO(null, null, "Turno");

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.usuarioId").exists())
                    .andExpect(jsonPath("$.errors.atraccionId").exists());

            verify(asignacionOperarioService, never()).asignar(any());
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando el operario ya tiene asignación activa")
        void testAsignar_OperarioYaAsignado_Conflict() throws Exception {
            // Arrange
            AsignacionRequestDTO request = new AsignacionRequestDTO(5L, 1L, "Turno tarde");
            when(asignacionOperarioService.asignar(any(AsignacionRequestDTO.class)))
                    .thenThrow(new ConflictoException("El operario ya tiene una asignación activa"));

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("El operario ya tiene una asignación activa")));
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el operario o la atracción no existen")
        void testAsignar_NoEncontrado_NotFound() throws Exception {
            // Arrange
            AsignacionRequestDTO request = new AsignacionRequestDTO(99L, 1L, "Turno tarde");
            when(asignacionOperarioService.asignar(any(AsignacionRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso si el usuario no tiene permisos")
        void testAsignar_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            AsignacionRequestDTO request = new AsignacionRequestDTO(5L, 1L, "Turno tarde");
            when(asignacionOperarioService.asignar(any(AsignacionRequestDTO.class)))
                    .thenThrow(new AccessDeniedException("No tiene permisos para asignar operarios"));

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("POST /api/asignaciones/reasignar - Reasignar operario en caliente")
    class ReasignarOperarioTests {

        @Test
        @DisplayName("200 OK - Reasignación exitosa de operario (Happy Path)")
        void testReasignar_Exito_Ok() throws Exception {
            // Arrange
            ReasignacionRequestDTO request = new ReasignacionRequestDTO(5L, 2L, "Rotación");
            AsignacionResponseDTO reasignada = new AsignacionResponseDTO(
                    11L, 5L, "Juan Perez", "juan@test.com", 2L, "Toro Mecánico",
                    LocalDate.now(), LocalDateTime.now(), null, EstadoAsignacion.ACTIVA, "Reasignado"
            );

            when(asignacionOperarioService.reasignar(any(ReasignacionRequestDTO.class))).thenReturn(reasignada);

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones/reasignar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombreAtraccion", is("Toro Mecánico")));

            verify(asignacionOperarioService, times(1)).reasignar(any(ReasignacionRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por nuevaAtraccionId nulo")
        void testReasignar_AtraccionNula_BadRequest() throws Exception {
            // Arrange
            ReasignacionRequestDTO request = new ReasignacionRequestDTO(5L, null, "Rotación");

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones/reasignar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.nuevaAtraccionId").exists());

            verify(asignacionOperarioService, never()).reasignar(any());
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el operario no tiene asignación activa para reasignar")
        void testReasignar_SinAsignacionActiva_NotFound() throws Exception {
            // Arrange
            ReasignacionRequestDTO request = new ReasignacionRequestDTO(5L, 2L, "Rotación");
            when(asignacionOperarioService.reasignar(any(ReasignacionRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("No se encontró asignación activa para el operario"));

            // Act & Assert
            mockMvc.perform(post("/api/asignaciones/reasignar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/asignaciones/{id}/finalizar - Finalizar asignación")
    class FinalizarAsignacionTests {

        @Test
        @DisplayName("200 OK - Finalización exitosa de asignación (Happy Path)")
        void testFinalizar_Exito_Ok() throws Exception {
            // Arrange
            AsignacionResponseDTO finalizada = new AsignacionResponseDTO(
                    10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                    LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), EstadoAsignacion.FINALIZADA, "Turno finalizado"
            );

            when(asignacionOperarioService.finalizar(10L)).thenReturn(finalizada);

            // Act & Assert
            mockMvc.perform(patch("/api/asignaciones/10/finalizar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado", is("FINALIZADA")));

            verify(asignacionOperarioService, times(1)).finalizar(10L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la asignación no existe")
        void testFinalizar_NoExiste_NotFound() throws Exception {
            // Arrange
            when(asignacionOperarioService.finalizar(99L))
                    .thenThrow(new RecursoNoEncontradoException("Asignación con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(patch("/api/asignaciones/99/finalizar")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("200 OK - Liberación/Desasignación exitosa mediante DELETE /{id}")
        void testLiberar_Exito_Ok() throws Exception {
            // Arrange
            AsignacionResponseDTO finalizada = new AsignacionResponseDTO(
                    10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                    LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), EstadoAsignacion.FINALIZADA, "Turno finalizado"
            );

            when(asignacionOperarioService.finalizar(10L)).thenReturn(finalizada);

            // Act & Assert
            mockMvc.perform(delete("/api/asignaciones/10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.estado", is("FINALIZADA")))
                    .andExpect(jsonPath("$.activo", is(false)));

            verify(asignacionOperarioService, times(1)).finalizar(10L);
        }

        @Test
        @DisplayName("404 Not Found - Error al liberar con DELETE /{id} cuando no existe")
        void testLiberar_NoExiste_NotFound() throws Exception {
            // Arrange
            when(asignacionOperarioService.finalizar(99L))
                    .thenThrow(new RecursoNoEncontradoException("Asignación con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(delete("/api/asignaciones/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));

            verify(asignacionOperarioService, times(1)).finalizar(99L);
        }

        @Test
        @DisplayName("200 OK - Liberación mediante DELETE en ruta alternativa /api/assignments/{id}")
        void testLiberar_RutaIngles_Ok() throws Exception {
            // Arrange
            AsignacionResponseDTO finalizada = new AsignacionResponseDTO(
                    10L, 5L, "Juan Perez", "juan@test.com", 1L, "Inflable",
                    LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), EstadoAsignacion.FINALIZADA, "Turno finalizado"
            );

            when(asignacionOperarioService.finalizar(10L)).thenReturn(finalizada);

            // Act & Assert
            mockMvc.perform(delete("/api/assignments/10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.estado", is("FINALIZADA")));

            verify(asignacionOperarioService, times(1)).finalizar(10L);
        }
    }

    @Nested
    @DisplayName("GET /api/asignaciones - Consultas de asignaciones")
    class ConsultasAsignacionesTests {

        @Test
        @DisplayName("200 OK - Obtiene lista de asignaciones activas")
        void testObtenerActivas_Ok() throws Exception {
            // Arrange
            when(asignacionOperarioService.obtenerActivas()).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/asignaciones/activas")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombreOperario", is("Juan Perez")));

            verify(asignacionOperarioService, times(1)).obtenerActivas();
        }

        @Test
        @DisplayName("200 OK - Obtiene asignación activa de un operario específico")
        void testObtenerActivaPorOperario_Ok() throws Exception {
            // Arrange
            when(asignacionOperarioService.obtenerActivaPorOperario(5L)).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(get("/api/asignaciones/operario/5/activa")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuarioId", is(5)));

            verify(asignacionOperarioService, times(1)).obtenerActivaPorOperario(5L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando el operario no tiene asignación activa")
        void testObtenerActivaPorOperario_NoExiste_NotFound() throws Exception {
            // Arrange
            when(asignacionOperarioService.obtenerActivaPorOperario(99L))
                    .thenThrow(new RecursoNoEncontradoException("No hay asignación activa para el operario"));

            // Act & Assert
            mockMvc.perform(get("/api/asignaciones/operario/99/activa")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("200 OK - Obtiene asignaciones activas de una atracción")
        void testObtenerActivasPorAtraccion_Ok() throws Exception {
            // Arrange
            when(asignacionOperarioService.obtenerActivasPorAtraccion(1L)).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/asignaciones/atraccion/1/activas")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(asignacionOperarioService, times(1)).obtenerActivasPorAtraccion(1L);
        }
    }
}
