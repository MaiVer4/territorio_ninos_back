package com.software.territoriodeninos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.software.territoriodeninos.dto.IncidenteDetalleDTO;
import com.software.territoriodeninos.dto.ResumenIncidentesAuditoriaDTO;
import com.software.territoriodeninos.dto.TimelineTicketDTO;
import com.software.territoriodeninos.service.AuditoriaOperacionService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaControllerTest - Pruebas de endpoints para trazabilidad de tickets y auditoría de incidentes (HU-38.05, HU-41.04)")
class AuditoriaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditoriaOperacionService auditoriaOperacionService;

    @InjectMocks
    private AuditoriaController auditoriaController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(auditoriaController).build();
    }

    @Test
    @DisplayName("GET /api/auditoria/tickets/{criterio} debe retornar línea de tiempo del ticket (HU-38.05)")
    void testConsultarTimelineTicket() throws Exception {
        UUID uuid = UUID.randomUUID();

        TimelineTicketDTO.EventoTimelineTicketDTO evento = TimelineTicketDTO.EventoTimelineTicketDTO.builder()
                .id(1L)
                .estadoDestino("PAGADO")
                .motivo("Venta exitosa")
                .fechaLegible("2026-11-02 10:00:00")
                .usuarioNombre("Laura Taquillera")
                .build();

        TimelineTicketDTO timeline = TimelineTicketDTO.builder()
                .ticketUuid(uuid)
                .atraccionNombre("Carrusel Encantado")
                .clienteNombre("Diana Morales")
                .clienteDocumento("10203040")
                .menorNombre("Sofía Morales")
                .estadoActual("EN_USO")
                .eventos(List.of(evento))
                .build();

        when(auditoriaOperacionService.consultarTimelinePorCriterio(uuid.toString()))
                .thenReturn(List.of(timeline));

        mockMvc.perform(get("/api/auditoria/tickets/" + uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticketUuid", is(uuid.toString())))
                .andExpect(jsonPath("$[0].atraccionNombre", is("Carrusel Encantado")))
                .andExpect(jsonPath("$[0].eventos", hasSize(1)))
                .andExpect(jsonPath("$[0].eventos[0].estadoDestino", is("PAGADO")));
    }

    @Test
    @DisplayName("GET /api/auditoria/incidentes debe retornar el resumen de tiempos y eventos (HU-41.04)")
    void testConsultarIncidentes() throws Exception {
        IncidenteDetalleDTO detalle = IncidenteDetalleDTO.builder()
                .incidenteId(1L)
                .atraccionId(10L)
                .atraccionNombre("Castillo Mágico")
                .tipoPausa("OPERATIVA")
                .motivo("Cambio de turno")
                .esPausaGeneral(false)
                .operarioNombre("Andrés Gómez")
                .duracionSegundos(600)
                .duracionMinutos(10.0)
                .build();

        ResumenIncidentesAuditoriaDTO resumen = ResumenIncidentesAuditoriaDTO.builder()
                .totalIncidentes(1)
                .tiempoTotalInactividadSegundos(600L)
                .tiempoTotalInactividadMinutos(10.0)
                .inactividadOperativaSegundos(600L)
                .inactividadTecnicaSegundos(0L)
                .incidentes(List.of(detalle))
                .build();

        when(auditoriaOperacionService.consultarIncidentes(any(), any(), any(), any()))
                .thenReturn(resumen);

        mockMvc.perform(get("/api/auditoria/incidentes")
                        .param("tipoPausa", "OPERATIVA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidentes", is(1)))
                .andExpect(jsonPath("$.tiempoTotalInactividadMinutos", is(10.0)))
                .andExpect(jsonPath("$.inactividadOperativaSegundos", is(600)))
                .andExpect(jsonPath("$.incidentes", hasSize(1)))
                .andExpect(jsonPath("$.incidentes[0].atraccionNombre", is("Castillo Mágico")));
    }
}
