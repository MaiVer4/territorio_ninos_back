package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;

public interface ConfiguracionParqueService {
    ConfiguracionParqueDTO obtenerConfiguracion();
    ConfiguracionParqueDTO actualizarConfiguracion(ConfiguracionParqueDTO dto);
}
