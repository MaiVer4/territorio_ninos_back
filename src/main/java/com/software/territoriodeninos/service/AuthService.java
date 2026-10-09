package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;

public interface AuthService {
    AuthResponseDTO login(LoginRequestDTO loginRequestDTO);
    void generarTokenRecuperacion(String email);
    void cambiarContrasena(String token, String nuevaContrasena);
    void purgarTokensExpirados();
}
