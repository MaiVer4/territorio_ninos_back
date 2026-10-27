package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.TarifaEvento;
import com.software.territoriodeninos.entity.TarifaTiempo;
import com.software.territoriodeninos.util.EstadoAtraccion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AtraccionCatalogoRepositoryTest - Pruebas de integración de consultas en lote para catálogo")
class AtraccionCatalogoRepositoryTest {

    @Autowired
    private AtraccionRepository atraccionRepository;

    @Autowired
    private TarifaTiempoRepository tarifaTiempoRepository;

    @Autowired
    private TarifaEventoRepository tarifaEventoRepository;

    @Test
    @DisplayName("findByEstadoOrderByNombreAsc y batch queries de tarifas activas ordenadas")
    void testConsultasCatalogo() {
        // Crear 2 atracciones activas y 1 inactiva
        Atraccion a1 = Atraccion.builder()
                .nombre("Zona B - Inflable")
                .tarifaMinuto(new BigDecimal("1000.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();
        Atraccion a2 = Atraccion.builder()
                .nombre("Zona A - Camas Elásticas")
                .tarifaMinuto(new BigDecimal("1200.00"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();
        Atraccion a3 = Atraccion.builder()
                .nombre("Zona C - Mantenimiento")
                .tarifaMinuto(new BigDecimal("800.00"))
                .estado(EstadoAtraccion.INACTIVO)
                .build();

        atraccionRepository.saveAll(List.of(a1, a2, a3));

        // Verificar ordenamiento alfabético de activas
        List<Atraccion> activas = atraccionRepository.findByEstadoOrderByNombreAsc(EstadoAtraccion.ACTIVO);
        assertEquals(2, activas.size());
        assertEquals("Zona A - Camas Elásticas", activas.get(0).getNombre());
        assertEquals("Zona B - Inflable", activas.get(1).getNombre());

        List<Long> ids = activas.stream().map(Atraccion::getId).toList();

        // Agregar tarifas activas e inactivas de tiempo
        TarifaTiempo tt1 = TarifaTiempo.builder()
                .atraccion(a2)
                .duracionMinutos(30)
                .precio(new BigDecimal("25000.00"))
                .activo(true)
                .build();
        TarifaTiempo tt2 = TarifaTiempo.builder()
                .atraccion(a2)
                .duracionMinutos(15)
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .build();
        TarifaTiempo ttInactiva = TarifaTiempo.builder()
                .atraccion(a2)
                .duracionMinutos(45)
                .precio(new BigDecimal("35000.00"))
                .activo(false)
                .build();

        tarifaTiempoRepository.saveAll(List.of(tt1, tt2, ttInactiva));

        // Batch query tiempo: orden ascendente por duracionMinutos y solo activas
        List<TarifaTiempo> resTiempo = tarifaTiempoRepository
                .findByAtraccionIdInAndActivoTrueOrderByDuracionMinutosAsc(ids);
        assertEquals(2, resTiempo.size());
        assertEquals(15, resTiempo.get(0).getDuracionMinutos());
        assertEquals(30, resTiempo.get(1).getDuracionMinutos());

        // Agregar tarifas de evento
        TarifaEvento te1 = TarifaEvento.builder()
                .atraccion(a1)
                .intentos(5)
                .precio(new BigDecimal("18000.00"))
                .activo(true)
                .build();
        TarifaEvento te2 = TarifaEvento.builder()
                .atraccion(a1)
                .intentos(2)
                .precio(new BigDecimal("9000.00"))
                .activo(true)
                .build();
        TarifaEvento teInactiva = TarifaEvento.builder()
                .atraccion(a1)
                .intentos(10)
                .precio(new BigDecimal("30000.00"))
                .activo(false)
                .build();

        tarifaEventoRepository.saveAll(List.of(te1, te2, teInactiva));

        // Batch query evento: orden ascendente por intentos y solo activas
        List<TarifaEvento> resEvento = tarifaEventoRepository
                .findByAtraccionIdInAndActivoTrueOrderByIntentosAsc(ids);
        assertEquals(2, resEvento.size());
        assertEquals(2, resEvento.get(0).getIntentos());
        assertEquals(5, resEvento.get(1).getIntentos());
    }
}
