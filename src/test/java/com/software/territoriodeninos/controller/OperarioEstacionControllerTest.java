package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.EstacionTrabajoDTO;
import com.software.territoriodeninos.exception.GlobalExceptionHandler;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.service.OperacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperarioEstacionControllerTest - Pruebas Web MockMvc para consulta de estación activa (HU-24.03)")
class OperarioEstacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperacionService operacionService;

    @InjectMocks
    private OperarioEstacionController operarioEstacionController;

    private EstacionTrabajoDTO estacionDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(operarioEstacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        estacionDTO = new EstacionTrabajoDTO(
                10L,
                5L,
                "Pepito Operario",
                "operario@territoriodeninos.com",
                1L,
                "Piscina de Pelotas",
                "Atracción principal",
                "TEMPORIZADA",
                20,
                5,
                15,
                15,
                5,
                3,
                3,
                12,
                "3 a 12 años",
                "ACTIVO",
                LocalDate.now(),
                LocalDateTime.now().minusHours(1)
        );
    }

    @Test
    @DisplayName("GET /api/operario/estacion-activa - Retorna 200 OK con datos de estación activa (HU-24.03)")
    void obtenerEstacionActiva_exito_retorna200() throws Exception {
        when(operacionService.obtenerEstacionActiva(any())).thenReturn(estacionDTO);

        mockMvc.perform(get("/api/operario/estacion-activa"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.asignacionId", is(10)))
                .andExpect(jsonPath("$.operarioNombre", is("Pepito Operario")))
                .andExpect(jsonPath("$.atraccionId", is(1)))
                .andExpect(jsonPath("$.atraccionNombre", is("Piscina de Pelotas")))
                .andExpect(jsonPath("$.tipoAtraccion", is("TEMPORIZADA")))
                .andExpect(jsonPath("$.capacidadMaxima", is(20)))
                .andExpect(jsonPath("$.aforoActual", is(5)))
                .andExpect(jsonPath("$.cuposDisponibles", is(15)))
                .andExpect(jsonPath("$.rangoEdad", is("3 a 12 años")));

        verify(operacionService).obtenerEstacionActiva(any());
    }

    @Test
    @DisplayName("GET /api/operario/estacion-activa - Retorna 404 Not Found si no tiene asignación activa")
    void obtenerEstacionActiva_sinAsignacion_retorna404() throws Exception {
        when(operacionService.obtenerEstacionActiva(any()))
                .thenThrow(new RecursoNoEncontradoException("No tiene una estación de trabajo activa asignada"));

        mockMvc.perform(get("/api/operario/estacion-activa"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.detalles", containsString("No tiene una estación de trabajo activa")));

        verify(operacionService).obtenerEstacionActiva(any());
    }
}
