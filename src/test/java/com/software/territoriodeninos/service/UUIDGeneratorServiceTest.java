package com.software.territoriodeninos.service;

import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UUIDGeneratorServiceTest - Pruebas unitarias de generación criptosegura y detección de colisiones (Notion: CA-01)")
class UUIDGeneratorServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private UUIDGeneratorServiceImpl uuidGeneratorService;

    @BeforeEach
    void setUp() {
        uuidGeneratorService = Mockito.spy(new UUIDGeneratorServiceImpl(ticketRepository));
    }

    @Test
    @DisplayName("generarUuidUnico - Genera un UUID criptoseguro y verifica no colisión en repositorio")
    void generarUuidUnico_sinColision_retornaUuidValido() {
        when(ticketRepository.existsByUuidCodigo(any(UUID.class))).thenReturn(false);

        UUID uuid = uuidGeneratorService.generarUuidUnico();

        assertNotNull(uuid, "El UUID generado no debe ser nulo");
        assertEquals(4, uuid.version(), "El UUID debe ser versión 4 (criptoseguro)");
        verify(ticketRepository, atLeastOnce()).existsByUuidCodigo(uuid);
    }

    @Test
    @DisplayName("generarUuidUnico - Detecta colisión en TicketRepository, reintenta y retorna el siguiente candidato disponible")
    void generarUuidUnico_conColisionInicial_reintentaYRetornaSiguienteUuid() {
        UUID uuidColisionado = UUID.randomUUID();
        UUID uuidValido = UUID.randomUUID();

        doReturn(uuidColisionado, uuidValido).when(uuidGeneratorService).generarUuidCandidato();
        when(ticketRepository.existsByUuidCodigo(uuidColisionado)).thenReturn(true);
        when(ticketRepository.existsByUuidCodigo(uuidValido)).thenReturn(false);

        UUID resultado = uuidGeneratorService.generarUuidUnico();

        assertNotNull(resultado);
        assertEquals(uuidValido, resultado, "Debe retornar el UUID libre tras descartar la colisión");
        verify(ticketRepository).existsByUuidCodigo(uuidColisionado);
        verify(ticketRepository).existsByUuidCodigo(uuidValido);
    }

    @Test
    @DisplayName("generarUuidUnico - Lanza ReglaNegocioException si se supera el límite de reintentos por colisión")
    void generarUuidUnico_superaMaximoReintentos_lanzaReglaNegocioException() {
        when(ticketRepository.existsByUuidCodigo(any(UUID.class))).thenReturn(true);

        ReglaNegocioException ex = assertThrows(
                ReglaNegocioException.class,
                () -> uuidGeneratorService.generarUuidUnico(),
                "Debe lanzar excepción al agotarse los 10 intentos de colisión"
        );

        assertTrue(ex.getMessage().contains("10 intentos consecutivos por colisión"));
        verify(ticketRepository, times(UUIDGeneratorServiceImpl.MAX_REINTENTOS_COLISION)).existsByUuidCodigo(any(UUID.class));
    }

    @Test
    @DisplayName("generarUuidsUnicos - Genera la cantidad exacta solicitada garantizando unicidad en memoria y BD")
    void generarUuidsUnicos_cantidadPositiva_retornaListaSinDuplicados() {
        when(ticketRepository.existsByUuidCodigo(any(UUID.class))).thenReturn(false);

        int cantidad = 5;
        List<UUID> uuids = uuidGeneratorService.generarUuidsUnicos(cantidad);

        assertNotNull(uuids);
        assertEquals(cantidad, uuids.size());
        Set<UUID> conjuntoUnicos = new HashSet<>(uuids);
        assertEquals(cantidad, conjuntoUnicos.size(), "No deben existir UUIDs duplicados en el lote generado");
    }

    @Test
    @DisplayName("generarUuidsUnicos - Lanza ReglaNegocioException si la cantidad es menor o igual a cero")
    void generarUuidsUnicos_cantidadInvalida_lanzaReglaNegocioException() {
        assertThrows(ReglaNegocioException.class, () -> uuidGeneratorService.generarUuidsUnicos(0));
        assertThrows(ReglaNegocioException.class, () -> uuidGeneratorService.generarUuidsUnicos(-3));
    }
}
