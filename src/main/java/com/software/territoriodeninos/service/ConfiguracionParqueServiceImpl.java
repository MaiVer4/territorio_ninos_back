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
        return mapToDTO(obtenerOCrearConfiguracion());
    }

    @Override
    public ConfiguracionParqueDTO actualizarConfiguracion(ConfiguracionParqueDTO dto) {
        ConfiguracionParque config = obtenerOCrearConfiguracion();

        String nombreParque = dto.getNombreParque() != null && !dto.getNombreParque().isBlank()
                ? dto.getNombreParque()
                : dto.getNombreComercial();
        config.setNombreParque(nombreParque);
        config.setNombreComercial(dto.getNombreComercial() != null ? dto.getNombreComercial() : nombreParque);
        config.setRazonSocial(dto.getRazonSocial());
        config.setNit(dto.getNit());
        config.setDireccion(dto.getDireccion());
        config.setTelefono(dto.getTelefono());
        config.setCiudad(dto.getCiudad());
        config.setPoliticasSeguridad(dto.getPoliticasSeguridad());
        config.setAnchoPapelMm(dto.getAnchoPapelMm() != null ? dto.getAnchoPapelMm() : 80);
        config.setMensajePie(dto.getMensajePie());
        config.setPieTicketLegal(dto.getPieTicketLegal() != null ? dto.getPieTicketLegal() : dto.getPoliticasSeguridad());
        config.setTiempoGraciaGeneralSegundos(dto.getTiempoGraciaGeneralSegundos());

        ConfiguracionParque guardada = repository.save(config);
        log.info("Configuración general del parque actualizada");
        return mapToDTO(guardada);
    }

    private ConfiguracionParque obtenerOCrearConfiguracion() {
        return repository.findFirstByOrderByIdAsc()
                .orElseGet(this::crearConfiguracionPorDefecto);
    }

    private ConfiguracionParque crearConfiguracionPorDefecto() {
        ConfiguracionParque defaultConf = ConfiguracionParque.builder()
                .nombreParque("Territorio de Niños")
                .nombreComercial("Territorio de Niños")
                .razonSocial("Territorio de Niños S.A.S.")
                .nit("901.458.789-2")
                .direccion("Av. Bolívar # 14N-55")
                .telefono("+57 310 456 7890")
                .ciudad("Armenia, Quindío")
                .politicasSeguridad("Uso obligatorio de medias antideslizantes.\nProhibido el ingreso de alimentos y bebidas.\nEl parque no se hace responsable por objetos olvidados.")
                .anchoPapelMm(80)
                .mensajePie("¡Gracias por su visita a Territorio de Niños!")
                .pieTicketLegal("Territorio de Niños S.A.S. - El cuidado de los menores es responsabilidad del acudiente. Boleto válido únicamente para el día de emisión.")
                .tiempoGraciaGeneralSegundos(60)
                .build();
        return repository.save(defaultConf);
    }

    private ConfiguracionParqueDTO mapToDTO(ConfiguracionParque c) {
        String nombreComercial = c.getNombreComercial() != null ? c.getNombreComercial() : c.getNombreParque();
        String pieTicketLegal = c.getPieTicketLegal() != null ? c.getPieTicketLegal() : c.getPoliticasSeguridad();
        return ConfiguracionParqueDTO.builder()
                .id(c.getId())
                .nombreParque(c.getNombreParque())
                .nombreComercial(nombreComercial)
                .razonSocial(c.getRazonSocial())
                .nit(c.getNit())
                .direccion(c.getDireccion())
                .telefono(c.getTelefono())
                .ciudad(c.getCiudad())
                .politicasSeguridad(c.getPoliticasSeguridad())
                .anchoPapelMm(c.getAnchoPapelMm())
                .mensajePie(c.getMensajePie())
                .pieTicketLegal(pieTicketLegal)
                .tiempoGraciaGeneralSegundos(c.getTiempoGraciaGeneralSegundos())
                .build();
    }
}
