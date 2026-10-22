package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;

public interface ClienteService {

    /**
     * Busca un cliente por su número de documento de identidad.
     *
     * @param documento Número de documento a buscar
     * @return DTO con la información del cliente y sus menores
     * @throws com.software.territoriodeninos.exception.RecursoNoEncontradoException si el cliente no existe
     */
    ClienteResponseDTO buscarPorDocumento(String documento);

    /**
     * Registra un nuevo cliente acudiente y opcionalmente sus menores a cargo.
     *
     * @param request Datos del cliente y menores a registrar
     * @return DTO con la información del cliente registrado
     * @throws com.software.territoriodeninos.exception.ConflictoException si ya existe un cliente con el mismo documento
     */
    ClienteResponseDTO registrarCliente(ClienteRequestDTO request);

    /**
     * Actualiza los datos de contacto y menores asociados de un cliente existente.
     *
     * @param id Identificador único del cliente
     * @param request Datos del cliente y menores a actualizar
     * @return DTO con la información del cliente actualizado
     * @throws com.software.territoriodeninos.exception.RecursoNoEncontradoException si el cliente no existe
     */
    ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request);
}
