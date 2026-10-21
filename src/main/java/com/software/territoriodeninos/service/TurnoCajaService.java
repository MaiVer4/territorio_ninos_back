package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AperturaCajaDTO;
import com.software.territoriodeninos.dto.CierreCajaDTO;
import com.software.territoriodeninos.dto.TurnoCajaResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface TurnoCajaService {

    TurnoCajaResponseDTO abrirTurno(AperturaCajaDTO dto, Authentication auth);

    Optional<TurnoCajaResponseDTO> obtenerTurnoActivo(Authentication auth);

    TurnoCajaResponseDTO cerrarTurno(CierreCajaDTO dto, Authentication auth);
}
