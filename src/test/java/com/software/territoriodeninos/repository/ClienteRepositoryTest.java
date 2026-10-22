package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.Menor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ClienteRepositoryTest - Pruebas de integración con H2 para Cliente y Menor")
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MenorRepository menorRepository;

    @Test
    @DisplayName("Guarda cliente con menores en cascada y consulta por documento")
    void testGuardarYConsultarClienteConMenores() {
        Cliente cliente = Cliente.builder()
                .numeroDocumento("1098765432")
                .nombres("Laura")
                .apellidos("Castro")
                .telefono("3005551234")
                .correo("laura.castro@example.com")
                .build();

        Menor menor1 = Menor.builder()
                .numeroDocumento("TI1001")
                .nombreCompleto("Lucas Castro")
                .fechaNacimiento(LocalDate.now().minusYears(7))
                .build();

        Menor menor2 = Menor.builder()
                .numeroDocumento("TI1002")
                .nombreCompleto("Martina Castro")
                .fechaNacimiento(LocalDate.now().minusYears(4))
                .build();

        cliente.addMenor(menor1);
        cliente.addMenor(menor2);

        Cliente guardado = clienteRepository.save(cliente);
        assertNotNull(guardado.getId());
        assertEquals(2, guardado.getMenores().size());

        // Test existsByNumeroDocumento
        assertTrue(clienteRepository.existsByNumeroDocumento("1098765432"));
        assertFalse(clienteRepository.existsByNumeroDocumento("999999999"));

        // Test findByNumeroDocumento
        Optional<Cliente> encontrado = clienteRepository.findByNumeroDocumento("1098765432");
        assertTrue(encontrado.isPresent());
        assertEquals("Laura", encontrado.get().getNombres());

        // Test findByNumeroDocumentoWithMenores
        Optional<Cliente> conMenores = clienteRepository.findByNumeroDocumentoWithMenores("1098765432");
        assertTrue(conMenores.isPresent());
        assertEquals(2, conMenores.get().getMenores().size());

        // Test MenorRepository queries
        Optional<Menor> menorEncontrado = menorRepository.findByNumeroDocumento("TI1001");
        assertTrue(menorEncontrado.isPresent());
        assertEquals("Lucas Castro", menorEncontrado.get().getNombreCompleto());

        List<Menor> menoresPorCliente = menorRepository.findByClienteId(guardado.getId());
        assertEquals(2, menoresPorCliente.size());
    }
}
