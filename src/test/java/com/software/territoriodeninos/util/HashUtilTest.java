package com.software.territoriodeninos.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HashUtil - Tests unitarios")
class HashUtilTest {

    @Test
    @DisplayName("sha256Hex genera hash correcto para una cadena dada")
    void sha256Hex_GeneraHashCorrecto() {
        String input = "test123";
        String hash1 = HashUtil.sha256Hex(input);
        String hash2 = HashUtil.sha256Hex(input);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("sha256Hex retorna null cuando la entrada es nula")
    void sha256Hex_NullInput_RetornaNull() {
        assertNull(HashUtil.sha256Hex(null));
    }
}
