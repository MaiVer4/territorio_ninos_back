package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.TarifaEventoRequestDTO;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.TipoAtraccion;
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
class TarifaEventoServiceTest {

    @Mock
    private TarifaEventoRepository tarifaEventoRepository;

    @Mock
    private AtraccionRepository atraccionRepository;

    @InjectMocks
    private TarifaEventoServiceImpl tarifaEventoService;

    private Atraccion atraccionMock;
    private TarifaEvento tarifaMock;
    private TarifaEventoRequestDTO requestMock;

    @BeforeEach
    void setUp() {
        atraccionMock = Atraccion.builder()
                .id(2L)
                .nombre("Toro Mecánico")
                .tipo(TipoAtraccion.EVENTOS_CAIDAS)
                .tarifaMinuto(new BigDecimal("0.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .caidasLimite(3)
                .build();

        tarifaMock = TarifaEvento.builder()
                .id(20L)
                .atraccion(atraccionMock)
                .intentos(1)
                .precio(new BigDecimal("8000.00"))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        requestMock = new TarifaEventoRequestDTO(2L, 1, new BigDecimal("8000.00"), true);
    }

    @Test
    @DisplayName("HU-09: Crear tarifa por evento exitosamente (201)")
    void crear_Exito() {
        when(atraccionRepository.findById(2L)).thenReturn(Optional.of(atraccionMock));
        when(tarifaEventoRepository.existsByAtraccionIdAndIntentos(2L, 1)).thenReturn(false);
        when(tarifaEventoRepository.save(any(TarifaEvento.class))).thenReturn(tarifaMock);

        TarifaEventoResponseDTO response = tarifaEventoService.crear(requestMock);

        assertNotNull(response);
        assertEquals(20L, response.id());
        assertEquals(1, response.intentos());
        assertEquals(new BigDecimal("8000.00"), response.precio());
        verify(tarifaEventoRepository).save(any(TarifaEvento.class));
    }

    @Test
    @DisplayName("HU-09: Error al crear con intentos menores a 1")
    void crear_IntentosInvalidos_LanzaExcepcion() {
        TarifaEventoRequestDTO requestInvalido = new TarifaEventoRequestDTO(2L, 0, new BigDecimal("8000"), true);
        when(atraccionRepository.findById(2L)).thenReturn(Optional.of(atraccionMock));

        assertThrows(IllegalArgumentException.class, () -> tarifaEventoService.crear(requestInvalido));
        verify(tarifaEventoRepository, never()).save(any(TarifaEvento.class));
    }

    @Test
    @DisplayName("HU-09: Error al crear con precio menor o igual a 0")
    void crear_PrecioInvalido_LanzaExcepcion() {
        TarifaEventoRequestDTO requestInvalido = new TarifaEventoRequestDTO(2L, 2, BigDecimal.ZERO, true);
        when(atraccionRepository.findById(2L)).thenReturn(Optional.of(atraccionMock));

        assertThrows(IllegalArgumentException.class, () -> tarifaEventoService.crear(requestInvalido));
        verify(tarifaEventoRepository, never()).save(any(TarifaEvento.class));
    }

    @Test
    @DisplayName("HU-09: Conflicto 409 cuando ya existe tarifa para el mismo número de intentos")
    void crear_ConflictoDuplicado() {
        when(atraccionRepository.findById(2L)).thenReturn(Optional.of(atraccionMock));
        when(tarifaEventoRepository.existsByAtraccionIdAndIntentos(2L, 1)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> tarifaEventoService.crear(requestMock));
        verify(tarifaEventoRepository, never()).save(any(TarifaEvento.class));
    }

    @Test
    @DisplayName("HU-09: Obtener tarifas de evento por atracción")
    void obtenerPorAtraccion_Exito() {
        when(atraccionRepository.existsById(2L)).thenReturn(true);
        when(tarifaEventoRepository.findByAtraccionId(2L)).thenReturn(List.of(tarifaMock));

        List<TarifaEventoResponseDTO> lista = tarifaEventoService.obtenerPorAtraccion(2L);

        assertFalse(lista.isEmpty());
        assertEquals(1, lista.size());
        assertEquals(1, lista.get(0).intentos());
    }

    @Test
    @DisplayName("HU-09: Actualizar tarifa de evento exitosamente")
    void actualizar_Exito() {
        when(tarifaEventoRepository.findById(20L)).thenReturn(Optional.of(tarifaMock));
        TarifaEventoRequestDTO updateRequest = new TarifaEventoRequestDTO(2L, 3, new BigDecimal("20000.00"), true);
        when(tarifaEventoRepository.existsByAtraccionIdAndIntentos(2L, 3)).thenReturn(false);

        TarifaEventoResponseDTO response = tarifaEventoService.actualizar(20L, updateRequest);

        assertNotNull(response);
        assertEquals(3, tarifaMock.getIntentos());
        assertEquals(new BigDecimal("20000.00"), tarifaMock.getPrecio());
    }

    @Test
    @DisplayName("HU-09: Eliminar tarifa de evento exitosamente")
    void eliminar_Exito() {
        when(tarifaEventoRepository.findById(20L)).thenReturn(Optional.of(tarifaMock));

        tarifaEventoService.eliminar(20L);

        verify(tarifaEventoRepository).delete(tarifaMock);
    }
}
