package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.TarifaTiempoService;
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
@DisplayName("TarifaTiempoController - Pruebas de integración Web con MockMvc")
class TarifaTiempoControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TarifaTiempoService tarifaTiempoService;

    @InjectMocks
    private TarifaTiempoController tarifaTiempoController;

    private TarifaTiempoResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tarifaTiempoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockResponseDTO = new TarifaTiempoResponseDTO(
                1L, 1L, "Inflable", 15, new BigDecimal("5000.00"), true, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/tarifas-tiempo - Crear tarifa por tiempo")
    class CrearTarifaTiempoTests {

        @Test
        @DisplayName("201 Created - Creación exitosa de tarifa por tiempo (Happy Path)")
        void testCrear_Exito_Created() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, 15, new BigDecimal("5000.00"), true);
            when(tarifaTiempoService.crear(any(TarifaTiempoRequestDTO.class))).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-tiempo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.duracionMinutos", is(15)))
                    .andExpect(jsonPath("$.precio", is(5000.00)));

            verify(tarifaTiempoService, times(1)).crear(any(TarifaTiempoRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por campos obligatorios faltantes o precio <= 0")
        void testCrear_DatosInvalidos_BadRequest() throws Exception {
            // Arrange: precio en 0 y duración menor a 1
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(null, 0, new BigDecimal("0.00"), true);

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-tiempo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.atraccionId").exists())
                    .andExpect(jsonPath("$.errors.duracionMinutos").exists())
                    .andExpect(jsonPath("$.errors.precio").exists());

            verify(tarifaTiempoService, never()).crear(any());
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando ya existe una tarifa idéntica para la atracción")
        void testCrear_Duplicado_Conflict() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, 15, new BigDecimal("5000.00"), true);
            when(tarifaTiempoService.crear(any(TarifaTiempoRequestDTO.class)))
                    .thenThrow(new ConflictoException("Ya existe una tarifa de 15 minutos para esta atracción"));

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-tiempo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("Ya existe una tarifa de 15 minutos para esta atracción")));
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la atracción asociada no existe")
        void testCrear_AtraccionNoExiste_NotFound() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(99L, 15, new BigDecimal("5000.00"), true);
            when(tarifaTiempoService.crear(any(TarifaTiempoRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Atracción con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-tiempo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el usuario no tiene rol administrador")
        void testCrear_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, 15, new BigDecimal("5000.00"), true);
            when(tarifaTiempoService.crear(any(TarifaTiempoRequestDTO.class)))
                    .thenThrow(new AccessDeniedException("No tiene permisos para crear tarifas"));

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-tiempo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("GET /api/tarifas-tiempo - Consultas de tarifas por tiempo")
    class ConsultasTarifaTiempoTests {

        @Test
        @DisplayName("200 OK - Obtiene tarifas asociadas a una atracción específica")
        void testObtenerPorAtraccion_Ok() throws Exception {
            // Arrange
            when(tarifaTiempoService.obtenerPorAtraccion(1L)).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/tarifas-tiempo/atraccion/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].duracionMinutos", is(15)));

            verify(tarifaTiempoService, times(1)).obtenerPorAtraccion(1L);
        }

        @Test
        @DisplayName("200 OK - Obtiene tarifa específica por ID")
        void testObtenerPorId_Ok() throws Exception {
            // Arrange
            when(tarifaTiempoService.obtenerPorId(1L)).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(get("/api/tarifas-tiempo/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.duracionMinutos", is(15)));

            verify(tarifaTiempoService, times(1)).obtenerPorId(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la tarifa no existe")
        void testObtenerPorId_NoExiste_NotFound() throws Exception {
            // Arrange
            when(tarifaTiempoService.obtenerPorId(99L))
                    .thenThrow(new RecursoNoEncontradoException("Tarifa con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(get("/api/tarifas-tiempo/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("PUT /api/tarifas-tiempo/{id} - Actualizar tarifa por tiempo")
    class ActualizarTarifaTiempoTests {

        @Test
        @DisplayName("200 OK - Actualización de tarifa exitosa (Happy Path)")
        void testActualizar_Exito_Ok() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, 30, new BigDecimal("8000.00"), true);
            TarifaTiempoResponseDTO actualizado = new TarifaTiempoResponseDTO(
                    1L, 1L, "Inflable", 30, new BigDecimal("8000.00"), true, LocalDateTime.now()
            );

            when(tarifaTiempoService.actualizar(eq(1L), any(TarifaTiempoRequestDTO.class))).thenReturn(actualizado);

            // Act & Assert
            mockMvc.perform(put("/api/tarifas-tiempo/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.duracionMinutos", is(30)))
                    .andExpect(jsonPath("$.precio", is(8000.00)));

            verify(tarifaTiempoService, times(1)).actualizar(eq(1L), any(TarifaTiempoRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por duración negativa")
        void testActualizar_DuracionInvalida_BadRequest() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, -5, new BigDecimal("8000.00"), true);

            // Act & Assert
            mockMvc.perform(put("/api/tarifas-tiempo/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.duracionMinutos").exists());

            verify(tarifaTiempoService, never()).actualizar(any(), any());
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la tarifa a actualizar no existe")
        void testActualizar_NoExiste_NotFound() throws Exception {
            // Arrange
            TarifaTiempoRequestDTO request = new TarifaTiempoRequestDTO(1L, 30, new BigDecimal("8000.00"), true);
            when(tarifaTiempoService.actualizar(eq(99L), any(TarifaTiempoRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Tarifa con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(put("/api/tarifas-tiempo/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/tarifas-tiempo/{id} - Eliminar tarifa por tiempo")
    class EliminarTarifaTiempoTests {

        @Test
        @DisplayName("204 No Content - Eliminación exitosa de tarifa (Happy Path)")
        void testEliminar_Exito_NoContent() throws Exception {
            // Arrange
            doNothing().when(tarifaTiempoService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/tarifas-tiempo/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(tarifaTiempoService, times(1)).eliminar(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la tarifa a eliminar no existe")
        void testEliminar_NoExiste_NotFound() throws Exception {
            // Arrange
            doThrow(new RecursoNoEncontradoException("Tarifa con ID 99 no encontrada"))
                    .when(tarifaTiempoService).eliminar(99L);

            // Act & Assert
            mockMvc.perform(delete("/api/tarifas-tiempo/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el usuario no tiene permisos")
        void testEliminar_SinPermisos_Forbidden() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("No tiene permisos para eliminar tarifas"))
                    .when(tarifaTiempoService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/tarifas-tiempo/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }
}
