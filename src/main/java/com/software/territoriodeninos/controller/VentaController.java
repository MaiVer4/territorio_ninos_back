package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO;
import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;
import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;
import com.software.territoriodeninos.service.AforoValidatorService;
import com.software.territoriodeninos.service.AtraccionService;
import com.software.territoriodeninos.service.VentaCalculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas y Taquilla", description = "Endpoints para catálogo de atracciones y operaciones de venta en taquilla")
public class VentaController {

    private final AtraccionService atraccionService;
    private final AforoValidatorService aforoValidatorService;
    private final VentaCalculoService ventaCalculoService;

    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    @Operation(
        summary = "Obtener catálogo de atracciones para venta",
        description = "Retorna las atracciones en estado ACTIVO con sus tarifas vigentes ordenadas (tiempo y evento) y el aforo disponible en tiempo real"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catálogo obtenido exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<CatalogoAtraccionVentaDTO>> obtenerCatalogo() {
        List<CatalogoAtraccionVentaDTO> catalogo = atraccionService.obtenerCatalogoVenta();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS).mustRevalidate().cachePrivate())
                .body(catalogo);
    }

    @PostMapping("/validar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    @Operation(
        summary = "Validar aforo y consistencia de tarifas de una venta",
        description = "Comprueba que la suma de tickets por atracción no supere la capacidad máxima y que los precios coincidan con la base de datos"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validación exitosa"),
        @ApiResponse(responseCode = "400", description = "Exceso de aforo, tarifa inactiva o adulteración de precios detectada"),
        @ApiResponse(responseCode = "404", description = "Atracción o tarifa no encontrada")
    })
    public ResponseEntity<ResultadoValidacionVentaDTO> validarVenta(
            @Valid @RequestBody @NotEmpty(message = "La lista de ítems a validar no puede estar vacía") List<@Valid ItemVentaValidacionDTO> items
    ) {
        return ResponseEntity.ok(aforoValidatorService.validarItemsVenta(items));
    }

    @PostMapping("/cotizar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    @Operation(
        summary = "Cotizar venta previamente",
        description = "Calcula subtotales, base gravable, impuestos dinámicos según ConfiguracionParque y total neto con redondeo comercial HALF_UP"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cotización calculada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Valores nulos, negativos o inconsistencia en tarifas"),
        @ApiResponse(responseCode = "404", description = "Atracción o tarifa referenciada no encontrada")
    })
    public ResponseEntity<CotizacionResponseDTO> cotizarVenta(
            @Valid @RequestBody CotizacionRequestDTO request
    ) {
        return ResponseEntity.ok(ventaCalculoService.cotizarVenta(request));
    }
}
