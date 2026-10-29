package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.software.territoriodeninos.util.MetodoPago;
import com.software.territoriodeninos.util.TipoTarifa;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VentaProcesarRequestDTOTest - Pruebas unitarias de validación y deserialización JSON")
class VentaProcesarRequestDTOTest {

    private Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Debe validar exitosamente cuando todos los campos son válidos")
    void testValidacionExitosa() {
        DetalleVentaRequestDTO detalle = new DetalleVentaRequestDTO(
                1L, 5L, TipoTarifa.TIEMPO, 10L, null, 2, new BigDecimal("15000.00"), new BigDecimal("30000.00")
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, 2L, MetodoPago.EFECTIVO, new BigDecimal("50000.00"), BigDecimal.ZERO, "Nota venta", List.of(detalle)
        );

        Set<ConstraintViolation<VentaProcesarRequestDTO>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Debe fallar cuando clienteId, metodoPago o montoRecibido son nulos")
    void testCamposObligatoriosNulos() {
        DetalleVentaRequestDTO detalle = new DetalleVentaRequestDTO(
                1L, null, TipoTarifa.TIEMPO, 10L, null, 1, new BigDecimal("15000.00"), null
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                null, null, null, null, null, null, List.of(detalle)
        );

        Set<ConstraintViolation<VentaProcesarRequestDTO>> violations = validator.validate(request);
        assertEquals(3, violations.size());
    }

    @Test
    @DisplayName("Debe fallar cuando la lista de ítems está vacía")
    void testItemsVacios() {
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, null, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, Collections.emptyList()
        );

        Set<ConstraintViolation<VentaProcesarRequestDTO>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("al menos un ítem"));
    }

    @Test
    @DisplayName("Debe fallar cuando el montoRecibido o el descuento son negativos")
    void testMontosNegativos() {
        DetalleVentaRequestDTO detalle = new DetalleVentaRequestDTO(
                1L, null, TipoTarifa.TIEMPO, 10L, null, 1, new BigDecimal("15000.00"), null
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, null, MetodoPago.EFECTIVO, new BigDecimal("-1000.00"), new BigDecimal("-500.00"), null, List.of(detalle)
        );

        Set<ConstraintViolation<VentaProcesarRequestDTO>> violations = validator.validate(request);
        assertEquals(2, violations.size());
    }

    @Test
    @DisplayName("Debe fallar cuando un ítem tiene cantidad menor a 1 o precioUnitario negativo")
    void testItemInvalido() {
        DetalleVentaRequestDTO detalleInvalido = new DetalleVentaRequestDTO(
                null, null, null, null, null, 0, new BigDecimal("-10.00"), null
        );

        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                1L, null, MetodoPago.EFECTIVO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(detalleInvalido)
        );

        Set<ConstraintViolation<VentaProcesarRequestDTO>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Debe deserializar correctamente un JSON con alias y enums en español o inglés")
    void testDeserializacionJsonConAliases() throws Exception {
        String json = """
                {
                    "cliente_id": 10,
                    "turno_caja_id": 2,
                    "metodo_pago": "CASH",
                    "monto_recibido": 60000.00,
                    "descuento": 5000.00,
                    "observaciones": "Pago en efectivo",
                    "detalles": [
                        {
                            "atraccion_id": 1,
                            "menor_id": 3,
                            "tipo_tarifa": "TIEMPO",
                            "tarifa_tiempo_id": 100,
                            "cantidad": 2,
                            "precio_unitario": 25000.00,
                            "subtotal": 50000.00
                        }
                    ]
                }
                """;

        VentaProcesarRequestDTO dto = objectMapper.readValue(json, VentaProcesarRequestDTO.class);

        assertNotNull(dto);
        assertEquals(10L, dto.clienteId());
        assertEquals(2L, dto.turnoCajaId());
        assertEquals(MetodoPago.EFECTIVO, dto.metodoPago());
        assertEquals(0, new BigDecimal("60000.00").compareTo(dto.montoRecibido()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(dto.descuento()));
        assertEquals("Pago en efectivo", dto.observaciones());
        assertEquals(1, dto.items().size());

        DetalleVentaRequestDTO item = dto.items().get(0);
        assertEquals(1L, item.atraccionId());
        assertEquals(3L, item.menorId());
        assertEquals(TipoTarifa.TIEMPO, item.tipoTarifa());
        assertEquals(100L, item.tarifaTiempoId());
        assertEquals(2, item.cantidad());
        assertEquals(0, new BigDecimal("25000.00").compareTo(item.precioUnitario()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(item.subtotal()));
    }
}
