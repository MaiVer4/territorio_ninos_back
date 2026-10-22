package com.software.territoriodeninos.entity;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClienteMenorEntityTest - Pruebas de modelos de entidad y DTOs de Cliente y Menor")
class ClienteMenorEntityTest {

    @Test
    @DisplayName("Cliente entity y relación con Menor")
    void testClienteYMenorRelacion() {
        Cliente cliente = Cliente.builder()
                .numeroDocumento("1098765432")
                .nombres("Juan")
                .apellidos("Valdez")
                .telefono("3001234567")
                .correo("juan.valdez@cafe.co")
                .fechaRegistro(LocalDateTime.now())
                .build();

        Menor menor = Menor.builder()
                .numeroDocumento("1020304050")
                .nombreCompleto("Pepito Valdez")
                .fechaNacimiento(LocalDate.now().minusYears(8))
                .build();

        cliente.addMenor(menor);

        assertEquals(1, cliente.getMenores().size());
        assertEquals(cliente, menor.getCliente());

        menor.calcularEdadSiAplica();
        assertEquals(8, menor.getEdad());

        cliente.removeMenor(menor);
        assertEquals(0, cliente.getMenores().size());
        assertNull(menor.getCliente());
    }

    @Test
    @DisplayName("MenorDTO toEntity y fromEntity calculan la edad correctamente")
    void testMenorDTOConversion() {
        LocalDate fechaNac = LocalDate.now().minusYears(5);
        MenorDTO dto = new MenorDTO(null, "11223344", "Sofia Gómez", fechaNac, null);

        Menor entity = dto.toEntity();
        assertEquals(5, entity.getEdad());
        assertEquals("11223344", entity.getNumeroDocumento());
        assertEquals("Sofia Gómez", entity.getNombreCompleto());

        MenorDTO fromEntity = MenorDTO.fromEntity(entity);
        assertEquals(5, fromEntity.edad());
        assertEquals("11223344", fromEntity.numeroDocumento());
    }

    @Test
    @DisplayName("ClienteResponseDTO expone aliases nombreCompleto, documento y email")
    void testClienteResponseDTOAliases() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .numeroDocumento("123456789")
                .nombres("María")
                .apellidos("Rodríguez")
                .telefono("3109876543")
                .correo("maria@gmail.com")
                .fechaRegistro(LocalDateTime.now())
                .build();

        ClienteResponseDTO response = ClienteResponseDTO.fromEntity(cliente);

        assertEquals("María Rodríguez", response.nombreCompleto());
        assertEquals("123456789", response.documento());
        assertEquals("maria@gmail.com", response.email());
        assertTrue(response.menores().isEmpty());
    }
}
