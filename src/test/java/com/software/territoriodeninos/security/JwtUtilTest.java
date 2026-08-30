package com.software.territoriodeninos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
        String token = jwtUtil.generarToken("test@example.com", "ADMINISTRADOR");
        
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
