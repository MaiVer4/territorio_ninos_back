package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.entity.ConfiguracionParque;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfiguracionParqueServiceImpl - Tests unitarios")
class ConfiguracionParqueServiceTest {

    @Mock
    private ConfiguracionParqueRepository repository;

    @InjectMocks
    private ConfiguracionParqueServiceImpl configuracionParqueService;

    private ConfiguracionParque configuracionExistente;

    @BeforeEach
    void setUp() {
        configuracionExistente = ConfiguracionParque.builder()
                .id(1L)
                .nombreParque("Territorio de Niños")
                .nit("900.123.456-7")
                .direccion("Centro Comercial Parque Central, Local 201")
                .telefono("+57 300 123 4567")
                .pieTicketLegal("Boleto válido únicamente para el día de emisión.")
                .tiempoGraciaGeneralSegundos(60)
                .aplicaImpuesto(true)
                .porcentajeImpuesto(new BigDecimal("19.00"))
                .build();
    }

    @Nested
    @DisplayName("Obtener Configuración del Parque")
    class ObtenerConfiguracionTests {

        @Test
        @DisplayName("Debe retornar la configuración existente cuando ya se encuentra registrada en base de datos")
        void testObtenerConfiguracion_Existente() {
            // Arrange
            when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionExistente));

            // Act
            ConfiguracionParqueDTO resultado = configuracionParqueService.obtenerConfiguracion();

            // Assert
            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Territorio de Niños", resultado.getNombreParque());
            assertEquals("900.123.456-7", resultado.getNit());
            assertEquals("Centro Comercial Parque Central, Local 201", resultado.getDireccion());
            assertEquals("+57 300 123 4567", resultado.getTelefono());
            assertEquals("Boleto válido únicamente para el día de emisión.", resultado.getPieTicketLegal());
            assertEquals(60, resultado.getTiempoGraciaGeneralSegundos());
            assertTrue(resultado.getAplicaImpuesto());
            assertEquals(new BigDecimal("19.00"), resultado.getPorcentajeImpuesto());
            verify(repository, times(1)).findFirstByOrderByIdAsc();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe crear y retornar configuración por defecto cuando no existe registro previo en base de datos")
        void testObtenerConfiguracion_CreaPorDefecto() {
            // Arrange
            when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
            when(repository.save(any(ConfiguracionParque.class))).thenAnswer(invocation -> {
                ConfiguracionParque c = invocation.getArgument(0);
                c.setId(1L);
                return c;
            });

            // Act
            ConfiguracionParqueDTO resultado = configuracionParqueService.obtenerConfiguracion();

            // Assert
            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Territorio de Niños", resultado.getNombreParque());
            assertEquals("Territorio de Niños", resultado.getNombreComercial());
            assertEquals("901.458.789-2", resultado.getNit());
            assertEquals(60, resultado.getTiempoGraciaGeneralSegundos());
            assertNotNull(resultado.getPieTicketLegal());
            assertTrue(resultado.getAplicaImpuesto());
            assertEquals(new BigDecimal("0.00"), resultado.getPorcentajeImpuesto());
            verify(repository, times(1)).findFirstByOrderByIdAsc();
            verify(repository, times(1)).save(any(ConfiguracionParque.class));
        }
    }

    @Nested
    @DisplayName("Actualizar Configuración del Parque")
    class ActualizarConfiguracionTests {

        @Test
        @DisplayName("Debe actualizar los datos de la configuración existente y retornar el DTO actualizado")
        void testActualizarConfiguracion_Exito() {
            // Arrange
            ConfiguracionParqueDTO actualizacionDTO = ConfiguracionParqueDTO.builder()
                    .nombreParque("Territorio de Niños Actualizado")
                    .nit("800.999.888-1")
                    .direccion("Avenida Siempre Viva 123")
                    .telefono("+57 311 000 0000")
                    .pieTicketLegal("Nuevo pie de página legal")
                    .tiempoGraciaGeneralSegundos(120)
                    .aplicaImpuesto(true)
                    .porcentajeImpuesto(new BigDecimal("8.00"))
                    .build();

            when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionExistente));
            when(repository.save(any(ConfiguracionParque.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ConfiguracionParqueDTO resultado = configuracionParqueService.actualizarConfiguracion(actualizacionDTO);

            // Assert
            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Territorio de Niños Actualizado", resultado.getNombreParque());
            assertEquals("800.999.888-1", resultado.getNit());
            assertEquals("Avenida Siempre Viva 123", resultado.getDireccion());
            assertEquals("+57 311 000 0000", resultado.getTelefono());
            assertEquals("Nuevo pie de página legal", resultado.getPieTicketLegal());
            assertEquals(120, resultado.getTiempoGraciaGeneralSegundos());
            assertTrue(resultado.getAplicaImpuesto());
            assertEquals(new BigDecimal("8.00"), resultado.getPorcentajeImpuesto());
            verify(repository, times(1)).save(configuracionExistente);
        }

        @Test
        @DisplayName("Debe crear la configuración por defecto antes de aplicar los cambios si no existía previa")
        void testActualizarConfiguracion_CuandoNoExistePrevia() {
            // Arrange
            ConfiguracionParqueDTO actualizacionDTO = ConfiguracionParqueDTO.builder()
                    .nombreParque("Nuevo Parque")
                    .nit("111.222.333-4")
                    .direccion("Calle 1 # 2-3")
                    .telefono("3001112233")
                    .pieTicketLegal("Condiciones generales")
                    .tiempoGraciaGeneralSegundos(90)
                    .aplicaImpuesto(false)
                    .porcentajeImpuesto(new BigDecimal("0.00"))
                    .build();

            when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
            // Primer save crea default, segundo save guarda la modificación
            when(repository.save(any(ConfiguracionParque.class))).thenAnswer(invocation -> {
                ConfiguracionParque c = invocation.getArgument(0);
                if (c.getId() == null) {
                    c.setId(1L);
                }
                return c;
            });

            // Act
            ConfiguracionParqueDTO resultado = configuracionParqueService.actualizarConfiguracion(actualizacionDTO);

            // Assert
            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Nuevo Parque", resultado.getNombreParque());
            assertEquals("111.222.333-4", resultado.getNit());
            assertEquals(90, resultado.getTiempoGraciaGeneralSegundos());
            assertFalse(resultado.getAplicaImpuesto());
            assertEquals(new BigDecimal("0.00"), resultado.getPorcentajeImpuesto());
            verify(repository, times(2)).save(any(ConfiguracionParque.class));
        }
    }
}
