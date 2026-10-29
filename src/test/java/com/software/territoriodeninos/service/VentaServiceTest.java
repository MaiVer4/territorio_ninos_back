package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import com.software.territoriodeninos.util.NombreRol;
import com.software.territoriodeninos.util.TipoTarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaServiceTest - Pruebas unitarias para procesamiento transaccional de ventas y liquidación de turnos")
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MenorRepository menorRepository;

    @Mock
    private AtraccionRepository atraccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VentaCalculoService ventaCalculoService;

    @Mock
    private AtraccionService atraccionService;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private Usuario cajero;
    private Cliente cliente;
    private Menor menor;
    private Atraccion atraccion;
    private TurnoCaja turnoAbierto;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        Rol rolCajero = Rol.builder().id(2L).nombre(NombreRol.TAQUILLERO).build();
        cajero = Usuario.builder()
                .id(10L)
                .nombre("Cajero Juan")
                .correo("cajero@territoriodeninos.com")
                .rol(rolCajero)
                .build();

        cliente = Cliente.builder()
                .id(50L)
                .nombres("Carlos")
                .apellidos("Gómez")
                .numeroDocumento("1020304050")
                .correo("carlos@gmail.com")
                .build();

        menor = Menor.builder()
                .id(100L)
                .nombreCompleto("Mateo Gómez")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .cliente(cliente)
                .build();

        atraccion = Atraccion.builder()
                .id(1L)
                .nombre("Camas Elásticas")
                .duracionMinutosBase(15)
                .capacidadMaxima(10)
                .build();

        turnoAbierto = TurnoCaja.builder()
                .id(200L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("100000.00"))
                .ventasEfectivoSistema(new BigDecimal("0.00"))
                .estado(EstadoTurnoCaja.ABIERTO)
                .fechaApertura(LocalDateTime.now())
                .build();

        auth = new UsernamePasswordAuthenticationToken(
                "cajero@territoriodeninos.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TAQUILLERO"))
        );
    }

    @Test
    @DisplayName("procesarVenta - Venta en efectivo exitosa actualiza turno e invalida catálogo")
    void procesarVenta_conEfectivo_exitoso() {
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                1L, 100L, TipoTarifa.TIEMPO, 1L, null, 2, new BigDecimal("15000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                50L, 200L, MetodoPago.EFECTIVO, new BigDecimal("40000.00"), BigDecimal.ZERO, "Nota venta", List.of(itemDTO)
        );

        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findById(200L)).thenReturn(Optional.of(turnoAbierto));
        when(clienteRepository.findById(50L)).thenReturn(Optional.of(cliente));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccion));
        when(menorRepository.findById(100L)).thenReturn(Optional.of(menor));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("30000.00")),
                List.of(new ItemCotizacionDTO(1L, 1L, null, "Camas Elásticas", 2, new BigDecimal("15000.00"), new BigDecimal("30000.00"))),
                new BigDecimal("30000.00"),
                BigDecimal.ZERO,
                new BigDecimal("30000.00"),
                new BigDecimal("19.00"),
                new BigDecimal("5700.00"),
                new BigDecimal("35700.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);

        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(999L);
            v.setFechaVenta(LocalDateTime.now());
            return v;
        });

        VentaResponseDTO response = ventaService.procesarVenta(request, auth);

        assertNotNull(response);
        assertEquals(999L, response.id());
        assertEquals(new BigDecimal("35700.00"), response.montoTotal());
        assertEquals(new BigDecimal("40000.00"), response.montoRecibido());
        assertEquals(new BigDecimal("4300.00"), response.cambio());
        assertEquals(MetodoPago.EFECTIVO, response.metodoPago());
        assertEquals(EstadoVenta.COMPLETADA, response.estadoVenta());

        // Verificar incremento en turno de caja
        assertEquals(new BigDecimal("35700.00"), turnoAbierto.getVentasEfectivoSistema());
        verify(turnoCajaRepository).save(turnoAbierto);
        verify(atraccionService).invalidarCacheCatalogo();
    }

    @Test
    @DisplayName("procesarVenta - Venta con tarjeta débito no incrementa ventas en efectivo")
    void procesarVenta_conTarjetaDebito_exitoso() {
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                1L, null, TipoTarifa.TIEMPO, 1L, null, 1, new BigDecimal("20000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                50L, 200L, MetodoPago.TARJETA_DEBITO, new BigDecimal("20000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findById(200L)).thenReturn(Optional.of(turnoAbierto));
        when(clienteRepository.findById(50L)).thenReturn(Optional.of(cliente));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccion));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("20000.00")),
                List.of(),
                new BigDecimal("20000.00"),
                BigDecimal.ZERO,
                new BigDecimal("20000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);

        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(888L);
            v.setFechaVenta(LocalDateTime.now());
            return v;
        });

        VentaResponseDTO response = ventaService.procesarVenta(request, auth);

        assertNotNull(response);
        assertEquals(new BigDecimal("20000.00"), response.montoTotal());
        assertEquals(BigDecimal.ZERO.setScale(2), response.cambio());
        assertEquals(new BigDecimal("0.00"), turnoAbierto.getVentasEfectivoSistema());
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("procesarVenta - Lanza ReglaNegocioException si el efectivo es menor al total neto")
    void procesarVenta_efectivoInsuficiente_debeLanzarExcepcion() {
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                1L, null, TipoTarifa.TIEMPO, 1L, null, 1, new BigDecimal("10000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                50L, 200L, MetodoPago.EFECTIVO, new BigDecimal("5000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findById(200L)).thenReturn(Optional.of(turnoAbierto));
        when(clienteRepository.findById(50L)).thenReturn(Optional.of(cliente));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccion));

        CotizacionResponseDTO cotizacionMock = new CotizacionResponseDTO(
                List.of(new BigDecimal("10000.00")),
                List.of(),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10000.00")
        );
        when(ventaCalculoService.cotizarVenta(any(CotizacionRequestDTO.class))).thenReturn(cotizacionMock);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ventaService.procesarVenta(request, auth));
        assertTrue(ex.getMessage().contains("insuficiente para cubrir el total"));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("procesarVenta - Lanza ConflictoException si no hay turno abierto para el cajero")
    void procesarVenta_sinTurnoAbierto_debeLanzarConflictoException() {
        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                1L, null, TipoTarifa.TIEMPO, 1L, null, 1, new BigDecimal("10000.00")
        );
        // turnoCajaId null -> busca por cajero
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                50L, null, MetodoPago.EFECTIVO, new BigDecimal("10000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.empty());

        assertThrows(ConflictoException.class, () -> ventaService.procesarVenta(request, auth));
    }

    @Test
    @DisplayName("procesarVenta - Lanza ReglaNegocioException si el menor no pertenece al cliente")
    void procesarVenta_menorNoPerteneceACliente_debeLanzarExcepcion() {
        Cliente otroCliente = Cliente.builder().id(999L).build();
        Menor menorAjeno = Menor.builder().id(100L).nombreCompleto("Pedrito").cliente(otroCliente).build();

        DetalleVentaRequestDTO itemDTO = new DetalleVentaRequestDTO(
                1L, 100L, TipoTarifa.TIEMPO, 1L, null, 1, new BigDecimal("10000.00")
        );
        VentaProcesarRequestDTO request = new VentaProcesarRequestDTO(
                50L, 200L, MetodoPago.EFECTIVO, new BigDecimal("10000.00"), BigDecimal.ZERO, null, List.of(itemDTO)
        );

        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findById(200L)).thenReturn(Optional.of(turnoAbierto));
        when(clienteRepository.findById(50L)).thenReturn(Optional.of(cliente));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccion));
        when(menorRepository.findById(100L)).thenReturn(Optional.of(menorAjeno));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ventaService.procesarVenta(request, auth));
        assertTrue(ex.getMessage().contains("no se encuentra vinculado al cliente titular"));
    }

    @Test
    @DisplayName("obtenerVentaPorId - Retorna DTO cuando existe")
    void obtenerVentaPorId_existente() {
        Venta venta = Venta.builder()
                .id(1L)
                .montoTotal(new BigDecimal("25000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estadoVenta(EstadoVenta.COMPLETADA)
                .fechaVenta(LocalDateTime.now())
                .build();

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        VentaResponseDTO dto = ventaService.obtenerVentaPorId(1L);
        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals(new BigDecimal("25000.00"), dto.montoTotal());
    }

    @Test
    @DisplayName("obtenerVentaPorId - Lanza RecursoNoEncontradoException cuando no existe")
    void obtenerVentaPorId_noExiste_debeLanzarExcepcion() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> ventaService.obtenerVentaPorId(99L));
    }

    @Test
    @DisplayName("listarVentasPorTurno - Taquillero bloqueado de consultar ventas de otro turno")
    void listarVentasPorTurno_otroCajero_debeLanzarAccessDeniedException() {
        TurnoCaja turnoOtroCajero = TurnoCaja.builder().id(300L).usuarioId(999L).build();

        when(usuarioRepository.findByCorreo("cajero@territoriodeninos.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.findById(300L)).thenReturn(Optional.of(turnoOtroCajero));

        assertThrows(AccessDeniedException.class, () -> ventaService.listarVentasPorTurno(300L, auth));
    }
}
