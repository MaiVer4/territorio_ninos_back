package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.ConfiguracionParqueService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfiguracionParqueController - Pruebas de integración Web con MockMvc")
class ConfiguracionParqueControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ConfiguracionParqueService configuracionParqueService;

    @InjectMocks
    private ConfiguracionParqueController configuracionParqueController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(configuracionParqueController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/configuracion - Obtener configuración general")
    class ObtenerConfiguracionEndpointTests {

        @Test
        @DisplayName("200 OK - Obtiene la configuración exitosamente (Happy Path)")
        void testObtenerConfiguracion_Ok() throws Exception {
            // Arrange
            ConfiguracionParqueDTO dto = ConfiguracionParqueDTO.builder()
                    .id(1L)
                    .nombreParque("Territorio de Niños")
                    .nit("900.123.456-7")
                    .direccion("Centro Comercial Parque Central, Local 201")
                    .telefono("+57 300 123 4567")
                    .pieTicketLegal("Boleto válido únicamente para el día de emisión.")
                    .tiempoGraciaGeneralSegundos(60)
                    .build();

            when(configuracionParqueService.obtenerConfiguracion()).thenReturn(dto);

            // Act & Assert
            mockMvc.perform(get("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.nombreParque", is("Territorio de Niños")))
                    .andExpect(jsonPath("$.nit", is("900.123.456-7")))
                    .andExpect(jsonPath("$.direccion", is("Centro Comercial Parque Central, Local 201")))
                    .andExpect(jsonPath("$.telefono", is("+57 300 123 4567")))
                    .andExpect(jsonPath("$.pieTicketLegal", is("Boleto válido únicamente para el día de emisión.")))
                    .andExpect(jsonPath("$.tiempoGraciaGeneralSegundos", is(60)));

            verify(configuracionParqueService, times(1)).obtenerConfiguracion();
        }

        @Test
        @DisplayName("404 Not Found - Retorna error cuando la configuración no es encontrada")
        void testObtenerConfiguracion_NoEncontrada() throws Exception {
            // Arrange
            when(configuracionParqueService.obtenerConfiguracion())
                    .thenThrow(new RecursoNoEncontradoException("Configuración del parque no encontrada"));

            // Act & Assert
            mockMvc.perform(get("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)))
                    .andExpect(jsonPath("$.mensaje", is("Recurso no encontrado")))
                    .andExpect(jsonPath("$.detalles", is("Configuración del parque no encontrada")));

            verify(configuracionParqueService, times(1)).obtenerConfiguracion();
        }

        @Test
        @DisplayName("403 Forbidden - Manejo de denegación de acceso si el usuario carece de permisos")
        void testObtenerConfiguracion_AccesoDenegado() throws Exception {
            // Arrange
            when(configuracionParqueService.obtenerConfiguracion())
                    .thenThrow(new AccessDeniedException("No autorizado para ver la configuración"));

            // Act & Assert
            mockMvc.perform(get("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)))
                    .andExpect(jsonPath("$.mensaje", is("Acceso denegado")))
                    .andExpect(jsonPath("$.detalles", is("No autorizado para ver la configuración")));

            verify(configuracionParqueService, times(1)).obtenerConfiguracion();
        }
    }

    @Nested
    @DisplayName("PUT /api/configuracion - Actualizar configuración general")
    class ActualizarConfiguracionEndpointTests {

        @Test
        @DisplayName("200 OK - Actualiza la configuración exitosamente (Happy Path)")
        void testActualizarConfiguracion_Ok() throws Exception {
            // Arrange
            ConfiguracionParqueDTO requestDto = ConfiguracionParqueDTO.builder()
                    .nombreParque("Territorio Actualizado")
                    .nit("900.123.456-7")
                    .direccion("Calle 100 # 15-20")
                    .telefono("+57 301 987 6543")
                    .pieTicketLegal("Nuevo pie legal")
                    .tiempoGraciaGeneralSegundos(120)
                    .build();

            ConfiguracionParqueDTO responseDto = ConfiguracionParqueDTO.builder()
                    .id(1L)
                    .nombreParque("Territorio Actualizado")
                    .nit("900.123.456-7")
                    .direccion("Calle 100 # 15-20")
                    .telefono("+57 301 987 6543")
                    .pieTicketLegal("Nuevo pie legal")
                    .tiempoGraciaGeneralSegundos(120)
                    .build();

            when(configuracionParqueService.actualizarConfiguracion(any(ConfiguracionParqueDTO.class)))
                    .thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(put("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.nombreParque", is("Territorio Actualizado")))
                    .andExpect(jsonPath("$.nit", is("900.123.456-7")))
                    .andExpect(jsonPath("$.direccion", is("Calle 100 # 15-20")))
                    .andExpect(jsonPath("$.tiempoGraciaGeneralSegundos", is(120)));

            verify(configuracionParqueService, times(1)).actualizarConfiguracion(any(ConfiguracionParqueDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por nombre de parque en blanco")
        void testActualizarConfiguracion_NombreVacio_BadRequest() throws Exception {
            // Arrange: DTO con nombre en blanco
            ConfiguracionParqueDTO requestDto = ConfiguracionParqueDTO.builder()
                    .nombreParque("")
                    .nit("900.123.456-7")
                    .tiempoGraciaGeneralSegundos(60)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Error de validación")))
                    .andExpect(jsonPath("$.errors.nombreParque").value("El nombre del parque es obligatorio"));

            verify(configuracionParqueService, never()).actualizarConfiguracion(any());
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por NIT en blanco")
        void testActualizarConfiguracion_NitVacio_BadRequest() throws Exception {
            // Arrange: DTO con NIT en blanco
            ConfiguracionParqueDTO requestDto = ConfiguracionParqueDTO.builder()
                    .nombreParque("Territorio de Niños")
                    .nit("")
                    .tiempoGraciaGeneralSegundos(60)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Error de validación")))
                    .andExpect(jsonPath("$.errors.nit").value("El NIT es obligatorio"));

            verify(configuracionParqueService, never()).actualizarConfiguracion(any());
        }

        @Test
        @DisplayName("400 Bad Request - Falla de validación por tiempo de gracia nulo")
        void testActualizarConfiguracion_TiempoGraciaNulo_BadRequest() throws Exception {
            // Arrange: DTO con tiempo de gracia nulo
            ConfiguracionParqueDTO requestDto = ConfiguracionParqueDTO.builder()
                    .nombreParque("Territorio de Niños")
                    .nit("900.123.456-7")
                    .tiempoGraciaGeneralSegundos(null)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Error de validación")))
                    .andExpect(jsonPath("$.errors.tiempoGraciaGeneralSegundos").value("El tiempo de gracia en segundos es obligatorio"));

            verify(configuracionParqueService, never()).actualizarConfiguracion(any());
        }

        @Test
        @DisplayName("403 Forbidden - Manejo de denegación de permisos al intentar actualizar")
        void testActualizarConfiguracion_AccesoDenegado_Forbidden() throws Exception {
            // Arrange
            ConfiguracionParqueDTO requestDto = ConfiguracionParqueDTO.builder()
                    .nombreParque("Territorio de Niños")
                    .nit("900.123.456-7")
                    .tiempoGraciaGeneralSegundos(60)
                    .build();

            when(configuracionParqueService.actualizarConfiguracion(any(ConfiguracionParqueDTO.class)))
                    .thenThrow(new AccessDeniedException("Solo un administrador puede modificar la configuración"));

            // Act & Assert
            mockMvc.perform(put("/api/configuracion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo", is(403)))
                    .andExpect(jsonPath("$.mensaje", is("Acceso denegado")))
                    .andExpect(jsonPath("$.detalles", is("Solo un administrador puede modificar la configuración")));

            verify(configuracionParqueService, times(1)).actualizarConfiguracion(any(ConfiguracionParqueDTO.class));
        }
    }
}
