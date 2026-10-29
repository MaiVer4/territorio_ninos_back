package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;
import com.software.territoriodeninos.dto.ItemCotizacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;
import com.software.territoriodeninos.entity.ConfiguracionParque;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.ConfiguracionParqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaCalculoServiceImpl - Tests unitarios para motor de liquidación y cotización (CA-04)")
class VentaCalculoServiceTest {

    @Mock
    private ConfiguracionParqueRepository configuracionParqueRepository;

    @Mock
    private AforoValidatorService aforoValidatorService;

    @InjectMocks
    private VentaCalculoServiceImpl ventaCalculoService;

    private ConfiguracionParque configuracionConIva;
    private ConfiguracionParque configuracionExenta;

    @BeforeEach
    void setUp() {
        configuracionConIva = ConfiguracionParque.builder()
                .id(1L)
                .nombreParque("Territorio de Niños")
                .aplicaImpuesto(true)
                .porcentajeImpuesto(new BigDecimal("19.00"))
                .build();

        configuracionExenta = ConfiguracionParque.builder()
                .id(1L)
                .nombreParque("Territorio de Niños")
                .aplicaImpuesto(false)
                .porcentajeImpuesto(BigDecimal.ZERO)
                .build();
    }

    @Nested
    @DisplayName("Redondeo Comercial Exacto (HALF_UP a 2 decimales)")
    class RedondeoComercialTests {

