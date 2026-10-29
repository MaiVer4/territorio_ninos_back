package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO;
import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;
import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;
import com.software.territoriodeninos.dto.VentaProcesarRequestDTO;
import com.software.territoriodeninos.dto.VentaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Ventas y Taquilla", description = "Endpoints para catálogo de atracciones, validación, cotización y procesamiento de ventas")
public interface VentaApi {

    @Operation(
        summary = "Obtener catálogo de atracciones para venta",
        description = "Retorna las atracciones en estado ACTIVO con sus tarifas vigentes ordenadas (tiempo y evento) y el aforo disponible en tiempo real"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catálogo obtenido exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CatalogoAtraccionVentaDTO.class)))),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<List<CatalogoAtraccionVentaDTO>> obtenerCatalogo();

    @Operation(
        summary = "Validar aforo y consistencia de tarifas de una venta",
        description = "Comprueba que la suma de tickets por atracción no supere la capacidad máxima y que los precios coincidan con la base de datos"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validación exitosa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResultadoValidacionVentaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Exceso de aforo, tarifa inactiva o adulteración de precios detectada",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Atracción o tarifa no encontrada",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<ResultadoValidacionVentaDTO> validarVenta(
            @Valid @RequestBody @NotEmpty(message = "La lista de ítems a validar no puede estar vacía") List<@Valid ItemVentaValidacionDTO> items
    );

    @Operation(
        summary = "Cotizar venta previamente",
        description = "Calcula subtotales, base gravable, impuestos dinámicos según ConfiguracionParque y total neto con redondeo comercial HALF_UP"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cotización calculada exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CotizacionResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Valores nulos, negativos o inconsistencia en tarifas",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Atracción o tarifa referenciada no encontrada",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<CotizacionResponseDTO> cotizarVenta(
            @Valid @RequestBody CotizacionRequestDTO request
    );

    @Operation(
        summary = "Procesar y liquidar venta en taquilla",
        description = "Valida aforo, tarifas y parentesco, registra la venta con sus boletas e incrementa las ventas del turno de caja"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Venta procesada exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, monto insuficiente o inconsistencias en tarifas",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Cliente, menor, atracción o turno no encontrado",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Turno de caja cerrado o conflicto de negocio",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<VentaResponseDTO> procesarVenta(
            @Valid @RequestBody VentaProcesarRequestDTO request,
            @Parameter(hidden = true) Authentication auth
    );

    @Operation(
        summary = "Consultar venta por ID",
        description = "Retorna el comprobante y detalle de una venta específica"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Venta no encontrada",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<VentaResponseDTO> obtenerVenta(
            @Parameter(description = "ID único de la venta", example = "1") @PathVariable Long id
    );

    @Operation(
        summary = "Listar ventas por turno de caja",
        description = "Retorna el listado de ventas ejecutadas durante un turno de caja específico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ventas del turno obtenidas exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = VentaResponseDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Turno de caja no encontrado",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<List<VentaResponseDTO>> listarVentasPorTurno(
            @Parameter(description = "ID del turno de caja", example = "3") @PathVariable Long turnoId,
            @Parameter(hidden = true) Authentication auth
    );
}
