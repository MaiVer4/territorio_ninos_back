package com.software.territoriodeninos.dto.tienda;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.IngresoTextoFormatter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Detalle del ticket digital emitido para presentación óptica ante el operario")
public record TicketOnlineDTO(
    UUID uuidCodigo,
    Long atraccionId,
    String atraccionNombre,
    String menorNombre,
    Integer menorEdad,
    String tipoTarifa,
    String modalidadTexto,
    BigDecimal precioUnitario,
    EstadoTicket estadoTicket,
    LocalDateTime fechaEmision,
    String qrData,
    LocalDateTime horaIngresoEstimada,
    String ingresoTexto
) {
    public TicketOnlineDTO(
            UUID uuidCodigo,
            Long atraccionId,
            String atraccionNombre,
            String menorNombre,
            Integer menorEdad,
            String tipoTarifa,
            String modalidadTexto,
            BigDecimal precioUnitario,
            EstadoTicket estadoTicket,
            LocalDateTime fechaEmision,
            String qrData
    ) {
        this(
                uuidCodigo,
                atraccionId,
                atraccionNombre,
                menorNombre,
                menorEdad,
                tipoTarifa,
                modalidadTexto,
                precioUnitario,
                estadoTicket,
                fechaEmision,
                qrData,
                null,
                IngresoTextoFormatter.AHORA
        );
    }

    @JsonProperty("codigoCorto")
    public String codigoCorto() {
        if (uuidCodigo == null) {
            return null;
        }
        return "TDN-" + uuidCodigo.toString().substring(0, 8).toUpperCase();
    }
}
