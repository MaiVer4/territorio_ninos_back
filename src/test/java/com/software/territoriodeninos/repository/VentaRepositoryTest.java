package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("VentaRepositoryTest - Pruebas de integración con base de datos para Venta y DetalleVenta")
class VentaRepositoryTest {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    @Autowired
    private AtraccionRepository atraccionRepository;

    private Cliente cliente;
    private TurnoCaja turnoCaja;
    private Atraccion atraccion;
    private Menor menor;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .numeroDocumento("CC" + System.currentTimeMillis())
                .nombres("Carlos")
                .apellidos("Gómez")
                .telefono("3101234567")
                .correo("carlos.gomez" + System.currentTimeMillis() + "@test.com")
                .build();

        menor = Menor.builder()
                .numeroDocumento("RC" + System.currentTimeMillis())
                .nombreCompleto("Juanito Gómez")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .build();
        cliente.addMenor(menor);
        cliente = clienteRepository.save(cliente);
        menor = cliente.getMenores().get(0);

        turnoCaja = TurnoCaja.builder()
                .usuarioId(1L)
                .montoBase(new BigDecimal("100000.00"))
                .fechaApertura(LocalDateTime.now())
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        turnoCaja = turnoCajaRepository.save(turnoCaja);

        atraccion = Atraccion.builder()
                .nombre("Piscina de Pelotas Test " + System.currentTimeMillis())
                .tarifaMinuto(new BigDecimal("1000.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .tipo(TipoAtraccion.TEMPORIZADA)
                .capacidadMaxima(20)
                .build();
        atraccion = atraccionRepository.save(atraccion);
    }

    @Test
    @DisplayName("Debe persistir una venta con sus detalles en cascada y calcular subtotal automáticamente")
    void testGuardarVentaConDetalles() {
        Venta venta = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("30000.00"))
                .subtotal(new BigDecimal("30000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .montoRecibido(new BigDecimal("50000.00"))
                .cambio(new BigDecimal("20000.00"))
                .build();

        DetalleVenta detalle1 = DetalleVenta.builder()
                .atraccion(atraccion)
                .menor(menor)
                .tipoTarifa(TipoTarifa.TIEMPO)
                .cantidad(2)
                .precioUnitario(new BigDecimal("15000.00"))
                // subtotal se omite para probar cálculo automático en @PrePersist
                .build();

        venta.addDetalle(detalle1);
        Venta guardada = ventaRepository.save(venta);

        assertNotNull(guardada.getId());
        assertEquals(EstadoVenta.COMPLETADA, guardada.getEstadoVenta());
        assertNotNull(guardada.getFechaVenta());
        assertEquals(1, guardada.getDetalles().size());

        DetalleVenta detalleGuardado = guardada.getDetalles().get(0);
        assertNotNull(detalleGuardado.getId());
        assertEquals(new BigDecimal("30000.00"), detalleGuardado.getSubtotal());
    }

    @Test
    @DisplayName("Debe consultar histórico de ventas por turno de caja ordenadas descendentemente")
    void testConsultarHistoricoPorTurnoCaja() {
        Venta v1 = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("15000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.COMPLETADA)
                .fechaVenta(LocalDateTime.now().minusMinutes(10))
                .build();

        Venta v2 = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("25000.00"))
                .metodoPago(MetodoPago.TARJETA)
                .estadoVenta(EstadoVenta.COMPLETADA)
                .fechaVenta(LocalDateTime.now())
                .build();

        ventaRepository.save(v1);
        ventaRepository.save(v2);

        List<Venta> historico = ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(turnoCaja.getId());
        assertEquals(2, historico.size());
        assertTrue(historico.get(0).getFechaVenta().isAfter(historico.get(1).getFechaVenta()) ||
                historico.get(0).getFechaVenta().isEqual(historico.get(1).getFechaVenta()));
    }

    @Test
    @DisplayName("Debe sumar el total vendido por turno de caja y método de pago")
    void testSumMontoTotalPorTurnoYMetodoPago() {
        Venta v1 = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("20000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.COMPLETADA)
                .build();

        Venta v2 = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("35000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.COMPLETADA)
                .build();

        Venta v3 = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("50000.00"))
                .metodoPago(MetodoPago.TARJETA)
                .estadoVenta(EstadoVenta.COMPLETADA)
                .build();

        ventaRepository.save(v1);
        ventaRepository.save(v2);
        ventaRepository.save(v3);

        BigDecimal totalEfectivo = ventaRepository.sumMontoTotalByTurnoCajaAndMetodoPagoAndEstado(
                turnoCaja.getId(), MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA
        );
        assertEquals(new BigDecimal("55000.00"), totalEfectivo);

        BigDecimal totalTarjeta = ventaRepository.sumMontoTotalByTurnoCajaAndMetodoPagoAndEstado(
                turnoCaja.getId(), MetodoPago.TARJETA, EstadoVenta.COMPLETADA
        );
        assertEquals(new BigDecimal("50000.00"), totalTarjeta);
    }

    @Test
    @DisplayName("Debe consultar histórico de detalles de venta por turno de caja y por atracción")
    void testConsultasDetalleVentaRepository() {
        Venta venta = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(new BigDecimal("40000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .build();

        DetalleVenta detalle = DetalleVenta.builder()
                .atraccion(atraccion)
                .menor(menor)
                .tipoTarifa(TipoTarifa.TIEMPO)
                .cantidad(2)
                .precioUnitario(new BigDecimal("20000.00"))
                .subtotal(new BigDecimal("40000.00"))
                .build();

        venta.addDetalle(detalle);
        Venta guardada = ventaRepository.save(venta);

        List<DetalleVenta> porVenta = detalleVentaRepository.findByVentaId(guardada.getId());
        assertEquals(1, porVenta.size());

        List<DetalleVenta> porTurno = detalleVentaRepository.findByVentaTurnoCajaIdOrderByVentaFechaVentaDesc(turnoCaja.getId());
        assertEquals(1, porTurno.size());

        List<DetalleVenta> porAtraccion = detalleVentaRepository.findByAtraccionId(atraccion.getId());
        assertEquals(1, porAtraccion.size());

        List<DetalleVenta> porMenor = detalleVentaRepository.findByMenorIdOrderByVentaFechaVentaDesc(menor.getId());
        assertEquals(1, porMenor.size());
    }
}
