package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

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
@DisplayName("ClienteControllerTest - Pruebas MockMvc para Notion CA-03 y CA-04")
class ClienteControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteResponseDTO mockClienteConTresMenores;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        List<MenorDTO> tresMenores = List.of(
                new MenorDTO(10L, "TI101", "Lucas Gómez", LocalDate.of(2022, 1, 15), 4),
                new MenorDTO(11L, "TI102", "Martina Gómez", LocalDate.of(2018, 5, 10), 8),
                new MenorDTO(12L, "TI103", "Esteban Gómez", LocalDate.of(2011, 9, 20), 15)
        );

        mockClienteConTresMenores = new ClienteResponseDTO(
                1L,
                "1098765432",
                "Carlos Andrés",
                "Gómez Pérez",
                "3001234567",
                "carlos.gomez@example.com",
                LocalDateTime.now(),
                tresMenores
        );
    }

    @Nested
    @DisplayName("Notion CA-03 & CA-04: POST /api/clientes (Registro y vinculación de menores)")
    class RegistrarClienteTests {

        @Test
        @DisplayName("201 Created - Registro exitoso de acudiente con múltiples menores (edades válidas 4, 8, 15)")
        void testRegistrarCliente_ConMultiplesMenores_201Created() throws Exception {
            List<MenorDTO> menoresValidos = List.of(
                    new MenorDTO(null, "TI101", "Lucas Gómez", LocalDate.of(2022, 1, 15), 4),
                    new MenorDTO(null, "TI102", "Martina Gómez", LocalDate.of(2018, 5, 10), 8),
                    new MenorDTO(null, "TI103", "Esteban Gómez", LocalDate.of(2011, 9, 20), 15)
            );

            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos Andrés",
                    "Gómez Pérez",
                    "3001234567",
                    "carlos.gomez@example.com",
                    menoresValidos
            );

            when(clienteService.registrarCliente(any(ClienteRequestDTO.class)))
                    .thenReturn(mockClienteConTresMenores);

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.nombreCompleto", is("Carlos Andrés Gómez Pérez")))
                    .andExpect(jsonPath("$.menores.length()", is(3)))
                    .andExpect(jsonPath("$.menores[0].edad", is(4)))
                    .andExpect(jsonPath("$.menores[1].edad", is(8)))
                    .andExpect(jsonPath("$.menores[2].edad", is(15)));

            verify(clienteService).registrarCliente(any(ClienteRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Error de validación al registrar menor con edad < 1 (edad = 0 o negativa)")
        void testRegistrarCliente_ErrorEdadMenorAUno_400BadRequest() throws Exception {
            List<MenorDTO> menorConEdadCero = List.of(
                    new MenorDTO(null, "RC001", "Bebé Gómez", LocalDate.of(2026, 1, 1), 0)
            );

            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3001234567",
                    "carlos@example.com",
                    menorConEdadCero
            );

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(clienteService, never()).registrarCliente(any());
        }

        @Test
        @DisplayName("400 Bad Request - Error de validación al registrar menor con edad > 17 (edad = 18 o 25)")
        void testRegistrarCliente_ErrorEdadMayorADiecisiete_400BadRequest() throws Exception {
            List<MenorDTO> menorMayorDeEdad = List.of(
                    new MenorDTO(null, "CC1801", "Adulto Gómez", LocalDate.of(2005, 1, 1), 18)
            );

            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3001234567",
                    "carlos@example.com",
                    menorMayorDeEdad
            );

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(clienteService, never()).registrarCliente(any());
        }

        @Test
        @DisplayName("400 Bad Request - Error de validación cuando la edad del menor es nula")
        void testRegistrarCliente_ErrorEdadNula_400BadRequest() throws Exception {
            String jsonConEdadNula = """
                    {
                        "numeroDocumento": "1098765432",
                        "nombres": "Carlos",
                        "apellidos": "Gómez",
                        "telefono": "3001234567",
                        "correo": "carlos@example.com",
                        "menores": [
                            {
                                "numeroDocumento": "TI999",
                                "nombreCompleto": "Hijo Sin Edad",
                                "edad": null
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonConEdadNula))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(clienteService, never()).registrarCliente(any());
        }

        @Test
        @DisplayName("400 Bad Request - Error de negocio ReglaNegocioException desde el servicio")
        void testRegistrarCliente_ReglaNegocioException_400BadRequest() throws Exception {
            List<MenorDTO> menoresValidos = List.of(
                    new MenorDTO(null, "TI101", "Lucas Gómez", LocalDate.of(2022, 1, 15), 4)
            );

            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos Andrés",
                    "Gómez",
                    "3001234567",
                    "carlos@example.com",
                    menoresValidos
            );

            when(clienteService.registrarCliente(any(ClienteRequestDTO.class)))
                    .thenThrow(new ReglaNegocioException("La edad del menor debe estar comprendida entre 1 y 17 años."));

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is(400)))
                    .andExpect(jsonPath("$.detalles", is("La edad del menor debe estar comprendida entre 1 y 17 años.")));
        }

        @Test
        @DisplayName("201 Created - Notion CA-04: Vinculación de nuevos menores a acudiente existente vía POST")
        void testRegistrarCliente_VinculacionNuevosMenoresAAcudienteExistente_201Created() throws Exception {
            List<MenorDTO> nuevosMenores = List.of(
                    new MenorDTO(null, "TI555", "Nuevo Menor", LocalDate.of(2016, 3, 10), 10)
            );

            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3110009999",
                    "carlos.nuevo@example.com",
                    nuevosMenores
            );

            ClienteResponseDTO clienteActualizado = new ClienteResponseDTO(
                    1L,
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3110009999",
                    "carlos.nuevo@example.com",
                    LocalDateTime.now(),
                    nuevosMenores
            );

            when(clienteService.registrarCliente(any(ClienteRequestDTO.class)))
                    .thenReturn(clienteActualizado);

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.telefono", is("3110009999")))
                    .andExpect(jsonPath("$.menores.length()", is(1)))
                    .andExpect(jsonPath("$.menores[0].nombreCompleto", is("Nuevo Menor")));

            verify(clienteService).registrarCliente(any(ClienteRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request cuando faltan campos requeridos del acudiente")
        void testRegistrarCliente_ValidacionFallidaAcudiente() throws Exception {
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
    @DisplayName("GET /api/clientes/buscar y /api/clientes/documento/{documento}")
    class ConsultaClienteTests {

        @Test
        @DisplayName("200 OK - Consulta de cliente por documento con sus múltiples menores asociados")
        void testBuscarPorDocumento_ConMultiplesMenores_200Ok() throws Exception {
            when(clienteService.buscarPorDocumento("1098765432")).thenReturn(mockClienteConTresMenores);

            mockMvc.perform(get("/api/clientes/buscar")
                            .param("documento", "1098765432")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.nombreCompleto", is("Carlos Andrés Gómez Pérez")))
                    .andExpect(jsonPath("$.menores.length()", is(3)))
                    .andExpect(jsonPath("$.menores[0].nombreCompleto", is("Lucas Gómez")))
                    .andExpect(jsonPath("$.menores[1].nombreCompleto", is("Martina Gómez")))
                    .andExpect(jsonPath("$.menores[2].nombreCompleto", is("Esteban Gómez")));

            verify(clienteService).buscarPorDocumento("1098765432");
        }

        @Test
        @DisplayName("200 OK - Consulta de cliente por documento en Path Variable")
        void testBuscarPorDocumentoPath_Exitoso() throws Exception {
            when(clienteService.buscarPorDocumento("1098765432")).thenReturn(mockClienteConTresMenores);

            mockMvc.perform(get("/api/clientes/documento/1098765432")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.numeroDocumento", is("1098765432")))
                    .andExpect(jsonPath("$.menores.length()", is(3)));

            verify(clienteService).buscarPorDocumento("1098765432");
        }

        @Test
        @DisplayName("404 Not Found cuando el cliente no existe")
        void testBuscarPorDocumento_NoEncontrado() throws Exception {
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
    @DisplayName("PUT /api/clientes/{id}")
    class ActualizarClienteTests {

        @Test
        @DisplayName("200 OK - Actualización de cliente agregando menores")
        void testActualizarCliente_AgregandoMenores_200Ok() throws Exception {
            List<MenorDTO> menoresActualizados = List.of(
                    new MenorDTO(10L, "TI101", "Lucas Gómez", LocalDate.of(2022, 1, 15), 4),
                    new MenorDTO(null, "TI104", "Sofía Gómez", LocalDate.of(2017, 3, 1), 9)
            );

            ClienteRequestDTO request = new ClienteRequestDTO(
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3119876543",
                    "nuevo.correo@example.com",
                    menoresActualizados
            );

            ClienteResponseDTO clienteActualizado = new ClienteResponseDTO(
                    1L,
                    "1098765432",
                    "Carlos",
                    "Gómez",
                    "3119876543",
                    "nuevo.correo@example.com",
                    LocalDateTime.now(),
                    menoresActualizados
            );

            when(clienteService.actualizarCliente(eq(1L), any(ClienteRequestDTO.class)))
                    .thenReturn(clienteActualizado);

            mockMvc.perform(put("/api/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.telefono", is("3119876543")))
                    .andExpect(jsonPath("$.correo", is("nuevo.correo@example.com")))
                    .andExpect(jsonPath("$.menores.length()", is(2)))
                    .andExpect(jsonPath("$.menores[1].nombreCompleto", is("Sofía Gómez")));

            verify(clienteService).actualizarCliente(eq(1L), any(ClienteRequestDTO.class));
        }
    }
}
