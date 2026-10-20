package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.util.EstadoTurnoCaja;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EntityModelTest - Pruebas de entidades refactorizadas")
class EntityModelTest {

    @Test
    @DisplayName("PasswordResetToken tiene getters, setters, builder y columna de longitud 64")
    void testPasswordResetTokenModel() throws NoSuchFieldException {
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(15);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .token("a".repeat(64))
                .usuario(usuario)
                .fechaExpiracion(expiracion)
                .build();

        assertEquals(1L, token.getId());
        assertEquals(64, token.getToken().length());
        assertEquals(usuario, token.getUsuario());
        assertEquals(expiracion, token.getFechaExpiracion());
        assertFalse(token.isExpired());

        // Verificar expiración
        token.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        assertTrue(token.isExpired());

        // Verificar anotación JPA length = 64
        Field tokenField = PasswordResetToken.class.getDeclaredField("token");
        Column column = tokenField.getAnnotation(Column.class);
        assertNotNull(column, "El campo token debe tener la anotación @Column");
        assertEquals(64, column.length(), "La longitud de la columna token debe ser exactamente 64");
        assertFalse(column.nullable());
        assertTrue(column.unique());
    }

    @Test
    @DisplayName("ConfiguracionParque tiene getters, setters y builder")
    void testConfiguracionParqueModel() {
        ConfiguracionParque config = ConfiguracionParque.builder()
                .id(1L)
                .nombreParque("Territorio de Niños")
                .nit("900123456-7")
                .direccion("Calle 123 # 45-67")
                .telefono("3001234567")
                .pieTicketLegal("Ticket legal de prueba")
                .tiempoGraciaGeneralSegundos(90)
                .build();

        assertEquals(1L, config.getId());
        assertEquals("Territorio de Niños", config.getNombreParque());
        assertEquals("900123456-7", config.getNit());
        assertEquals("Calle 123 # 45-67", config.getDireccion());
        assertEquals("3001234567", config.getTelefono());
        assertEquals("Ticket legal de prueba", config.getPieTicketLegal());
        assertEquals(90, config.getTiempoGraciaGeneralSegundos());

        // Test setters
        config.setTiempoGraciaGeneralSegundos(120);
        assertEquals(120, config.getTiempoGraciaGeneralSegundos());
    }

    @Test
    @DisplayName("TurnoCaja tiene getters, setters, builder y anotaciones JPA requeridas")
    void testTurnoCajaModel() throws NoSuchFieldException {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal montoBase = new BigDecimal("100000.00");
        BigDecimal ventas = new BigDecimal("450000.00");
        BigDecimal reportado = new BigDecimal("550000.00");
        BigDecimal diferencia = BigDecimal.ZERO;

        TurnoCaja turno = TurnoCaja.builder()
                .id(1L)
                .usuarioId(42L)
                .montoBase(montoBase)
                .fechaApertura(now)
                .fechaCierre(now.plusHours(8))
                .ventasEfectivoSistema(ventas)
                .montoEfectivoReportado(reportado)
                .diferencia(diferencia)
                .estado(EstadoTurnoCaja.ABIERTO)
                .observaciones("Turno inicial")
                .build();

        assertEquals(1L, turno.getId());
        assertEquals(42L, turno.getUsuarioId());
        assertEquals(montoBase, turno.getMontoBase());
        assertEquals(now, turno.getFechaApertura());
        assertEquals(now.plusHours(8), turno.getFechaCierre());
        assertEquals(ventas, turno.getVentasEfectivoSistema());
        assertEquals(reportado, turno.getMontoEfectivoReportado());
        assertEquals(diferencia, turno.getDiferencia());
        assertEquals(EstadoTurnoCaja.ABIERTO, turno.getEstado());
        assertEquals("Turno inicial", turno.getObservaciones());

        // Verificar prePersist
        TurnoCaja turnoNuevo = new TurnoCaja();
        turnoNuevo.prePersist();
        assertNotNull(turnoNuevo.getFechaApertura());
        assertEquals(EstadoTurnoCaja.ABIERTO, turnoNuevo.getEstado());
        assertEquals(BigDecimal.ZERO, turnoNuevo.getVentasEfectivoSistema());

        // Verificar @Enumerated(EnumType.STRING) en el campo estado
        Field estadoField = TurnoCaja.class.getDeclaredField("estado");
        Enumerated enumerated = estadoField.getAnnotation(Enumerated.class);
        assertNotNull(enumerated, "El campo estado debe tener la anotación @Enumerated");
        assertEquals(EnumType.STRING, enumerated.value(), "La anotación @Enumerated debe ser de tipo STRING");

        // Verificar enumeradores EstadoTurnoCaja
        assertEquals(2, EstadoTurnoCaja.values().length);
        assertEquals(EstadoTurnoCaja.ABIERTO, EstadoTurnoCaja.valueOf("ABIERTO"));
        assertEquals(EstadoTurnoCaja.CERRADO, EstadoTurnoCaja.valueOf("CERRADO"));
    }
}
