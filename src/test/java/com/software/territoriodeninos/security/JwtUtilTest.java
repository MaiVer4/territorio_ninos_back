package com.software.territoriodeninos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.util.NombreRol;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set properties that would normally be injected by @Value
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "thisIsAVeryLongSecretKeyForTestingPurposeOnly");
        ReflectionTestUtils.setField(jwtUtil, "expirationHours", 24L);
    }

    @Test
    void generarToken_deberiaRetornarTokenValido() {
        Rol rol = new Rol();
        rol.setNombre(NombreRol.ADMINISTRADOR);
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombre("Test User");
        usuario.setCorreo("test@example.com");
        usuario.setRol(rol);
        
        String token = jwtUtil.generarToken(usuario);
        
        assertNotNull(token);
        assertTrue(jwtUtil.esValido(token));
        assertEquals("test@example.com", jwtUtil.extraerCorreo(token));
        assertEquals("ADMINISTRADOR", jwtUtil.extraerRol(token));
    }

    @Test
    void extraerTokenDelHeader_deberiaLimpiarBearer() {
        String token = "Bearer my-token-123";
        assertEquals("my-token-123", jwtUtil.extraerTokenDelHeader(token));
        
        String noBearer = "my-token-123";
        assertEquals("my-token-123", jwtUtil.extraerTokenDelHeader(noBearer));
    }
}
