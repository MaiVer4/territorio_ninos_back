package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.ClienteRepository;
import com.software.territoriodeninos.repository.MenorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServiceTest - Pruebas unitarias de la lógica de negocio de clientes y menores")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MenorRepository menorRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente clienteExistente;

    @BeforeEach
    void setUp() {
        clienteExistente = Cliente.builder()
                .id(1L)
                .numeroDocumento("1098765432")
                .nombres("Carlos")
                .apellidos("Gómez")
                .telefono("3001234567")
                .correo("carlos.gomez@example.com")
                .fechaRegistro(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("buscarPorDocumento retorna el cliente existente con sus menores")
    void testBuscarPorDocumento_Exitoso() {
        Menor menor = Menor.builder()
                .id(10L)
                .numeroDocumento("TI123")
                .nombreCompleto("Santiago Gómez")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .edad(6)
                .build();
        clienteExistente.addMenor(menor);

        when(clienteRepository.findByNumeroDocumentoWithMenores("1098765432"))
                .thenReturn(Optional.of(clienteExistente));

        ClienteResponseDTO result = clienteService.buscarPorDocumento("1098765432");

        assertNotNull(result);
        assertEquals("1098765432", result.numeroDocumento());
        assertEquals("Carlos Gómez", result.nombreCompleto());
        assertEquals(1, result.menores().size());
        assertEquals("Santiago Gómez", result.menores().get(0).nombreCompleto());
        verify(clienteRepository).findByNumeroDocumentoWithMenores("1098765432");
    }

    @Test
    @DisplayName("buscarPorDocumento lanza RecursoNoEncontradoException si no existe")
    void testBuscarPorDocumento_NoExiste() {
        when(clienteRepository.findByNumeroDocumentoWithMenores("999999999"))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                clienteService.buscarPorDocumento("999999999")
        );
    }

    @Test
    @DisplayName("buscarPorDocumento lanza RecursoNoEncontradoException si documento está vacío")
    void testBuscarPorDocumento_DocumentoVacio() {
        assertThrows(RecursoNoEncontradoException.class, () ->
                clienteService.buscarPorDocumento("   ")
        );
        assertThrows(RecursoNoEncontradoException.class, () ->
                clienteService.buscarPorDocumento(null)
        );
    }

    @Test
    @DisplayName("registrarCliente guarda el cliente correctamente sin menores")
    void testRegistrarCliente_SinMenores() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos.gomez@example.com",
                null
        );

        when(clienteRepository.existsByNumeroDocumento("1098765432")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            c.setFechaRegistro(LocalDateTime.now());
            return c;
        });

        ClienteResponseDTO result = clienteService.registrarCliente(request);

        assertNotNull(result);
        assertEquals("1098765432", result.numeroDocumento());
        assertEquals("Carlos", result.nombres());
        assertEquals("Gómez", result.apellidos());
        assertEquals("3001234567", result.telefono());
        assertEquals("carlos.gomez@example.com", result.correo());
        assertTrue(result.menores().isEmpty());

        verify(clienteRepository).existsByNumeroDocumento("1098765432");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("registrarCliente guarda el cliente con menores asociados")
    void testRegistrarCliente_ConMenores() {
        MenorDTO menorDTO = new MenorDTO(
                null,
                "TI998877",
                "Felipe Gómez",
                LocalDate.now().minusYears(5),
                null
        );

        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos.gomez@example.com",
                List.of(menorDTO)
        );

        when(clienteRepository.existsByNumeroDocumento("1098765432")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            c.setFechaRegistro(LocalDateTime.now());
            if (c.getMenores() != null && !c.getMenores().isEmpty()) {
                c.getMenores().get(0).setId(10L);
            }
            return c;
        });

        ClienteResponseDTO result = clienteService.registrarCliente(request);

        assertNotNull(result);
        assertEquals(1, result.menores().size());
        assertEquals("Felipe Gómez", result.menores().get(0).nombreCompleto());
        assertEquals(5, result.menores().get(0).edad());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("registrarCliente lanza ConflictoException si el documento ya está registrado")
    void testRegistrarCliente_ConflictoDuplicado() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos.gomez@example.com",
                null
        );

        when(clienteRepository.existsByNumeroDocumento("1098765432")).thenReturn(true);

        assertThrows(ConflictoException.class, () ->
                clienteService.registrarCliente(request)
        );

        verify(clienteRepository, never()).save(any(Cliente.class));
    }
}
