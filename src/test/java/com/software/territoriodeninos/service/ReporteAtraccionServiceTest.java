package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ReporteAtraccionEstadoDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionRequestDTO;
import com.software.territoriodeninos.dto.ReporteAtraccionResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.ReporteAtraccion;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.ReporteAtraccionRepository;
import com.software.territoriodeninos.security.AuthenticatedUserProvider;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import com.software.territoriodeninos.util.NombreRol;
import com.software.territoriodeninos.util.SeveridadReporte;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteAtraccionServiceTest {

    @Mock ReporteAtraccionRepository reporteAtraccionRepository;
    @Mock AtraccionRepository atraccionRepository;
    @Mock AtraccionService atraccionService;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;
    @Mock Authentication auth;

    @InjectMocks ReporteAtraccionServiceImpl service;

    private Usuario coordinador;
    private Atraccion atraccion;

    @BeforeEach
    void setUp() {
        coordinador = Usuario.builder()
                .id(10L)
                .nombre("Carlos Coord")
                .correo("coord@tdn.com")
                .rol(Rol.builder().nombre(NombreRol.COORDINADOR).build())
                .build();
        atraccion = Atraccion.builder()
                .id(1L)
                .nombre("Piscina de Pelotas")
                .estado(EstadoAtraccion.ACTIVO)
                .build();
    }

    @Test
    void crear_conDesactivarAtraccion_cambiaEstadoAFueraDeServicio() {
        when(authenticatedUserProvider.resolverUsuario(auth)).thenReturn(Optional.of(coordinador));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccion));
        when(reporteAtraccionRepository.save(any(ReporteAtraccion.class))).thenAnswer(inv -> {
            ReporteAtraccion r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReporteAtraccionRequestDTO req = new ReporteAtraccionRequestDTO(
                1L, TipoReporteAtraccion.DANO, SeveridadReporte.CRITICA, "Malla rota en zona sur", true
        );

        ReporteAtraccionResponseDTO resp = service.crear(req, auth);

        assertEquals(50L, resp.id());
        assertEquals(TipoReporteAtraccion.DANO, resp.tipoReporte());
        assertEquals(EstadoReporteAtraccion.ABIERTO, resp.estado());
        assertTrue(resp.atraccionDesactivada());
        assertEquals("FUERA_DE_SERVICIO", resp.atraccionEstadoActual());
        verify(atraccionService).cambiarEstado(1L, EstadoAtraccion.FUERA_DE_SERVICIO.name());
    }

    @Test
    void crear_faltaSuplementosSinDesactivar_noCambiaEstadoAtraccion() {
        when(authenticatedUserProvider.resolverUsuario(auth)).thenReturn(Optional.of(coordinador));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccion));
        when(reporteAtraccionRepository.save(any(ReporteAtraccion.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteAtraccionRequestDTO req = new ReporteAtraccionRequestDTO(
                1L, TipoReporteAtraccion.FALTA_SUPLEMENTOS, SeveridadReporte.MEDIA, "Faltan pelotas de repuesto", false
        );

        ReporteAtraccionResponseDTO resp = service.crear(req, auth);

        assertEquals(TipoReporteAtraccion.FALTA_SUPLEMENTOS, resp.tipoReporte());
        assertFalse(resp.atraccionDesactivada());
        verify(atraccionService, never()).cambiarEstado(anyLong(), anyString());
    }

    @Test
    void actualizarEstado_resueltoConReactivacion_reactivaAtraccion() {
        atraccion.setEstado(EstadoAtraccion.FUERA_DE_SERVICIO);
        ReporteAtraccion existente = ReporteAtraccion.builder()
                .id(50L)
                .atraccion(atraccion)
                .usuarioReporta(coordinador)
                .tipoReporte(TipoReporteAtraccion.DANO)
                .severidad(SeveridadReporte.ALTA)
                .descripcion("Malla rota")
                .estado(EstadoReporteAtraccion.ABIERTO)
                .atraccionDesactivada(true)
                .fechaReporte(LocalDateTime.now().minusHours(1))
                .build();

        when(authenticatedUserProvider.resolverUsuario(auth)).thenReturn(Optional.of(coordinador));
        when(reporteAtraccionRepository.findWithRelationsById(50L)).thenReturn(Optional.of(existente));
        when(reporteAtraccionRepository.save(any(ReporteAtraccion.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteAtraccionEstadoDTO dto = new ReporteAtraccionEstadoDTO(
                EstadoReporteAtraccion.RESUELTO, "Malla reparada", true
        );

        ReporteAtraccionResponseDTO resp = service.actualizarEstado(50L, dto, auth);

        assertEquals(EstadoReporteAtraccion.RESUELTO, resp.estado());
        assertNotNull(resp.fechaResolucion());
        assertEquals("ACTIVO", resp.atraccionEstadoActual());
        verify(atraccionService).cambiarEstado(1L, EstadoAtraccion.ACTIVO.name());
    }

    @Test
    void listar_retornaReportesFiltrados() {
        ReporteAtraccion r = ReporteAtraccion.builder()
                .id(1L).atraccion(atraccion).usuarioReporta(coordinador)
                .tipoReporte(TipoReporteAtraccion.FALTA_SUPLEMENTOS)
                .severidad(SeveridadReporte.MEDIA)
                .descripcion("Sin arneses")
                .estado(EstadoReporteAtraccion.ABIERTO)
                .fechaReporte(LocalDateTime.now())
                .build();
        when(reporteAtraccionRepository.buscarConFiltros(1L, TipoReporteAtraccion.FALTA_SUPLEMENTOS, EstadoReporteAtraccion.ABIERTO))
                .thenReturn(List.of(r));

        List<ReporteAtraccionResponseDTO> lista = service.listar(1L, TipoReporteAtraccion.FALTA_SUPLEMENTOS, EstadoReporteAtraccion.ABIERTO);
        assertEquals(1, lista.size());
        assertEquals("Sin arneses", lista.get(0).descripcion());
    }
}
