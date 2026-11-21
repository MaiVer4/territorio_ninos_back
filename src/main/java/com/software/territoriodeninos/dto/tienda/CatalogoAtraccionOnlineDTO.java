package com.software.territoriodeninos.dto.tienda;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.dto.TarifaEventoResponseDTO;
import com.software.territoriodeninos.dto.TarifaTiempoResponseDTO;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.IngresoTextoFormatter;
import com.software.territoriodeninos.util.TipoAtraccion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Atracción visible en el catálogo de compra online con sus tarifas y disponibilidad")
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
    List<TarifaEventoResponseDTO> tarifasEvento,
    BigDecimal tarifaMinuto,
    Integer ocupacionActual,
    Integer cuposLibres,
    LocalDateTime horaProximoIngreso,
    String ingresoTexto
) {
    public CatalogoAtraccionOnlineDTO(
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
    ) {
        this(
                id,
                nombre,
                descripcion,
                imagenUrl,
                tipo,
                estado,
                capacidadMaxima,
                duracionMinutosBase,
                edadMinima,
                edadMaxima,
                disponibleParaCompra,
                tarifasTiempo,
                tarifasEvento,
                null,
                0,
                capacidadMaxima != null ? capacidadMaxima : 0,
                null,
                IngresoTextoFormatter.AHORA
        );
    }

    @JsonProperty("tipoAtraccion")
    public TipoAtraccion tipoAtraccion() {
        return tipo;
    }

    @JsonProperty("duracionRecomendadaMinutos")
    public Integer duracionRecomendadaMinutos() {
        return duracionMinutosBase;
    }
}
