package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.ClienteRepository;
import com.software.territoriodeninos.repository.MenorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final MenorRepository menorRepository;

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new RecursoNoEncontradoException("El número de documento de búsqueda no puede estar vacío");
        }

        String docTrimmed = documento.trim();
        Cliente cliente = clienteRepository.findByNumeroDocumentoWithMenores(docTrimmed)
                .orElseGet(() -> clienteRepository.findByNumeroDocumento(docTrimmed)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No se encontró ningún cliente registrado con el documento: " + docTrimmed
                        )));

        return ClienteResponseDTO.fromEntity(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO registrarCliente(ClienteRequestDTO request) {
        String doc = request.numeroDocumento() != null ? request.numeroDocumento().trim() : "";

        if (clienteRepository.existsByNumeroDocumento(doc)) {
            throw new ConflictoException("Ya existe un cliente registrado con el número de documento: " + doc);
        }

        Cliente cliente = Cliente.builder()
                .numeroDocumento(doc)
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .telefono(request.telefono() != null ? request.telefono().trim() : null)
                .correo(request.correo() != null ? request.correo().trim().toLowerCase() : null)
                .build();

        if (request.menores() != null && !request.menores().isEmpty()) {
            for (MenorDTO menorDTO : request.menores()) {
                Menor menor = menorDTO.toEntity();
                cliente.addMenor(menor);
            }
        }

        Cliente guardado = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(guardado);
    }
}