        @Test
        @DisplayName("Debe aplicar redondeo HALF_UP hacia arriba cuando el tercer decimal >= 5 (ej. 12345.67 * 19% = 2345.6773 -> 2345.68)")
        void testRedondeoComercial_HaciaArriba() {
            // Arrange: base 12345.67, IVA 19%
            // 12345.67 * 19 / 100 = 2345.6773 -> 2345.68
            // Total neto: 12345.67 + 2345.68 = 14691.35
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionConIva));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Servicio Especial", 1, new BigDecimal("12345.67")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("12345.67"), response.subtotal());
            assertEquals(new BigDecimal("0.00"), response.descuento());
            assertEquals(new BigDecimal("12345.67"), response.baseGravable());
            assertEquals(new BigDecimal("19.00"), response.porcentajeImpuesto());
            assertEquals(new BigDecimal("2345.68"), response.impuestos());
            assertEquals(new BigDecimal("14691.35"), response.totalNeto());
            assertEquals(new BigDecimal("14691.35"), response.total());
            assertEquals(1, response.subtotales().size());
            assertEquals(new BigDecimal("12345.67"), response.subtotales().get(0));
        }

        @Test
        @DisplayName("Debe aplicar redondeo HALF_UP en subtotales de líneas con decimales")
        void testRedondeoComercial_LineasConDecimales() {
            // Arrange: precio unitario 33.333 -> redondea a 33.33 por unidad
            // 33.33 * 3 = 99.99
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionExenta));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item con decimales", 3, new BigDecimal("33.333")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("99.99"), response.subtotal());
            assertEquals(new BigDecimal("0.00"), response.impuestos());
            assertEquals(new BigDecimal("99.99"), response.totalNeto());
        }
    }

    @Nested
    @DisplayName("Cálculo Dinámico de Impuestos según ConfiguracionParque")
    class ImpuestosDinamicosTests {

        @Test
        @DisplayName("Debe calcular IVA al 19.00% cuando aplicaImpuesto es true y porcentaje es 19.00")
        void testCalculoImpuesto_IVA19() {
            // Arrange: subtotal 30000.00, IVA 19% -> impuesto 5700.00, total 35700.00
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionConIva));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Entrada General", 2, new BigDecimal("15000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("30000.00"), response.subtotal());
            assertEquals(new BigDecimal("5700.00"), response.impuestos());
            assertEquals(new BigDecimal("35700.00"), response.totalNeto());
        }

        @Test
        @DisplayName("Debe calcular Impoconsumo al 8.00% cuando la configuración del parque así lo define")
        void testCalculoImpuesto_Impoconsumo8() {
            // Arrange: base 50000.00, Impuesto 8% -> impuesto 4000.00, total 54000.00
            ConfiguracionParque config8 = ConfiguracionParque.builder()
                    .aplicaImpuesto(true)
                    .porcentajeImpuesto(new BigDecimal("8.00"))
                    .build();
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config8));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Snacks y Bebidas", 5, new BigDecimal("10000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("50000.00"), response.subtotal());
            assertEquals(new BigDecimal("8.00"), response.porcentajeImpuesto());
            assertEquals(new BigDecimal("4000.00"), response.impuestos());
            assertEquals(new BigDecimal("54000.00"), response.totalNeto());
        }

        @Test
        @DisplayName("Debe fijar impuestos en 0.00 cuando aplicaImpuesto es false (exento)")
        void testCalculoImpuesto_Exento() {
            // Arrange
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionExenta));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Entrada Exenta", 1, new BigDecimal("20000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("20000.00"), response.subtotal());
            assertEquals(new BigDecimal("0.00"), response.impuestos());
            assertEquals(new BigDecimal("20000.00"), response.totalNeto());
        }

        @Test
        @DisplayName("Debe manejar ausencia de configuración asignando 0.00 de impuestos por defecto")
        void testCalculoImpuesto_SinConfiguracion() {
            // Arrange
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item General", 1, new BigDecimal("10000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("10000.00"), response.subtotal());
            assertEquals(new BigDecimal("0.00"), response.impuestos());
            assertEquals(new BigDecimal("10000.00"), response.totalNeto());
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException si el porcentaje de impuesto en configuración es negativo")
        void testCalculoImpuesto_PorcentajeNegativo() {
            // Arrange
            ConfiguracionParque configInvalida = ConfiguracionParque.builder()
                    .aplicaImpuesto(true)
                    .porcentajeImpuesto(new BigDecimal("-5.00"))
                    .build();
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configInvalida));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 1, new BigDecimal("10000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            // Act & Assert
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }
    }

    @Nested
    @DisplayName("Cálculo con Descuentos")
    class DescuentoTests {

        @Test
        @DisplayName("Debe restar descuento del subtotal y calcular impuestos sobre la base gravable restante")
        void testDescuento_Valido() {
            // Arrange: subtotal 100000.00, descuento 20000.00 -> base gravable 80000.00
            // IVA 19% sobre 80000.00 = 15200.00
            // Total neto: 80000.00 + 15200.00 = 95200.00
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionConIva));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Paquete Cumpleaños", 1, new BigDecimal("100000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item), null, new BigDecimal("20000.00"));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("100000.00"), response.subtotal());
            assertEquals(new BigDecimal("20000.00"), response.descuento());
            assertEquals(new BigDecimal("80000.00"), response.baseGravable());
            assertEquals(new BigDecimal("15200.00"), response.impuestos());
            assertEquals(new BigDecimal("95200.00"), response.totalNeto());
        }

        @Test
        @DisplayName("Debe permitir descuento del 100% resultando en base gravable y total en cero")
        void testDescuento_Total() {
            // Arrange
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionConIva));

            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Cortesía", 1, new BigDecimal("25000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item), null, new BigDecimal("25000.00"));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("25000.00"), response.subtotal());
            assertEquals(new BigDecimal("25000.00"), response.descuento());
            assertEquals(new BigDecimal("0.00"), response.baseGravable());
            assertEquals(new BigDecimal("0.00"), response.impuestos());
            assertEquals(new BigDecimal("0.00"), response.totalNeto());
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException si el descuento es negativo")
        void testDescuento_Negativo() {
            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 1, new BigDecimal("10000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item), null, new BigDecimal("-500.00"));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException si el descuento es superior al subtotal general")
        void testDescuento_SuperiorAlSubtotal() {
            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 1, new BigDecimal("10000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item), null, new BigDecimal("15000.00"));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }
    }

    @Nested
    @DisplayName("Cálculo Directo con Subtotales")
    class SubtotalesDirectosTests {

        @Test
        @DisplayName("Debe cotizar correctamente a partir de una lista directa de subtotales")
        void testSubtotalesDirectos_Exito() {
            // Arrange: subtotales [100.00, 200.50], descuento 50.00 -> base gravable 250.50
            // IVA 19% sobre 250.50 = 47.595 -> 47.60
            // Total neto: 250.50 + 47.60 = 298.10
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionConIva));

            CotizacionRequestDTO request = new CotizacionRequestDTO(
                    null,
                    List.of(new BigDecimal("100.00"), new BigDecimal("200.50")),
                    new BigDecimal("50.00")
            );

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("300.50"), response.subtotal());
            assertEquals(new BigDecimal("50.00"), response.descuento());
            assertEquals(new BigDecimal("250.50"), response.baseGravable());
            assertEquals(new BigDecimal("47.60"), response.impuestos());
            assertEquals(new BigDecimal("298.10"), response.totalNeto());
            assertNull(response.items());
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException si algún subtotal en la lista es negativo")
        void testSubtotalesDirectos_ElementoNegativo() {
            CotizacionRequestDTO request = new CotizacionRequestDTO(
                    null,
                    List.of(new BigDecimal("100.00"), new BigDecimal("-10.00")),
                    BigDecimal.ZERO
            );

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException si algún subtotal en la lista es nulo")
        void testSubtotalesDirectos_ElementoNulo() {
            List<BigDecimal> listaConNulo = new java.util.ArrayList<>();
            listaConNulo.add(new BigDecimal("100.00"));
            listaConNulo.add(null);

            CotizacionRequestDTO request = new CotizacionRequestDTO(null, listaConNulo, BigDecimal.ZERO);

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }
    }

    @Nested
    @DisplayName("Regla CA-04: Blindaje Contra Valores Nulos y Negativos")
    class ValidacionesCA04Tests {

        @Test
        @DisplayName("Debe rechazar solicitud nula")
        void testValidacion_RequestNulo() {
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(null));
        }

        @Test
        @DisplayName("Debe rechazar solicitud sin ítems ni subtotales")
        void testValidacion_SinItemsNiSubtotales() {
            CotizacionRequestDTO request = new CotizacionRequestDTO(null, null, BigDecimal.ZERO);
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));

            CotizacionRequestDTO request2 = new CotizacionRequestDTO(Collections.emptyList(), Collections.emptyList(), BigDecimal.ZERO);
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request2));
        }

        @Test
        @DisplayName("Debe rechazar ítem nulo en la lista")
        void testValidacion_ItemNulo() {
            List<ItemCotizacionDTO> items = new java.util.ArrayList<>();
            items.add(null);
            CotizacionRequestDTO request = new CotizacionRequestDTO(items);

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }

        @Test
        @DisplayName("Debe rechazar ítem con precioUnitario nulo")
        void testValidacion_PrecioUnitarioNulo() {
            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 1, null
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }

        @Test
        @DisplayName("Debe rechazar ítem con precioUnitario negativo")
        void testValidacion_PrecioUnitarioNegativo() {
            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 1, new BigDecimal("-1000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }

        @Test
        @DisplayName("Debe rechazar ítem con cantidad nula o menor a 1")
        void testValidacion_CantidadInvalida() {
            ItemCotizacionDTO itemCero = new ItemCotizacionDTO(
                    null, null, null, "Item", 0, new BigDecimal("1000.00")
            );
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(new CotizacionRequestDTO(List.of(itemCero))));

            ItemCotizacionDTO itemNegativo = new ItemCotizacionDTO(
                    null, null, null, "Item", -2, new BigDecimal("1000.00")
            );
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(new CotizacionRequestDTO(List.of(itemNegativo))));

            ItemCotizacionDTO itemNula = new ItemCotizacionDTO(
                    null, null, null, "Item", null, new BigDecimal("1000.00")
            );
            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(new CotizacionRequestDTO(List.of(itemNula))));
        }

        @Test
        @DisplayName("Debe rechazar ítem con subtotal negativo")
        void testValidacion_SubtotalItemNegativo() {
            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 1, new BigDecimal("1000.00"), new BigDecimal("-500.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }

        @Test
        @DisplayName("Debe rechazar ítem cuando el subtotal enviado difiere del cálculo precioUnitario * cantidad")
        void testValidacion_SubtotalItemInconsistente() {
            // enviado 5000.00, pero esperado 2 * 10000.00 = 20000.00
            ItemCotizacionDTO item = new ItemCotizacionDTO(
                    null, null, null, "Item", 2, new BigDecimal("10000.00"), new BigDecimal("5000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(item));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }
    }

    @Nested
    @DisplayName("Integración con AforoValidatorService")
    class IntegracionAforoValidatorTests {

        @Test
        @DisplayName("Debe delegar validación en AforoValidatorService cuando los ítems contienen tarifas de atracciones")
        void testDelegacionAforoValidator_Exito() {
            // Arrange
            when(configuracionParqueRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionConIva));

            ResultadoValidacionVentaDTO resultadoValidacion = new ResultadoValidacionVentaDTO(
                    true, new BigDecimal("30000.00"), 2, Map.of(1L, 18)
            );
            when(aforoValidatorService.validarItemsVenta(anyList())).thenReturn(resultadoValidacion);

            ItemCotizacionDTO itemAtraccion = new ItemCotizacionDTO(
                    1L, 10L, null, "Piscina de Pelotas - 15 min", 2, new BigDecimal("15000.00"), new BigDecimal("30000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(itemAtraccion));

            // Act
            CotizacionResponseDTO response = ventaCalculoService.cotizarVenta(request);

            // Assert
            assertNotNull(response);
            assertEquals(new BigDecimal("30000.00"), response.subtotal());
            assertEquals(new BigDecimal("35700.00"), response.totalNeto());
            verify(aforoValidatorService, times(1)).validarItemsVenta(anyList());
        }

        @Test
        @DisplayName("Debe propagar ReglaNegocioException lanzada por AforoValidatorService (ej. adulteración o sobrecupo)")
        void testDelegacionAforoValidator_FalloPropagado() {
            // Arrange
            when(aforoValidatorService.validarItemsVenta(anyList()))
                    .thenThrow(new ReglaNegocioException("Aforo máximo excedido para la atracción"));

            ItemCotizacionDTO itemAtraccion = new ItemCotizacionDTO(
                    1L, 10L, null, "Piscina de Pelotas - 15 min", 50, new BigDecimal("15000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(itemAtraccion));

            // Act & Assert
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> ventaCalculoService.cotizarVenta(request));
            assertTrue(ex.getMessage().contains("Aforo máximo excedido"));
        }

        @Test
        @DisplayName("Debe rechazar ítem con tarifa si atraccionId es nulo")
        void testTarifaSinAtraccionId() {
            ItemCotizacionDTO itemInvalido = new ItemCotizacionDTO(
                    null, 10L, null, "Tarifa huérfana", 1, new BigDecimal("15000.00")
            );
            CotizacionRequestDTO request = new CotizacionRequestDTO(List.of(itemInvalido));

            assertThrows(ReglaNegocioException.class, () -> ventaCalculoService.cotizarVenta(request));
        }
    }
}
