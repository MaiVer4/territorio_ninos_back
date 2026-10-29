package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AtraccionRequestDTO;
import com.software.territoriodeninos.dto.AtraccionResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.util.EstadoAtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtraccionServiceTest {

    @Mock
    private AtraccionRepository atraccionRepository;

    @Mock
    private com.software.territoriodeninos.repository.AsignacionOperarioRepository asignacionRepository;

    @Mock
    private com.software.territoriodeninos.repository.TarifaTiempoRepository tarifaTiempoRepository;

    @Mock
    private com.software.territoriodeninos.repository.TarifaEventoRepository tarifaEventoRepository;

    @Mock
    private com.software.territoriodeninos.repository.DetalleVentaRepository detalleVentaRepository;

    @InjectMocks
    private AtraccionServiceImpl atraccionService;

    private Atraccion atraccionMock;
    private AtraccionRequestDTO requestMock;

    @BeforeEach
    void setUp() {
        atraccionMock = Atraccion.builder()
                .id(1L)
                .nombre("Inflable")
                .descripcion("Juego para saltar")
                .tarifaMinuto(new BigDecimal("100.50"))
                .estado(EstadoAtraccion.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        requestMock = new AtraccionRequestDTO("Inflable", "Juego para saltar", new BigDecimal("100.50"));
    }

    @Test
    void crear_Exito() {
        when(atraccionRepository.existsByNombreIgnoreCase(anyString())).thenReturn(false);
        when(atraccionRepository.save(any(Atraccion.class))).thenReturn(atraccionMock);

        AtraccionResponseDTO result = atraccionService.crear(requestMock);

        assertNotNull(result);
        assertEquals("Inflable", result.nombre());
        verify(atraccionRepository).save(any(Atraccion.class));
    }

    @Test
    void crear_ConflictoNombre() {
        when(atraccionRepository.existsByNombreIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ConflictoException.class, () -> atraccionService.crear(requestMock));
        verify(atraccionRepository, never()).save(any(Atraccion.class));
    }

    @Test
    void obtenerTodas_Exito() {
        when(atraccionRepository.findAll()).thenReturn(List.of(atraccionMock));

        List<AtraccionResponseDTO> result = atraccionService.obtenerTodas();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void obtenerActivas_Exito() {
        when(atraccionRepository.findByEstado(EstadoAtraccion.ACTIVO)).thenReturn(List.of(atraccionMock));

        List<AtraccionResponseDTO> result = atraccionService.obtenerActivas();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void obtenerPorId_Exito() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));

        AtraccionResponseDTO result = atraccionService.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void obtenerPorId_NoEncontrado() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> atraccionService.obtenerPorId(1L));
    }

    @Test
    void actualizar_Exito() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));
        
        AtraccionRequestDTO updateRequest = new AtraccionRequestDTO("Inflable 2", "Nuevo juego", new BigDecimal("150.00"));
        when(atraccionRepository.existsByNombreIgnoreCase(anyString())).thenReturn(false);

        AtraccionResponseDTO result = atraccionService.actualizar(1L, updateRequest);

        assertEquals("Inflable 2", result.nombre());
        assertEquals(new BigDecimal("150.00"), result.tarifaMinuto());
    }

    @Test
    void cambiarEstado_Exito() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));

        AtraccionResponseDTO result = atraccionService.cambiarEstado(1L, "MANTENIMIENTO");

        assertEquals("MANTENIMIENTO", result.estado());
    }

    @Test
    void cambiarEstado_EstadoInvalido() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));

        assertThrows(ConflictoException.class, () -> atraccionService.cambiarEstado(1L, "INVALIDO"));
    }

    @Test
    void eliminar_Exito() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));

        atraccionService.eliminar(1L);

        assertEquals(EstadoAtraccion.INACTIVO, atraccionMock.getEstado());
    }

    @Test
    void eliminar_ConOperariosActivos_LanzaConflicto() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));
        when(asignacionRepository.existsByAtraccionIdAndEstado(atraccionMock.getId(),
                com.software.territoriodeninos.util.EstadoAsignacion.ACTIVA)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> atraccionService.eliminar(1L));
        assertNotEquals(EstadoAtraccion.INACTIVO, atraccionMock.getEstado());
    }

    @Test
    void cambiarEstado_AMantenimientoConOperariosActivos_LanzaConflicto() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));
        when(asignacionRepository.existsByAtraccionIdAndEstado(atraccionMock.getId(),
                com.software.territoriodeninos.util.EstadoAsignacion.ACTIVA)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> atraccionService.cambiarEstado(1L, "MANTENIMIENTO"));
    }

    @Test
    void obtenerCatalogoVenta_ExitoYOrdenamientoTarifas() {
        Atraccion atraccion1 = Atraccion.builder()
                .id(1L)
                .nombre("Autos Chocones")
                .estado(EstadoAtraccion.ACTIVO)
                .capacidadMaxima(10)
                .duracionMinutosBase(15)
                .tarifaMinuto(new BigDecimal("1500.00"))
                .build();

        com.software.territoriodeninos.entity.TarifaTiempo tt1 = com.software.territoriodeninos.entity.TarifaTiempo.builder()
                .id(101L)
                .atraccion(atraccion1)
                .duracionMinutos(15)
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .build();

        com.software.territoriodeninos.entity.TarifaTiempo tt2 = com.software.territoriodeninos.entity.TarifaTiempo.builder()
                .id(102L)
                .atraccion(atraccion1)
                .duracionMinutos(30)
                .precio(new BigDecimal("25000.00"))
                .activo(true)
                .build();

        com.software.territoriodeninos.entity.TarifaEvento te1 = com.software.territoriodeninos.entity.TarifaEvento.builder()
                .id(201L)
                .atraccion(atraccion1)
                .intentos(3)
                .precio(new BigDecimal("10000.00"))
                .activo(true)
                .build();

        when(atraccionRepository.findByEstadoOrderByNombreAsc(EstadoAtraccion.ACTIVO)).thenReturn(List.of(atraccion1));
        when(tarifaTiempoRepository.findByAtraccionIdInAndActivoTrueOrderByDuracionMinutosAsc(List.of(1L)))
                .thenReturn(List.of(tt1, tt2));
        when(tarifaEventoRepository.findByAtraccionIdInAndActivoTrueOrderByIntentosAsc(List.of(1L)))
                .thenReturn(List.of(te1));
        when(asignacionRepository.findAtraccionIdsConAsignacionEnEstado(List.of(1L), com.software.territoriodeninos.util.EstadoAsignacion.ACTIVA))
                .thenReturn(java.util.Set.of(1L));

        atraccionService.invalidarCacheCatalogo();
        List<com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO> catalogo = atraccionService.obtenerCatalogoVenta();

        assertNotNull(catalogo);
        assertEquals(1, catalogo.size());
        var item = catalogo.get(0);
        assertEquals("Autos Chocones", item.nombre());
        assertTrue(item.tieneOperarioAsignado());
        assertEquals(10, item.aforoDisponible());
        assertTrue(item.tieneCupo());
        assertEquals(2, item.tarifasTiempo().size());
        assertEquals(15, item.tarifasTiempo().get(0).duracionMinutos());
        assertEquals(30, item.tarifasTiempo().get(1).duracionMinutos());
        assertEquals(1, item.tarifasEvento().size());
        assertEquals(3, item.tarifasEvento().get(0).intentos());

        // Segunda llamada dentro del TTL: debe servirse de caché sin volver a consultar repositorios
        List<com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO> cachedCatalogo = atraccionService.obtenerCatalogoVenta();
        assertSame(catalogo, cachedCatalogo);
        verify(atraccionRepository, times(1)).findByEstadoOrderByNombreAsc(EstadoAtraccion.ACTIVO);

        // Invalidación forzada
        atraccionService.invalidarCacheCatalogo();
        atraccionService.obtenerCatalogoVenta();
        verify(atraccionRepository, times(2)).findByEstadoOrderByNombreAsc(EstadoAtraccion.ACTIVO);
    }
}
