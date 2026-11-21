package com.software.territoriodeninos.dto.tienda;

import com.software.territoriodeninos.util.EstadoTicket;
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
    String qrData
) {}
