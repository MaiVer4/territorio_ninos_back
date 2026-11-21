package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.tienda.CatalogoAtraccionOnlineDTO;
import com.software.territoriodeninos.dto.tienda.CompraOnlineRequestDTO;
import com.software.territoriodeninos.dto.tienda.CompraOnlineResponseDTO;
import com.software.territoriodeninos.dto.tienda.TicketOnlineDTO;
import com.software.territoriodeninos.service.TiendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tienda")
@RequiredArgsConstructor
@Tag(name = "Tienda Pública", description = "Endpoints públicos para compra de boletos en línea y consulta por acudientes")
public class TiendaPublicaController {

    private final TiendaService tiendaService;

    @GetMapping("/catalogo")
    @Operation(summary = "Obtener catálogo público de atracciones y tarifas para compra online")
    public ResponseEntity<List<CatalogoAtraccionOnlineDTO>> obtenerCatalogo() {
        return ResponseEntity.ok(tiendaService.obtenerCatalogoPublico());
    }

    @PostMapping("/comprar")
    @Operation(summary = "Procesar compra online de boletos por acudiente invitado (Guest Checkout)")
    public ResponseEntity<CompraOnlineResponseDTO> comprarBoletos(@Valid @RequestBody CompraOnlineRequestDTO request) {
        CompraOnlineResponseDTO response = tiendaService.procesarCompraOnline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-boletos")
    @Operation(summary = "Consultar boletos activos del día por documento y correo del acudiente")
    public ResponseEntity<List<TicketOnlineDTO>> consultarBoletos(
            @RequestParam String documento,
            @RequestParam(required = false) String correo
    ) {
        return ResponseEntity.ok(tiendaService.consultarBoletosActivos(documento, correo));
    }
}
