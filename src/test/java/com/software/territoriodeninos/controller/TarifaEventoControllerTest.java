package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.TarifaEventoService;
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
@DisplayName("TarifaEventoController - Pruebas de integración Web con MockMvc")
class TarifaEventoControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TarifaEventoService tarifaEventoService;

    @InjectMocks
    private TarifaEventoController tarifaEventoController;

    private TarifaEventoResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tarifaEventoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockResponseDTO = new TarifaEventoResponseDTO(
                1L, 2L, "Toro Mecánico", 1, new BigDecimal("6000.00"), true, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/tarifas-evento - Crear tarifa por evento")
    class CrearTarifaEventoTests {

        @Test
        @DisplayName("201 Created - Creación exitosa de tarifa por evento (Happy Path)")
        void testCrear_Exito_Created() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 1, new BigDecimal("6000.00"), true);
            when(tarifaEventoService.crear(any(TarifaEventoRequestDTO.class))).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-evento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.intentos", is(1)))
                    .andExpect(jsonPath("$.precio", is(6000.00)));

            verify(tarifaEventoService, times(1)).crear(any(TarifaEventoRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por campos obligatorios faltantes o precio <= 0")
        void testCrear_DatosInvalidos_BadRequest() throws Exception {
            // Arrange: precio en 0 e intentos en 0
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(null, 0, new BigDecimal("0.00"), true);

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-evento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.atraccionId").exists())
                    .andExpect(jsonPath("$.errors.intentos").exists())
                    .andExpect(jsonPath("$.errors.precio").exists());

            verify(tarifaEventoService, never()).crear(any());
        }

        @Test
        @DisplayName("409 Conflict - Retorna conflicto cuando ya existe una tarifa idéntica para la atracción")
        void testCrear_Duplicado_Conflict() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 1, new BigDecimal("6000.00"), true);
            when(tarifaEventoService.crear(any(TarifaEventoRequestDTO.class)))
                    .thenThrow(new ConflictoException("Ya existe una tarifa de 1 intentos para esta atracción"));

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-evento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles", is("Ya existe una tarifa de 1 intentos para esta atracción")));
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la atracción asociada no existe")
        void testCrear_AtraccionNoExiste_NotFound() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(99L, 1, new BigDecimal("6000.00"), true);
            when(tarifaEventoService.crear(any(TarifaEventoRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Atracción con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-evento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el usuario no tiene rol administrador")
        void testCrear_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 1, new BigDecimal("6000.00"), true);
            when(tarifaEventoService.crear(any(TarifaEventoRequestDTO.class)))
                    .thenThrow(new AccessDeniedException("No tiene permisos para crear tarifas"));

            // Act & Assert
            mockMvc.perform(post("/api/tarifas-evento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }

    @Nested
    @DisplayName("GET /api/tarifas-evento - Consultas de tarifas por evento")
    class ConsultasTarifaEventoTests {

        @Test
        @DisplayName("200 OK - Obtiene tarifas asociadas a una atracción específica")
        void testObtenerPorAtraccion_Ok() throws Exception {
            // Arrange
            when(tarifaEventoService.obtenerPorAtraccion(2L)).thenReturn(List.of(mockResponseDTO));

            // Act & Assert
            mockMvc.perform(get("/api/tarifas-evento/atraccion/2")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].intentos", is(1)));

            verify(tarifaEventoService, times(1)).obtenerPorAtraccion(2L);
        }

        @Test
        @DisplayName("200 OK - Obtiene tarifa específica por ID")
        void testObtenerPorId_Ok() throws Exception {
            // Arrange
            when(tarifaEventoService.obtenerPorId(1L)).thenReturn(mockResponseDTO);

            // Act & Assert
            mockMvc.perform(get("/api/tarifas-evento/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.intentos", is(1)));

            verify(tarifaEventoService, times(1)).obtenerPorId(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la tarifa no existe")
        void testObtenerPorId_NoExiste_NotFound() throws Exception {
            // Arrange
            when(tarifaEventoService.obtenerPorId(99L))
                    .thenThrow(new RecursoNoEncontradoException("Tarifa con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(get("/api/tarifas-evento/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("PUT /api/tarifas-evento/{id} - Actualizar tarifa por evento")
    class ActualizarTarifaEventoTests {

        @Test
        @DisplayName("200 OK - Actualización de tarifa exitosa (Happy Path)")
        void testActualizar_Exito_Ok() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 3, new BigDecimal("15000.00"), true);
            TarifaEventoResponseDTO actualizado = new TarifaEventoResponseDTO(
                    1L, 2L, "Toro Mecánico", 3, new BigDecimal("15000.00"), true, LocalDateTime.now()
            );

            when(tarifaEventoService.actualizar(eq(1L), any(TarifaEventoRequestDTO.class))).thenReturn(actualizado);

            // Act & Assert
            mockMvc.perform(put("/api/tarifas-evento/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.intentos", is(3)))
                    .andExpect(jsonPath("$.precio", is(15000.00)));

            verify(tarifaEventoService, times(1)).actualizar(eq(1L), any(TarifaEventoRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validación falla por número de intentos inválido")
        void testActualizar_IntentosInvalidos_BadRequest() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 0, new BigDecimal("10000.00"), true);

            // Act & Assert
            mockMvc.perform(put("/api/tarifas-evento/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.intentos").exists());

            verify(tarifaEventoService, never()).actualizar(any(), any());
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la tarifa a actualizar no existe")
        void testActualizar_NoExiste_NotFound() throws Exception {
            // Arrange
            TarifaEventoRequestDTO request = new TarifaEventoRequestDTO(2L, 3, new BigDecimal("15000.00"), true);
            when(tarifaEventoService.actualizar(eq(99L), any(TarifaEventoRequestDTO.class)))
                    .thenThrow(new RecursoNoEncontradoException("Tarifa con ID 99 no encontrada"));

            // Act & Assert
            mockMvc.perform(put("/api/tarifas-evento/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/tarifas-evento/{id} - Eliminar tarifa por evento")
    class EliminarTarifaEventoTests {

        @Test
        @DisplayName("204 No Content - Eliminación exitosa de tarifa (Happy Path)")
        void testEliminar_Exito_NoContent() throws Exception {
            // Arrange
            doNothing().when(tarifaEventoService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/tarifas-evento/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(tarifaEventoService, times(1)).eliminar(1L);
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la tarifa a eliminar no existe")
        void testEliminar_NoExiste_NotFound() throws Exception {
            // Arrange
            doThrow(new RecursoNoEncontradoException("Tarifa con ID 99 no encontrada"))
                    .when(tarifaEventoService).eliminar(99L);

            // Act & Assert
            mockMvc.perform(delete("/api/tarifas-evento/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)));
        }

        @Test
        @DisplayName("403 Forbidden - Denegación de acceso cuando el usuario no tiene permisos")
        void testEliminar_SinPermisos_Forbidden() throws Exception {
            // Arrange
            doThrow(new AccessDeniedException("No tiene permisos para eliminar tarifas"))
                    .when(tarifaEventoService).eliminar(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/tarifas-evento/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)));
        }
    }
}
