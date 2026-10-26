package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Clientes", description = "Endpoints para registro y consulta de clientes y sus menores a cargo")
public interface ClienteApi {

    @Operation(summary = "Buscar cliente por documento", description = "Obtiene los datos de un cliente y sus menores a cargo a partir de su número de identificación")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<ClienteResponseDTO> buscarPorDocumento(
            @Parameter(description = "Número de documento del cliente a buscar", required = false)
            @RequestParam(name = "documento", required = false) String documento,
            @Parameter(description = "Alias para el número de documento", required = false)
            @RequestParam(name = "doc", required = false) String doc
    );

    @Operation(summary = "Buscar cliente por documento (Path)", description = "Obtiene los datos de un cliente a partir de su número de identificación en la ruta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<ClienteResponseDTO> buscarPorDocumentoPath(
            @Parameter(description = "Número de documento del cliente a buscar", required = true)
            @PathVariable("documento") String documento
    );

    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente y agrega o actualiza menores asociados")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClienteRequestDTO request
    );

    @Operation(summary = "Registrar nuevo cliente", description = "Registra un cliente acudiente y opcionalmente sus menores a cargo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Cliente ya registrado con este documento",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "No autorizado",
            content = @Content(mediaType = "application/json"))
    })
    ResponseEntity<ClienteResponseDTO> registrarCliente(
            @Valid @RequestBody ClienteRequestDTO request
    );
}
