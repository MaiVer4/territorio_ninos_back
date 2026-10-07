package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaTiempoRequestDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.TarifaTiempoRepository;
import com.software.territoriodeninos.util.EstadoAtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class TarifaTiempoServiceTest {

    @Mock
    private TarifaTiempoRepository tarifaTiempoRepository;

    @Mock
    private AtraccionRepository atraccionRepository;

    @InjectMocks
    private TarifaTiempoServiceImpl tarifaTiempoService;

    private Atraccion atraccionMock;
    private TarifaTiempo tarifaMock;
    private TarifaTiempoRequestDTO requestMock;

    @BeforeEach
    void setUp() {
        atraccionMock = Atraccion.builder()
                .id(1L)
                .nombre("Camas Elásticas")
                .tarifaMinuto(new BigDecimal("50.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();

        tarifaMock = TarifaTiempo.builder()
                .id(10L)
                .atraccion(atraccionMock)
                .duracionMinutos(15)
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        requestMock = new TarifaTiempoRequestDTO(1L, 15, new BigDecimal("15000.00"), true);
    }

    @Test
    @DisplayName("HU-08: Crear tarifa por tiempo exitosamente (201)")
    void crear_Exito() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));
        when(tarifaTiempoRepository.existsByAtraccionIdAndDuracionMinutos(1L, 15)).thenReturn(false);
        when(tarifaTiempoRepository.save(any(TarifaTiempo.class))).thenReturn(tarifaMock);

        TarifaTiempoResponseDTO response = tarifaTiempoService.crear(requestMock);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(15, response.duracionMinutos());
        assertEquals(new BigDecimal("15000.00"), response.precio());
        verify(tarifaTiempoRepository).save(any(TarifaTiempo.class));
    }

    @Test
    @DisplayName("HU-08: Error al crear con precio menor o igual a 0")
    void crear_PrecioInvalido_LanzaExcepcion() {
        TarifaTiempoRequestDTO requestInvalido = new TarifaTiempoRequestDTO(1L, 15, BigDecimal.ZERO, true);
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));

        assertThrows(IllegalArgumentException.class, () -> tarifaTiempoService.crear(requestInvalido));
        verify(tarifaTiempoRepository, never()).save(any(TarifaTiempo.class));
    }

    @Test
    @DisplayName("HU-08: Error al crear con duracion menor o igual a 0")
    void crear_DuracionInvalida_LanzaExcepcion() {
        TarifaTiempoRequestDTO requestInvalido = new TarifaTiempoRequestDTO(1L, 0, new BigDecimal("5000"), true);
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));

        assertThrows(IllegalArgumentException.class, () -> tarifaTiempoService.crear(requestInvalido));
        verify(tarifaTiempoRepository, never()).save(any(TarifaTiempo.class));
    }

    @Test
    @DisplayName("HU-08: Conflicto 409 cuando ya existe tarifa para la misma duración")
    void crear_ConflictoDuplicado() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock));
        when(tarifaTiempoRepository.existsByAtraccionIdAndDuracionMinutos(1L, 15)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> tarifaTiempoService.crear(requestMock));
        verify(tarifaTiempoRepository, never()).save(any(TarifaTiempo.class));
    }

    @Test
    @DisplayName("HU-08: Atracción no encontrada al crear tarifa")
    void crear_AtraccionNoEncontrada() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> tarifaTiempoService.crear(requestMock));
        verify(tarifaTiempoRepository, never()).save(any(TarifaTiempo.class));
    }

    @Test
    @DisplayName("HU-08: Obtener tarifas por atracción")
    void obtenerPorAtraccion_Exito() {
        when(atraccionRepository.existsById(1L)).thenReturn(true);
        when(tarifaTiempoRepository.findByAtraccionId(1L)).thenReturn(List.of(tarifaMock));

        List<TarifaTiempoResponseDTO> lista = tarifaTiempoService.obtenerPorAtraccion(1L);

        assertFalse(lista.isEmpty());
        assertEquals(1, lista.size());
        assertEquals(15, lista.get(0).duracionMinutos());
    }

    @Test
    @DisplayName("HU-08: Actualizar tarifa exitosamente")
    void actualizar_Exito() {
        when(tarifaTiempoRepository.findById(10L)).thenReturn(Optional.of(tarifaMock));
        TarifaTiempoRequestDTO updateRequest = new TarifaTiempoRequestDTO(1L, 30, new BigDecimal("25000.00"), true);
        when(tarifaTiempoRepository.existsByAtraccionIdAndDuracionMinutos(1L, 30)).thenReturn(false);

        TarifaTiempoResponseDTO response = tarifaTiempoService.actualizar(10L, updateRequest);

        assertNotNull(response);
        assertEquals(30, tarifaMock.getDuracionMinutos());
        assertEquals(new BigDecimal("25000.00"), tarifaMock.getPrecio());
    }

    @Test
    @DisplayName("HU-08: Eliminar tarifa exitosamente")
    void eliminar_Exito() {
        when(tarifaTiempoRepository.findById(10L)).thenReturn(Optional.of(tarifaMock));

        tarifaTiempoService.eliminar(10L);

        verify(tarifaTiempoRepository).delete(tarifaMock);
    }
}
