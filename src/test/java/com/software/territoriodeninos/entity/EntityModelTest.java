package com.software.territoriodeninos.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
}
