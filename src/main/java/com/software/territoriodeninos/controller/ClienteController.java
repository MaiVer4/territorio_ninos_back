package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController implements ClienteApi {

    private final ClienteService clienteService;

    @Override
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(
            @RequestParam(name = "documento", required = false) String documento,
            @RequestParam(name = "doc", required = false) String doc
    ) {
        String numDoc = (documento != null && !documento.isBlank()) ? documento : doc;
        ClienteResponseDTO response = clienteService.buscarPorDocumento(numDoc);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/documento/{documento}")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumentoPath(
            @PathVariable("documento") String documento
    ) {
        ClienteResponseDTO response = clienteService.buscarPorDocumento(documento);
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClienteRequestDTO request
    ) {
        ClienteResponseDTO response = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('TAQUILLERO', 'ADMINISTRADOR')")
    public ResponseEntity<ClienteResponseDTO> registrarCliente(
            @Valid @RequestBody ClienteRequestDTO request
    ) {
        ClienteResponseDTO response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
