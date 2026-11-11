package com.software.territoriodeninos.service;

import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UUIDGeneratorServiceImpl implements UUIDGeneratorService {

    public static final int MAX_REINTENTOS_COLISION = 10;

    private final TicketRepository ticketRepository;

    @Override
    public UUID generarUuidUnico() {
        return generarUuidUnicoExcluyendo(Collections.emptySet());
    }

    @Override
    public List<UUID> generarUuidsUnicos(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad de UUIDs a generar debe ser mayor a cero");
        }

        List<UUID> resultado = new ArrayList<>(cantidad);
        Set<UUID> generadosEnLote = new HashSet<>(cantidad);

        if (cantidad == 1) {
            UUID unico = generarUuidUnicoExcluyendo(generadosEnLote);
            resultado.add(unico);
            return Collections.unmodifiableList(resultado);
        }

        int intentos = 0;
        while (generadosEnLote.size() < cantidad && intentos < MAX_REINTENTOS_COLISION * cantidad) {
            intentos++;
            int faltantes = cantidad - generadosEnLote.size();
            Set<UUID> candidatosLote = new HashSet<>(faltantes);
            for (int i = 0; i < faltantes; i++) {
                UUID cand = generarUuidCandidato();
                if (!generadosEnLote.contains(cand)) {
                    candidatosLote.add(cand);
                }
            }

            Set<UUID> colisionesBd = ticketRepository.findExistingUuids(candidatosLote);
            if (colisionesBd != null && !colisionesBd.isEmpty()) {
                candidatosLote.removeAll(colisionesBd);
                log.warn("Detectadas {} colisiones de UUID en BD durante generación en lote. Reintentando...", colisionesBd.size());
            }

            for (UUID valido : candidatosLote) {
                if (generadosEnLote.add(valido)) {
                    resultado.add(valido);
                    if (resultado.size() == cantidad) {
                        break;
                    }
                }
            }
        }

        if (resultado.size() < cantidad) {
            while (resultado.size() < cantidad) {
                UUID nuevo = generarUuidUnicoExcluyendo(generadosEnLote);
                generadosEnLote.add(nuevo);
                resultado.add(nuevo);
            }
        }

        return Collections.unmodifiableList(resultado);
    }

    private UUID generarUuidUnicoExcluyendo(Set<UUID> excluidosEnLote) {
        int intentos = 0;
        while (intentos < MAX_REINTENTOS_COLISION) {
            UUID candidato = generarUuidCandidato();
            intentos++;

            if (excluidosEnLote.contains(candidato)) {
                log.warn("Colisión de UUID detectada dentro del lote en memoria: {}. Reintento #{}", candidato, intentos);
                continue;
            }

            if (ticketRepository.existsByUuidCodigo(candidato)) {
                log.warn("Colisión de UUID detectada contra TicketRepository en base de datos: {}. Reintento #{}", candidato, intentos);
                continue;
            }

            return candidato;
        }

        throw new ReglaNegocioException("No fue posible generar un código UUID único para el ticket tras " +
                MAX_REINTENTOS_COLISION + " intentos consecutivos por colisión");
    }

    /**
     * Genera un UUID criptoseguro versión 4 mediante java.util.UUID.randomUUID().
     * Método protegido para facilitar la simulación y prueba de colisiones con Mockito spy.
     */
    protected UUID generarUuidCandidato() {
        return UUID.randomUUID();
    }
}
