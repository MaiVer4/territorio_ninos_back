package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.exception.TurnoConflictoException;
import com.software.territoriodeninos.repository.TurnoCajaRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.EstadoUsuario;
import com.software.territoriodeninos.util.NombreRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoCajaServiceTest - Pruebas unitarias de TurnoCajaService")
class TurnoCajaServiceTest {

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TurnoCajaServiceImpl turnoCajaService;

    private Usuario taquillero;
    private Usuario administrador;
    private Usuario operario;

    @BeforeEach
    void setUp() {
        Rol rolTaquillero = Rol.builder().id(1L).nombre(NombreRol.TAQUILLERO).build();
        Rol rolAdmin = Rol.builder().id(2L).nombre(NombreRol.ADMINISTRADOR).build();
        Rol rolOperario = Rol.builder().id(3L).nombre(NombreRol.OPERARIO).build();

        taquillero = Usuario.builder()
                .id(10L)
                .nombre("Cajero Pedro")
                .correo("pedro@test.com")
                .rol(rolTaquillero)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        administrador = Usuario.builder()
                .id(20L)
                .nombre("Admin Ana")
                .correo("ana@test.com")
                .rol(rolAdmin)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        operario = Usuario.builder()
                .id(30L)
                .nombre("Operario Carlos")
                .correo("carlos@test.com")
                .rol(rolOperario)
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("abrirTurno() - Éxito para TAQUILLERO")
    void testAbrirTurno_Exito_Taquillero() {
        AperturaCajaDTO dto = new AperturaCajaDTO(new BigDecimal("50000.00"), "Apertura turno mañana");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));
        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.empty());

        TurnoCaja turnoGuardado = TurnoCaja.builder()
                .id(100L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(LocalDateTime.now())
                .ventasEfectivoSistema(BigDecimal.ZERO)
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones("Apertura turno mañana")
                .build();
        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenReturn(turnoGuardado);

        TurnoCajaResponseDTO response = turnoCajaService.abrirTurno(dto, authentication);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L, response.usuarioId());
        assertEquals(new BigDecimal("50000.00"), response.montoBase());
        assertEquals(EstadoTurnoCaja.ABIERTO, response.estado());
        assertEquals("Apertura turno mañana", response.observaciones());
        assertEquals("Cajero Pedro", response.cajeroNombre());

        verify(turnoCajaRepository).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("abrirTurno() - Lanza TurnoConflictoException (HTTP 409) si ya existe turno ABIERTO")
    void testAbrirTurno_Conflicto_TurnoYaAbierto() {
        AperturaCajaDTO dto = new AperturaCajaDTO(new BigDecimal("50000.00"), "Nuevo turno");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));

        TurnoCaja turnoExistente = TurnoCaja.builder()
                .id(99L)
                .usuarioId(10L)
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoExistente));

        assertThrows(TurnoConflictoException.class, () -> turnoCajaService.abrirTurno(dto, authentication));
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("abrirTurno() - Lanza AccessDeniedException si el usuario tiene rol OPERARIO")
    void testAbrirTurno_AccessDenied_RolOperario() {
        AperturaCajaDTO dto = new AperturaCajaDTO(new BigDecimal("50000.00"), "Nuevo turno");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@test.com");
        when(authentication.getName()).thenReturn("carlos@test.com");

        when(usuarioRepository.findByCorreo("carlos@test.com")).thenReturn(Optional.of(operario));

        assertThrows(AccessDeniedException.class, () -> turnoCajaService.abrirTurno(dto, authentication));
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("obtenerTurnoActivo() - Retorna Optional con DTO si existe turno activo")
    void testObtenerTurnoActivo_Existe() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));

        TurnoCaja turnoActivo = TurnoCaja.builder()
                .id(100L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(LocalDateTime.now().minusHours(2))
                .ventasEfectivoSistema(new BigDecimal("15000.00"))
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones("Turno activo")
                .build();

        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoActivo));

        Optional<TurnoCajaResponseDTO> resultado = turnoCajaService.obtenerTurnoActivo(authentication);

        assertTrue(resultado.isPresent());
        assertEquals(100L, resultado.get().id());
        assertEquals(EstadoTurnoCaja.ABIERTO, resultado.get().estado());
        assertEquals("Cajero Pedro", resultado.get().cajeroNombre());
    }

    // --- PRUEBAS DE CIERRE DE CAJA (CA-02 y CA-04) ---

    @Test
    @DisplayName("cerrarTurno() - Éxito con arqueo balanceado exacto (diferencia == 0, CA-02)")
    void testCerrarTurno_Exito_BalanceExacto() {
        CierreCajaDTO dto = new CierreCajaDTO(new BigDecimal("150000.00"), null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));

        TurnoCaja turnoActivo = TurnoCaja.builder()
                .id(100L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .ventasEfectivoSistema(new BigDecimal("100000.00"))
                .fechaApertura(LocalDateTime.now().minusHours(4))
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones("Apertura mañana")
                .build();

        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoActivo));
        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TurnoCajaResponseDTO response = turnoCajaService.cerrarTurno(dto, authentication);

        assertNotNull(response);
        assertEquals(EstadoTurnoCaja.CERRADO, response.estado());
        assertEquals(new BigDecimal("150000.00"), response.montoEfectivoReportado());
        assertEquals(new BigDecimal("150000.00"), response.totalEsperado());
        assertEquals(0, response.diferencia().compareTo(BigDecimal.ZERO));
        assertNotNull(response.fechaCierre());

        verify(turnoCajaRepository).save(argThat(t ->
                t.getEstado() == EstadoTurnoCaja.CERRADO &&
                t.getDiferencia().compareTo(BigDecimal.ZERO) == 0 &&
                t.getMontoEfectivoReportado().compareTo(new BigDecimal("150000.00")) == 0
        ));
    }

    @Test
    @DisplayName("cerrarTurno() - Éxito con sobrante justificado (diferencia > 0, CA-02 y CA-04)")
    void testCerrarTurno_Exito_SobranteConJustificacion() {
        CierreCajaDTO dto = new CierreCajaDTO(new BigDecimal("160000.00"), "Sobrante por propina voluntaria");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));

        TurnoCaja turnoActivo = TurnoCaja.builder()
                .id(100L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .ventasEfectivoSistema(new BigDecimal("100000.00"))
                .fechaApertura(LocalDateTime.now().minusHours(4))
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones("Apertura mañana")
                .build();

        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoActivo));
        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TurnoCajaResponseDTO response = turnoCajaService.cerrarTurno(dto, authentication);

        assertNotNull(response);
        assertEquals(EstadoTurnoCaja.CERRADO, response.estado());
        assertEquals(new BigDecimal("10000.00"), response.diferencia());
        assertTrue(response.observaciones().contains("Sobrante por propina voluntaria"));
    }

    @Test
    @DisplayName("cerrarTurno() - Éxito con faltante justificado (diferencia < 0, CA-02 y CA-04)")
    void testCerrarTurno_Exito_FaltanteConJustificacion() {
        CierreCajaDTO dto = new CierreCajaDTO(new BigDecimal("140000.00"), "Faltante por billete dañado");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));

        TurnoCaja turnoActivo = TurnoCaja.builder()
                .id(100L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .ventasEfectivoSistema(new BigDecimal("100000.00"))
                .fechaApertura(LocalDateTime.now().minusHours(4))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoActivo));
        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TurnoCajaResponseDTO response = turnoCajaService.cerrarTurno(dto, authentication);

        assertNotNull(response);
        assertEquals(EstadoTurnoCaja.CERRADO, response.estado());
        assertEquals(new BigDecimal("-10000.00"), response.diferencia());
        assertTrue(response.observaciones().contains("Faltante por billete dañado"));
    }

    @Test
    @DisplayName("cerrarTurno() - Lanza ReglaNegocioException (HTTP 400) si diferencia != 0 y no hay observaciones (CA-04)")
    void testCerrarTurno_DiferenciaSinObservaciones_LanzaExcepcion() {
        // totalEsperado = 150000, reportado = 140000 -> diferencia = -10000
        CierreCajaDTO dtoSinObs = new CierreCajaDTO(new BigDecimal("140000.00"), "   ");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));

        TurnoCaja turnoActivo = TurnoCaja.builder()
                .id(100L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .ventasEfectivoSistema(new BigDecimal("100000.00"))
                .fechaApertura(LocalDateTime.now().minusHours(4))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.of(turnoActivo));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> turnoCajaService.cerrarTurno(dtoSinObs, authentication));

        assertTrue(ex.getMessage().contains("faltante"));
        assertTrue(ex.getMessage().contains("obligatorio"));
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("cerrarTurno() - Lanza RecursoNoEncontradoException si no hay turno ABIERTO")
    void testCerrarTurno_SinTurnoAbierto_LanzaExcepcion() {
        CierreCajaDTO dto = new CierreCajaDTO(new BigDecimal("150000.00"), null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("pedro@test.com");
        when(authentication.getName()).thenReturn("pedro@test.com");

        when(usuarioRepository.findByCorreo("pedro@test.com")).thenReturn(Optional.of(taquillero));
        when(turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(10L, EstadoTurnoCaja.ABIERTO))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> turnoCajaService.cerrarTurno(dto, authentication));
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("cerrarTurno() - Lanza AccessDeniedException para rol OPERARIO")
    void testCerrarTurno_RolOperario_AccessDenied() {
        CierreCajaDTO dto = new CierreCajaDTO(new BigDecimal("150000.00"), null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("carlos@test.com");
        when(authentication.getName()).thenReturn("carlos@test.com");

        when(usuarioRepository.findByCorreo("carlos@test.com")).thenReturn(Optional.of(operario));

        assertThrows(AccessDeniedException.class, () -> turnoCajaService.cerrarTurno(dto, authentication));
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    @DisplayName("obtenerHistorialTurnos() - Sin filtro de fecha retorna todos y mapea nombres con fallback")
    void testObtenerHistorialTurnos_SinFecha_RetornaTodosConNombres() {
        TurnoCaja t1 = TurnoCaja.builder()
                .id(1L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(LocalDateTime.now().minusHours(2))
                .estado(EstadoTurnoCaja.CERRADO)
                .build();
        TurnoCaja t2 = TurnoCaja.builder()
                .id(2L)
                .usuarioId(99L)
                .montoBase(new BigDecimal("30000.00"))
                .fechaApertura(LocalDateTime.now())
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        when(turnoCajaRepository.findAllByOrderByFechaAperturaDesc()).thenReturn(List.of(t2, t1));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(taquillero)); // Solo taquillero (id 10) existe

        List<TurnoCajaResponseDTO> resultado = turnoCajaService.obtenerHistorialTurnos(null);

        assertEquals(2, resultado.size());
        assertEquals("Cajero #99", resultado.get(0).cajeroNombre());
        assertEquals("Cajero #99", resultado.get(0).nombreCajero());
        assertEquals(2L, resultado.get(0).idTurno());
        assertEquals("Cajero Pedro", resultado.get(1).cajeroNombre());
        assertEquals("Cajero Pedro", resultado.get(1).nombreCajero());
        assertEquals(10L, resultado.get(1).idUsuario());
        verify(turnoCajaRepository).findAllByOrderByFechaAperturaDesc();
    }

    @Test
    @DisplayName("obtenerHistorialTurnos() - Con filtro de fecha consulta rango de fecha inicio a fin")
    void testObtenerHistorialTurnos_ConFecha_ConsultaRango() {
        String fechaStr = "2026-10-22";
        TurnoCaja t1 = TurnoCaja.builder()
                .id(1L)
                .usuarioId(10L)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(LocalDateTime.of(2026, 10, 22, 9, 0))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        when(turnoCajaRepository.findByFechaAperturaBetweenOrderByFechaAperturaDesc(any(), any()))
                .thenReturn(List.of(t1));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(taquillero));

        List<TurnoCajaResponseDTO> resultado = turnoCajaService.obtenerHistorialTurnos(fechaStr);

        assertEquals(1, resultado.size());
        assertEquals("Cajero Pedro", resultado.get(0).nombreCajero());
        verify(turnoCajaRepository).findByFechaAperturaBetweenOrderByFechaAperturaDesc(
                LocalDateTime.of(2026, 10, 22, 0, 0, 0),
                LocalDateTime.of(2026, 10, 22, 23, 59, 59, 999999999)
        );
    }

    @Test
    @DisplayName("obtenerHistorialTurnos() - Fecha malformada lanza ReglaNegocioException")
    void testObtenerHistorialTurnos_FechaInvalida_LanzaReglaNegocioException() {
        assertThrows(ReglaNegocioException.class, () -> turnoCajaService.obtenerHistorialTurnos("22-10-2026"));
        verifyNoInteractions(turnoCajaRepository);
    }
}
