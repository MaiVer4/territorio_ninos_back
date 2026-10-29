package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.CatalogoAtraccionVentaDTO;
import com.software.territoriodeninos.dto.CotizacionRequestDTO;
import com.software.territoriodeninos.dto.CotizacionResponseDTO;
import com.software.territoriodeninos.dto.ItemVentaValidacionDTO;
import com.software.territoriodeninos.dto.ResultadoValidacionVentaDTO;
import com.software.territoriodeninos.dto.VentaProcesarRequestDTO;
import com.software.territoriodeninos.dto.VentaResponseDTO;
import com.software.territoriodeninos.service.AforoValidatorService;
import com.software.territoriodeninos.service.AtraccionService;
import com.software.territoriodeninos.service.VentaCalculoService;
import com.software.territoriodeninos.service.VentaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController implements VentaApi {

    private final AtraccionService atraccionService;
    private final AforoValidatorService aforoValidatorService;
    private final VentaCalculoService ventaCalculoService;
    private final VentaService ventaService;

    @Override
    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<List<CatalogoAtraccionVentaDTO>> obtenerCatalogo() {
        List<CatalogoAtraccionVentaDTO> catalogo = atraccionService.obtenerCatalogoVenta();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS).mustRevalidate().cachePrivate())
                .body(catalogo);
    }

    @Override
    @PostMapping("/validar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<ResultadoValidacionVentaDTO> validarVenta(
            @Valid @RequestBody @NotEmpty(message = "La lista de ítems a validar no puede estar vacía") List<@Valid ItemVentaValidacionDTO> items
    ) {
        return ResponseEntity.ok(aforoValidatorService.validarItemsVenta(items));
    }

    @Override
    @PostMapping("/cotizar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<CotizacionResponseDTO> cotizarVenta(
            @Valid @RequestBody CotizacionRequestDTO request
    ) {
        return ResponseEntity.ok(ventaCalculoService.cotizarVenta(request));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<VentaResponseDTO> procesarVenta(
            @Valid @RequestBody VentaProcesarRequestDTO request,
            Authentication auth
    ) {
        VentaResponseDTO venta = ventaService.procesarVenta(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(venta);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<VentaResponseDTO> obtenerVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerVentaPorId(id));
    }

    @Override
    @GetMapping("/turno/{turnoId}")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<List<VentaResponseDTO>> listarVentasPorTurno(
            @PathVariable Long turnoId,
            Authentication auth
    ) {
        return ResponseEntity.ok(ventaService.listarVentasPorTurno(turnoId, auth));
    }
}
