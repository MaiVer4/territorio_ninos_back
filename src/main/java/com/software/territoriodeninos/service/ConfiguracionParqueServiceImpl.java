package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.entity.ConfiguracionParque;
import com.software.territoriodeninos.repository.ConfiguracionParqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfiguracionParqueServiceImpl implements ConfiguracionParqueService {

    private final ConfiguracionParqueRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionParqueDTO obtenerConfiguracion() {
        ConfiguracionParque config = repository.findFirstByOrderByIdAsc()
                .orElseGet(this::crearConfiguracionPorDefecto);
        return mapToDTO(config);
    }

    @Override
    public ConfiguracionParqueDTO actualizarConfiguracion(ConfiguracionParqueDTO dto) {
        ConfiguracionParque config = repository.findFirstByOrderByIdAsc()
                .orElseGet(this::crearConfiguracionPorDefecto);

        config.setNombreParque(dto.getNombreParque());
        config.setNit(dto.getNit());
        config.setDireccion(dto.getDireccion());
        config.setTelefono(dto.getTelefono());
        config.setPieTicketLegal(dto.getPieTicketLegal());
        config.setTiempoGraciaGeneralSegundos(dto.getTiempoGraciaGeneralSegundos());

        ConfiguracionParque guardada = repository.save(config);
        log.info("Configuración general del parque actualizada");
        return mapToDTO(guardada);
    }

    private ConfiguracionParque crearConfiguracionPorDefecto() {
        ConfiguracionParque defaultConf = ConfiguracionParque.builder()
                .nombreParque("Territorio de Niños")
                .nit("900.123.456-7")
                .direccion("Centro Comercial Parque Central, Local 201")
                .telefono("+57 300 123 4567")
                .pieTicketLegal("Territorio de Niños SAS - El cuidado de los menores es responsabilidad del acudiente. Boleto válido únicamente para el día de emisión.")
                .tiempoGraciaGeneralSegundos(60)
                .build();
        return repository.save(defaultConf);
    }

    private ConfiguracionParqueDTO mapToDTO(ConfiguracionParque c) {
        return ConfiguracionParqueDTO.builder()
                .id(c.getId())
                .nombreParque(c.getNombreParque())
                .nit(c.getNit())
                .direccion(c.getDireccion())
                .telefono(c.getTelefono())
                .pieTicketLegal(c.getPieTicketLegal())
                .tiempoGraciaGeneralSegundos(c.getTiempoGraciaGeneralSegundos())
                .build();
    }
}
