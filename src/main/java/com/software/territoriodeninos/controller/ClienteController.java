package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para registro y consulta de clientes y sus menores a cargo")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    @Operation(summary = "Buscar cliente por documento", description = "Obtiene los datos de un cliente y sus menores a cargo a partir de su número de identificación")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(
            @Parameter(description = "Número de documento del cliente a buscar", required = true)
            @RequestParam(name = "documento", required = false) String documento,
            @RequestParam(name = "doc", required = false) String doc
    ) {
        String numDoc = (documento != null && !documento.isBlank()) ? documento : doc;
        ClienteResponseDTO response = clienteService.buscarPorDocumento(numDoc);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar nuevo cliente", description = "Registra un cliente acudiente y opcionalmente sus menores a cargo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Cliente ya registrado con este documento"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ClienteResponseDTO> registrarCliente(
            @Valid @RequestBody ClienteRequestDTO request
    ) {
        ClienteResponseDTO response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
