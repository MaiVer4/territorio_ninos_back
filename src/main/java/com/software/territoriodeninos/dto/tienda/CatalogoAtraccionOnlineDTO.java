package com.software.territoriodeninos.dto.tienda;

import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.TipoAtraccion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Atracción visible en el catálogo de compra online con sus tarifas")
public record CatalogoAtraccionOnlineDTO(
    Long id,
    String nombre,
    String descripcion,
    String imagenUrl,
    TipoAtraccion tipo,
    EstadoAtraccion estado,
    Integer capacidadMaxima,
    Integer duracionMinutosBase,
    Integer edadMinima,
    Integer edadMaxima,
    boolean disponibleParaCompra,
    List<TarifaTiempoResponseDTO> tarifasTiempo,
    List<TarifaEventoResponseDTO> tarifasEvento
) {}
