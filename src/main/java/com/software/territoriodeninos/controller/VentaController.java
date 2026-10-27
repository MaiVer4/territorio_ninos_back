package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO;
import com.software.territoriodeninos.service.AtraccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas y Taquilla", description = "Endpoints para catálogo de atracciones y operaciones de venta en taquilla")
public class VentaController {

    private final AtraccionService atraccionService;

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
}
