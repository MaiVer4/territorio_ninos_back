package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
                .menores(new ArrayList<>())
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
    @DisplayName("registrarCliente guarda el cliente correctamente sin menores cuando es nuevo")
    void testRegistrarCliente_SinMenores() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos.gomez@example.com",
                null
        );

        when(clienteRepository.findByNumeroDocumentoWithMenores("1098765432")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("1098765432")).thenReturn(Optional.empty());
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

        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("registrarCliente guarda el cliente con menores asociados cuando es nuevo")
    void testRegistrarCliente_ConMenores() {
        MenorDTO menorDTO = new MenorDTO(
                null,
                "TI998877",
                "Felipe Gómez",
                LocalDate.now().minusYears(5),
                5
        );

        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos.gomez@example.com",
                List.of(menorDTO)
        );

        when(clienteRepository.findByNumeroDocumentoWithMenores("1098765432")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("1098765432")).thenReturn(Optional.empty());
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
    @DisplayName("Notion CA-04: registrarCliente vincula múltiples menores a un acudiente ya existente")
    void testRegistrarCliente_AcudienteExistente_VinculaMultiplesMenores() {
        Menor menorExistente = Menor.builder()
                .id(10L)
                .numeroDocumento("RC111")
                .nombreCompleto("Lucas Gómez")
                .edad(4)
                .build();
        clienteExistente.addMenor(menorExistente);

        MenorDTO menorActualizado = new MenorDTO(10L, "RC111", "Lucas Gómez Pérez", null, 5);
        MenorDTO nuevoMenor1 = new MenorDTO(null, "TI222", "Martina Gómez", null, 8);
        MenorDTO nuevoMenor2 = new MenorDTO(null, "TI333", "Esteban Gómez", null, 15);

        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos Andrés",
                "Gómez",
                "3115559999",
                "carlos.nuevo@example.com",
                List.of(menorActualizado, nuevoMenor1, nuevoMenor2)
        );

        when(clienteRepository.findByNumeroDocumentoWithMenores("1098765432"))
                .thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseDTO response = clienteService.registrarCliente(request);

        assertNotNull(response);
        assertEquals("3115559999", response.telefono());
        assertEquals("carlos.nuevo@example.com", response.correo());
        assertEquals(3, response.menores().size());
        assertEquals("Lucas Gómez Pérez", clienteExistente.getMenores().get(0).getNombreCompleto());
        assertEquals(5, clienteExistente.getMenores().get(0).getEdad());
        verify(clienteRepository).save(clienteExistente);
    }

    @Test
    @DisplayName("Notion CA-03: registrarCliente lanza ReglaNegocioException si la edad es menor a 1 año")
    void testRegistrarCliente_EdadMenorAUno_LanzaReglaNegocioException() {
        MenorDTO menorInvalido = new MenorDTO(null, "RC000", "Bebé Gómez", null, 0);
        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos@example.com",
                List.of(menorInvalido)
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                clienteService.registrarCliente(request)
        );
        assertEquals("La edad del menor debe estar comprendida entre 1 y 17 años.", ex.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Notion CA-03: registrarCliente lanza ReglaNegocioException si la edad es mayor a 17 años")
    void testRegistrarCliente_EdadMayorADiecisiete_LanzaReglaNegocioException() {
        MenorDTO menorInvalido = new MenorDTO(null, "CC1818", "Mayor Gómez", null, 18);
        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos",
                "Gómez",
                "3001234567",
                "carlos@example.com",
                List.of(menorInvalido)
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                clienteService.registrarCliente(request)
        );
        assertEquals("La edad del menor debe estar comprendida entre 1 y 17 años.", ex.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("actualizarCliente actualiza datos del cliente y vincula nuevos menores")
    void testActualizarCliente_Exitoso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenorDTO nuevoMenor = new MenorDTO(null, "TI777", "Valeria Gómez", null, 10);
        ClienteRequestDTO request = new ClienteRequestDTO(
                "1098765432",
                "Carlos Modificado",
                "Gómez",
                "3100000000",
                "carlos.mod@example.com",
                List.of(nuevoMenor)
        );

        ClienteResponseDTO result = clienteService.actualizarCliente(1L, request);

        assertNotNull(result);
        assertEquals("3100000000", result.telefono());
        assertEquals("carlos.mod@example.com", result.correo());
        assertEquals(1, result.menores().size());
        assertEquals("Valeria Gómez", result.menores().get(0).nombreCompleto());
        verify(clienteRepository).save(clienteExistente);
    }
}
