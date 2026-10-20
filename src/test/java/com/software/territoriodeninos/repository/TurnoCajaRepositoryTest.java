package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.TurnoCaja;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TurnoCajaRepositoryTest - Pruebas de consultas derivadas de TurnoCaja")
class TurnoCajaRepositoryTest {

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    @Test
    @DisplayName("findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc encuentra el turno activo más reciente")
    void testFindFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc_Exitoso() {
        Long usuarioId = 100L;
        LocalDateTime now = LocalDateTime.now();

        // Turno cerrado anterior
        TurnoCaja turnoCerrado = TurnoCaja.builder()
                .usuarioId(usuarioId)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(now.minusHours(8))
                .fechaCierre(now.minusHours(4))
                .ventasEfectivoSistema(new BigDecimal("120000.00"))
                .montoEfectivoReportado(new BigDecimal("170000.00"))
                .diferencia(BigDecimal.ZERO)
                .estado(EstadoTurnoCaja.CERRADO)
                .observaciones("Cierre regular")
                .build();
        turnoCajaRepository.save(turnoCerrado);

        // Turno abierto actual
        TurnoCaja turnoAbierto = TurnoCaja.builder()
                .usuarioId(usuarioId)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(now.minusHours(1))
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones("Turno tarde")
                .build();
        TurnoCaja guardado = turnoCajaRepository.save(turnoAbierto);

        // Turno abierto de OTRO usuario
        TurnoCaja turnoOtroUsuario = TurnoCaja.builder()
                .usuarioId(200L)
                .montoBase(new BigDecimal("60000.00"))
                .fechaApertura(now)
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();
        turnoCajaRepository.save(turnoOtroUsuario);

        // Act
        Optional<TurnoCaja> resultado = turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoTurnoCaja.ABIERTO);

        // Assert
        assertTrue(resultado.isPresent(), "Debe existir un turno abierto para el usuario");
        assertEquals(guardado.getId(), resultado.get().getId());
        assertEquals(new BigDecimal("50000.00"), resultado.get().getMontoBase());
        assertEquals(EstadoTurnoCaja.ABIERTO, resultado.get().getEstado());
        assertEquals("Turno tarde", resultado.get().getObservaciones());
    }

    @Test
    @DisplayName("findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc retorna empty cuando no hay turnos activos")
    void testFindFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc_SinTurnoActivo() {
        Long usuarioId = 300L;

        TurnoCaja turnoCerrado = TurnoCaja.builder()
                .usuarioId(usuarioId)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(LocalDateTime.now().minusDays(1))
                .fechaCierre(LocalDateTime.now().minusDays(1).plusHours(6))
                .estado(EstadoTurnoCaja.CERRADO)
                .build();
        turnoCajaRepository.save(turnoCerrado);

        Optional<TurnoCaja> resultado = turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoTurnoCaja.ABIERTO);

        assertTrue(resultado.isEmpty(), "No debe retornar turno abierto si todos están cerrados");
    }

    @Test
    @DisplayName("Ordena descendentemente y toma el primero si hay múltiples turnos abiertos")
    void testMultipleTurnosAbiertos_RetornaElMasReciente() {
        Long usuarioId = 400L;
        LocalDateTime baseTime = LocalDateTime.now();

        TurnoCaja turnoViejo = TurnoCaja.builder()
                .usuarioId(usuarioId)
                .montoBase(new BigDecimal("30000.00"))
                .fechaApertura(baseTime.minusHours(5))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        TurnoCaja turnoNuevo = TurnoCaja.builder()
                .usuarioId(usuarioId)
                .montoBase(new BigDecimal("50000.00"))
                .fechaApertura(baseTime.minusMinutes(10))
                .estado(EstadoTurnoCaja.ABIERTO)
                .build();

        turnoCajaRepository.save(turnoViejo);
        TurnoCaja guardadoNuevo = turnoCajaRepository.save(turnoNuevo);

        Optional<TurnoCaja> resultado = turnoCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoTurnoCaja.ABIERTO);

        assertTrue(resultado.isPresent());
        assertEquals(guardadoNuevo.getId(), resultado.get().getId());
    }
}
