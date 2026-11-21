package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.tienda.CatalogoAtraccionOnlineDTO;
import com.software.territoriodeninos.dto.tienda.CompraOnlineRequestDTO;
import com.software.territoriodeninos.dto.tienda.CompraOnlineResponseDTO;
import com.software.territoriodeninos.dto.tienda.TicketOnlineDTO;

import java.util.List;

public interface TiendaService {

    List<CatalogoAtraccionOnlineDTO> obtenerCatalogoPublico();

    CompraOnlineResponseDTO procesarCompraOnline(CompraOnlineRequestDTO request);

    default List<TicketOnlineDTO> consultarBoletosActivos(String documento, String correo) {
        return consultarBoletosActivos(documento, correo, null);
    }

    List<TicketOnlineDTO> consultarBoletosActivos(String documento, String correo, String codigoOrden);
}
