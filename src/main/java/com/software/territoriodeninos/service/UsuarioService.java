package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;

import java.util.List;

public interface UsuarioService {
    
    

    UsuarioResponseDTO registrar(UsuarioRegistroDTO registroDTO);
    
    

    List<UsuarioResponseDTO> obtenerTodos();
    
    

    UsuarioResponseDTO obtenerPorId(Long id);
    
    

    UsuarioResponseDTO obtenerPorCorreo(String correo);
    
    

    UsuarioResponseDTO actualizar(Long id, UsuarioUpdateDTO updateDTO);
    
    void cambiarPassword(Long id, com.software.territoriodeninos.dto.ChangePasswordRequest request);
    
    UsuarioResponseDTO inactivar(Long id);
}
