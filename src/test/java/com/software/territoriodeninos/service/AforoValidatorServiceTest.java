package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.TarifaEventoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AforoValidatorServiceTest - Pruebas unitarias de validación de aforo y anti-adulteración de tarifas")
class AforoValidatorServiceTest {

    @Mock
    private AtraccionRepository atraccionRepository;

    @Mock
    private TarifaTiempoRepository tarifaTiempoRepository;

    @Mock
    private TarifaEventoRepository tarifaEventoRepository;

    @InjectMocks
    private AforoValidatorServiceImpl aforoValidatorService;

    private Atraccion atraccionActiva;
    private TarifaTiempo tarifaTiempoOficial;
    private TarifaEvento tarifaEventoOficial;

    @BeforeEach
    void setUp() {
        atraccionActiva = Atraccion.builder()
                .id(1L)
                .nombre("Camas Elásticas")
                .estado(EstadoAtraccion.ACTIVO)
                .capacidadMaxima(10)
                .duracionMinutosBase(15)
                .tarifaMinuto(new BigDecimal("1000.00"))
                .build();

        tarifaTiempoOficial = TarifaTiempo.builder()
                .id(100L)
                .atraccion(atraccionActiva)
                .duracionMinutos(15)
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .build();

        tarifaEventoOficial = TarifaEvento.builder()
                .id(200L)
                .atraccion(atraccionActiva)
                .intentos(3)
                .precio(new BigDecimal("10000.00"))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("validarCapacidadAtraccion - Exitoso cuando hay cupo disponible")
    void testValidarCapacidadAtraccion_Exitoso() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));

        int cuposRestantes = aforoValidatorService.validarCapacidadAtraccion(1L, 4);

        assertEquals(6, cuposRestantes);
        verify(atraccionRepository).findById(1L);
    }

    @Test
    @DisplayName("validarCapacidadAtraccion - Lanza ReglaNegocioException cuando la cantidad supera la capacidad máxima")
    void testValidarCapacidadAtraccion_Excedido() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarCapacidadAtraccion(1L, 11)
        );

        assertTrue(ex.getMessage().contains("Aforo máximo excedido"));
    }

    @Test
    @DisplayName("validarCapacidadAtraccion - Lanza ReglaNegocioException cuando la atracción está en Mantenimiento o Inactiva")
    void testValidarCapacidadAtraccion_Inactiva() {
        atraccionActiva.setEstado(EstadoAtraccion.MANTENIMIENTO);
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarCapacidadAtraccion(1L, 2)
        );

        assertTrue(ex.getMessage().contains("no se encuentra activa"));
    }

    @Test
    @DisplayName("validarCapacidadAtraccion - Lanza RecursoNoEncontradoException si la atracción no existe")
    void testValidarCapacidadAtraccion_NoEncontrada() {
        when(atraccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                aforoValidatorService.validarCapacidadAtraccion(99L, 1)
        );
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Tarifa por tiempo válida con precio idéntico")
    void testValidarConsistenciaTarifaTiempo_Exitoso() {
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));

        ItemVentaValidacionDTO item = new ItemVentaValidacionDTO(
                1L, 100L, null, 2, new BigDecimal("15000.00"), new BigDecimal("30000.00"), 15, null
        );

        BigDecimal precioOficial = aforoValidatorService.validarConsistenciaTarifa(item);

        assertEquals(0, new BigDecimal("15000.00").compareTo(precioOficial));
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Tolera escala decimal equivalente (15000 vs 15000.00)")
    void testValidarConsistenciaTarifaTiempo_EscalaDecimalEquivalente() {
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));

        ItemVentaValidacionDTO item = new ItemVentaValidacionDTO(
                1L, 100L, null, 1, new BigDecimal("15000"), new BigDecimal("15000"), 15, null
        );

        BigDecimal precioOficial = aforoValidatorService.validarConsistenciaTarifa(item);

        assertEquals(0, new BigDecimal("15000.00").compareTo(precioOficial));
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Lanza ReglaNegocioException si el precio unitario fue adulterado en el cliente")
    void testValidarConsistenciaTarifaTiempo_AdulteracionPrecioUnitario() {
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));

        ItemVentaValidacionDTO itemAdulterado = new ItemVentaValidacionDTO(
                1L, 100L, null, 1, new BigDecimal("5000.00"), null, 15, null
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarConsistenciaTarifa(itemAdulterado)
        );

        assertTrue(ex.getMessage().contains("Inconsistencia en el precio"));
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Lanza ReglaNegocioException si el subtotal fue adulterado")
    void testValidarConsistenciaTarifaTiempo_AdulteracionSubtotal() {
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));

        // Precio unitario coincide ($15000), pero cantidad 2 con subtotal $15000 en vez de $30000
        ItemVentaValidacionDTO itemSubtotalAdulterado = new ItemVentaValidacionDTO(
                1L, 100L, null, 2, new BigDecimal("15000.00"), new BigDecimal("15000.00"), 15, null
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarConsistenciaTarifa(itemSubtotalAdulterado)
        );

        assertTrue(ex.getMessage().contains("Inconsistencia en el subtotal"));
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Lanza ReglaNegocioException si la tarifa está inactiva")
    void testValidarConsistenciaTarifaTiempo_Inactiva() {
        tarifaTiempoOficial.setActivo(false);
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));

        ItemVentaValidacionDTO item = new ItemVentaValidacionDTO(
                1L, 100L, null, 1, new BigDecimal("15000.00"), null, 15, null
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarConsistenciaTarifa(item)
        );

        assertTrue(ex.getMessage().contains("se encuentra inactiva"));
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Lanza ReglaNegocioException si la tarifa pertenece a otra atracción")
    void testValidarConsistenciaTarifaTiempo_AtraccionCruzada() {
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));

        // Intenta usar la tarifa de la atracción 1 para la atracción 2
        ItemVentaValidacionDTO itemCruzado = new ItemVentaValidacionDTO(
                2L, 100L, null, 1, new BigDecimal("15000.00"), null, 15, null
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarConsistenciaTarifa(itemCruzado)
        );

        assertTrue(ex.getMessage().contains("no pertenece a la atracción"));
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Tarifa por evento válida y detección de adulteración")
    void testValidarConsistenciaTarifaEvento() {
        when(tarifaEventoRepository.findById(200L)).thenReturn(Optional.of(tarifaEventoOficial));

        ItemVentaValidacionDTO itemValido = new ItemVentaValidacionDTO(
                1L, null, 200L, 1, new BigDecimal("10000.00"), new BigDecimal("10000.00"), null, 3
        );
        BigDecimal oficial = aforoValidatorService.validarConsistenciaTarifa(itemValido);
        assertEquals(0, new BigDecimal("10000.00").compareTo(oficial));

        ItemVentaValidacionDTO itemPrecioFalso = new ItemVentaValidacionDTO(
                1L, null, 200L, 1, new BigDecimal("1000.00"), null, null, 3
        );
        assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarConsistenciaTarifa(itemPrecioFalso)
        );
    }

    @Test
    @DisplayName("validarConsistenciaTarifa - Lanza excepción si especifica ambas tarifas o ninguna")
    void testValidarConsistenciaTarifa_DobleTarifaONinguna() {
        ItemVentaValidacionDTO sinTarifa = new ItemVentaValidacionDTO(
                1L, null, null, 1, new BigDecimal("10000.00"), null, null, null
        );
        assertThrows(ReglaNegocioException.class, () -> aforoValidatorService.validarConsistenciaTarifa(sinTarifa));

        ItemVentaValidacionDTO dobleTarifa = new ItemVentaValidacionDTO(
                1L, 100L, 200L, 1, new BigDecimal("10000.00"), null, null, null
        );
        assertThrows(ReglaNegocioException.class, () -> aforoValidatorService.validarConsistenciaTarifa(dobleTarifa));
    }

    @Test
    @DisplayName("validarItemsVenta - Agrupa tickets por atracción y detecta sobrecupo en la suma acumulada")
    void testValidarItemsVenta_SobrecupoAcumulado() {
        // Capacidad de la atracción 1 es 10
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));

        // Dos líneas en el carrito: línea 1 pide 6 tickets, línea 2 pide 5 tickets (Suma = 11 > 10)
        ItemVentaValidacionDTO linea1 = new ItemVentaValidacionDTO(
                1L, 100L, null, 6, new BigDecimal("15000.00"), null, 15, null
        );
        ItemVentaValidacionDTO linea2 = new ItemVentaValidacionDTO(
                1L, 100L, null, 5, new BigDecimal("15000.00"), null, 15, null
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                aforoValidatorService.validarItemsVenta(List.of(linea1, linea2))
        );

        assertTrue(ex.getMessage().contains("Aforo máximo excedido"));
        assertTrue(ex.getMessage().contains("11 ticket(s)"));
    }

    @Test
    @DisplayName("validarItemsVenta - Exitoso calcula el total oficial y aforo restante por atracción")
    void testValidarItemsVenta_Exitoso() {
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionActiva));
        when(tarifaTiempoRepository.findById(100L)).thenReturn(Optional.of(tarifaTiempoOficial));
        when(tarifaEventoRepository.findById(200L)).thenReturn(Optional.of(tarifaEventoOficial));

        // Línea 1: 2 tickets de tiempo a $15000 = $30000
        ItemVentaValidacionDTO linea1 = new ItemVentaValidacionDTO(
                1L, 100L, null, 2, new BigDecimal("15000.00"), new BigDecimal("30000.00"), 15, null
        );
        // Línea 2: 1 ticket de evento a $10000 = $10000
        ItemVentaValidacionDTO linea2 = new ItemVentaValidacionDTO(
                1L, null, 200L, 1, new BigDecimal("10000.00"), new BigDecimal("10000.00"), null, 3
        );

        ResultadoValidacionVentaDTO resultado = aforoValidatorService.validarItemsVenta(List.of(linea1, linea2));

        assertNotNull(resultado);
        assertTrue(resultado.valido());
        assertEquals(3, resultado.totalTickets());
        assertEquals(0, new BigDecimal("40000.00").compareTo(resultado.totalOficial()));
        // Capacidad inicial 10 - 3 tickets = 7 restantes
        assertEquals(7, resultado.aforoRestantePorAtraccion().get(1L));
    }
}
