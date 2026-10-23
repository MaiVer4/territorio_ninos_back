package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.ClienteRequestDTO;
import com.software.territoriodeninos.dto.ClienteResponseDTO;
import com.software.territoriodeninos.dto.MenorDTO;
import com.software.territoriodeninos.entity.Cliente;
import com.software.territoriodeninos.entity.Menor;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.ClienteRepository;
import com.software.territoriodeninos.repository.MenorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

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
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró ningún cliente registrado con el documento: " + docTrimmed
                ));

        return ClienteResponseDTO.fromEntity(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente con ID " + id + " no encontrado"));

        if (StringUtils.hasText(request.nombres())) {
            cliente.setNombres(request.nombres().trim());
        }
        if (StringUtils.hasText(request.apellidos())) {
            cliente.setApellidos(request.apellidos().trim());
        }
        if (request.telefono() != null) {
            cliente.setTelefono(request.telefono().trim());
        }
        if (request.correo() != null) {
            cliente.setCorreo(request.correo().trim().toLowerCase());
        }

        vincularOActualizarMenores(cliente, request.menores());

        Cliente guardado = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(guardado);
    }

    @Override
    @Transactional
    public ClienteResponseDTO registrarCliente(ClienteRequestDTO request) {
        String doc = request.numeroDocumento() != null ? request.numeroDocumento().trim() : "";

        Optional<Cliente> optCliente = clienteRepository.findByNumeroDocumentoWithMenores(doc);
        if (optCliente.isEmpty()) {
            optCliente = clienteRepository.findByNumeroDocumento(doc);
        }

        Cliente cliente;
        if (optCliente.isPresent()) {
            // Notion CA-04: Acudiente ya registrado -> recuperar entidad y actualizar contacto
            cliente = optCliente.get();
            if (StringUtils.hasText(request.nombres())) {
                cliente.setNombres(request.nombres().trim());
            }
            if (StringUtils.hasText(request.apellidos())) {
                cliente.setApellidos(request.apellidos().trim());
            }
            if (request.telefono() != null) {
                cliente.setTelefono(request.telefono().trim());
            }
            if (request.correo() != null) {
                cliente.setCorreo(request.correo().trim().toLowerCase());
            }
        } else {
            // Acudiente nuevo
            cliente = Cliente.builder()
                    .numeroDocumento(doc)
                    .nombres(request.nombres() != null ? request.nombres().trim() : "")
                    .apellidos(request.apellidos() != null ? request.apellidos().trim() : "")
                    .telefono(request.telefono() != null ? request.telefono().trim() : null)
                    .correo(request.correo() != null ? request.correo().trim().toLowerCase() : null)
                    .build();
        }

        vincularOActualizarMenores(cliente, request.menores());

        Cliente guardado = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(guardado);
    }

    /**
     * Notion CA-03 & CA-04:
     * Valida la regla de edad de cada menor (1 a 17 años) y los vincula a la colección del cliente.
     * Si el menor ya existe (por ID, documento o documento+nombres), actualiza sus datos.
     * Si no existe, crea una nueva entidad Menor asociada al cliente.
     */
    private void vincularOActualizarMenores(Cliente cliente, List<MenorDTO> menoresDTO) {
        if (menoresDTO == null || menoresDTO.isEmpty()) {
            return;
        }

        for (MenorDTO menorDTO : menoresDTO) {
            Integer edad = validarEdadMenor(menorDTO);
            Optional<Menor> menorExistenteOpt = buscarMenorCoincidente(cliente, menorDTO);

            if (menorExistenteOpt.isPresent()) {
                Menor existente = menorExistenteOpt.get();
                if (StringUtils.hasText(menorDTO.nombreCompleto())) {
                    existente.setNombreCompleto(menorDTO.nombreCompleto().trim());
                }
                if (StringUtils.hasText(menorDTO.numeroDocumento())) {
                    existente.setNumeroDocumento(menorDTO.numeroDocumento().trim());
                }
                if (menorDTO.fechaNacimiento() != null) {
                    existente.setFechaNacimiento(menorDTO.fechaNacimiento());
                }
                existente.setEdad(edad);
            } else {
                Menor nuevoMenor = menorDTO.toEntity();
                nuevoMenor.setEdad(edad);
                cliente.addMenor(nuevoMenor);
            }
        }
    }

    /**
     * Notion CA-03: Regla de edad de menores (1 a 17 años inclusive).
     */
    private Integer validarEdadMenor(MenorDTO menorDTO) {
        if (menorDTO == null) {
            throw new ReglaNegocioException("La edad del menor debe estar comprendida entre 1 y 17 años.");
        }
        Integer edad = menorDTO.edad();
        if (edad == null && menorDTO.fechaNacimiento() != null) {
            edad = Period.between(menorDTO.fechaNacimiento(), LocalDate.now()).getYears();
        }
        if (edad == null || edad < 1 || edad > 17) {
            throw new ReglaNegocioException("La edad del menor debe estar comprendida entre 1 y 17 años.");
        }
        return edad;
    }

    /**
     * Notion CA-04: Localiza un menor en la colección del acudiente por:
     * 1. Coincidencia de ID.
     * 2. Coincidencia de documento de identidad.
     * 3. Coincidencia de documento y nombres, o nombres completos si no tiene documento.
     */
    private Optional<Menor> buscarMenorCoincidente(Cliente cliente, MenorDTO menorDTO) {
        if (cliente.getMenores() == null || cliente.getMenores().isEmpty()) {
            return Optional.empty();
        }

        return cliente.getMenores().stream().filter(m -> {
            if (menorDTO.id() != null && menorDTO.id().equals(m.getId())) {
                return true;
            }

            boolean dtoTieneDoc = StringUtils.hasText(menorDTO.numeroDocumento());
            boolean entTieneDoc = StringUtils.hasText(m.getNumeroDocumento());

            if (dtoTieneDoc && entTieneDoc) {
                if (menorDTO.numeroDocumento().trim().equalsIgnoreCase(m.getNumeroDocumento().trim())) {
                    return true;
                }
            }

            if (StringUtils.hasText(menorDTO.nombreCompleto()) && StringUtils.hasText(m.getNombreCompleto())) {
                boolean nombresCoinciden = menorDTO.nombreCompleto().trim().equalsIgnoreCase(m.getNombreCompleto().trim());
                if (nombresCoinciden) {
                    if (dtoTieneDoc && entTieneDoc) {
                        return menorDTO.numeroDocumento().trim().equalsIgnoreCase(m.getNumeroDocumento().trim());
                    }
                    return true;
                }
            }

            return false;
        }).findFirst();
    }
}
