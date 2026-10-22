package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.ClienteService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteControllerTest - Pruebas de integración Web con MockMvc")
class ClienteControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteResponseDTO mockClienteResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MenorDTO menor = new MenorDTO(10L, "TI12345", "Mateo Gómez", LocalDate.of(2018, 5, 10), 6);

        mockClienteResponse = new ClienteResponseDTO(
                1L,
                "1098765432",
                "Carlos Andrés",
                "Gómez Pérez",
                "3001234567",
                "carlos.gomez@example.com",
                LocalDateTime.now(),
                List.of(menor)
        );
    }

    @Nested
    @DisplayName("GET /api/clientes/buscar")
    class BuscarClienteTests {

        @Test
        @DisplayName("200 OK cuando el cliente existe")
        void testBuscarCliente_Exitoso() throws Exception {
            when(clienteService.buscarPorDocumento("1098765432")).thenReturn(mockClienteResponse);

            mockMvc.perform(get("/api/clientes/buscar")
                            .param("documento", "1098765432")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.nombres", is("Carlos Andrés")))
                    .andExpect(jsonPath("$.apellidos", is("Gómez Pérez")))
                    .andExpect(jsonPath("$.nombreCompleto", is("Carlos Andrés Gómez Pérez")))
                    .andExpect(jsonPath("$.correo", is("carlos.gomez@example.com")))
                    .andExpect(jsonPath("$.menores.length()", is(1)))
                    .andExpect(jsonPath("$.menores[0].nombreCompleto", is("Mateo Gómez")));

            verify(clienteService).buscarPorDocumento("1098765432");
        }

        @Test
        @DisplayName("404 Not Found cuando el cliente no existe")
        void testBuscarCliente_NoEncontrado() throws Exception {
            when(clienteService.buscarPorDocumento("999999999"))
                    .thenThrow(new RecursoNoEncontradoException("No se encontró ningún cliente registrado con el documento: 999999999"));

            mockMvc.perform(get("/api/clientes/buscar")
                            .param("documento", "999999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is(404)))
                    .andExpect(jsonPath("$.detalles").value("No se encontró ningún cliente registrado con el documento: 999999999"));

            verify(clienteService).buscarPorDocumento("999999999");
        }
    }

    @Nested
    @DisplayName("POST /api/clientes")
    class RegistrarClienteTests {

        @Test
        @DisplayName("201 Created cuando el cliente se registra exitosamente")
        void testRegistrarCliente_Exitoso() throws Exception {
            MenorDTO menorDTO = new MenorDTO(null, "TI12345", "Mateo Gómez", LocalDate.of(2018, 5, 10), 6);
            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos Andrés",
                    "Gómez Pérez",
                    "3001234567",
                    "carlos.gomez@example.com",
                    List.of(menorDTO)
            );

            when(clienteService.registrarCliente(any(ClienteRequestDTO.class))).thenReturn(mockClienteResponse);

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.nombreCompleto", is("Carlos Andrés Gómez Pérez")))
                    .andExpect(jsonPath("$.menores[0].nombreCompleto", is("Mateo Gómez")));

            verify(clienteService).registrarCliente(any(ClienteRequestDTO.class));
        }

        @Test
        @DisplayName("409 Conflict cuando el documento ya existe")
        void testRegistrarCliente_ConflictoDuplicado() throws Exception {
            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos Andrés",
                    "Gómez Pérez",
                    "3001234567",
                    "carlos.gomez@example.com",
                    null
            );

            when(clienteService.registrarCliente(any(ClienteRequestDTO.class)))
                    .thenThrow(new ConflictoException("Ya existe un cliente registrado con el número de documento: 1098765432"));

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is(409)))
                    .andExpect(jsonPath("$.detalles").value("Ya existe un cliente registrado con el número de documento: 1098765432"));

            verify(clienteService).registrarCliente(any(ClienteRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request cuando faltan campos requeridos o son inválidos")
        void testRegistrarCliente_ValidacionFallida() throws Exception {
            // Nombres en blanco y formato de correo inválido
            ClienteRequestDTO requestInvalido = new ClienteRequestDTO(
                    "",
                    "",
                    "",
                    "3001234567",
                    "correo-no-valido",
                    null
            );

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.numeroDocumento").exists())
                    .andExpect(jsonPath("$.errors.nombres").exists())
                    .andExpect(jsonPath("$.errors.apellidos").exists())
                    .andExpect(jsonPath("$.errors.correo").exists());

            verify(clienteService, never()).registrarCliente(any());
        }
    }

    @Nested
    @DisplayName("GET /api/clientes/documento/{documento}")
    class BuscarClientePorPathTests {

        @Test
        @DisplayName("200 OK cuando el cliente existe por ruta")
        void testBuscarClientePorPath_Exitoso() throws Exception {
            when(clienteService.buscarPorDocumento("1098765432")).thenReturn(mockClienteResponse);

            mockMvc.perform(get("/api/clientes/documento/1098765432")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.nombreCompleto", is("Carlos Andrés Gómez Pérez")));

            verify(clienteService).buscarPorDocumento("1098765432");
        }
    }

    @Nested
    @DisplayName("PUT /api/clientes/{id}")
    class ActualizarClienteTests {

        @Test
        @DisplayName("200 OK cuando se actualiza cliente exitosamente")
        void testActualizarCliente_Exitoso() throws Exception {
            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3119876543",
                    "nuevo.correo@example.com",
                    null
            );

            when(clienteService.actualizarCliente(eq(1L), any(ClienteRequestDTO.class))).thenReturn(mockClienteResponse);

            mockMvc.perform(put("/api/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)));

            verify(clienteService).actualizarCliente(eq(1L), any(ClienteRequestDTO.class));
        }
    }
}
