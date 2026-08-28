package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.entity.PasswordResetToken;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.CredencialesInvalidasException;
import com.software.territoriodeninos.repository.PasswordResetTokenRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.util.EstadoUsuario;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    
    private final String FRONTEND_URL = "http://localhost:4200/reset-password?token=";
    
    public AuthServiceImpl(UsuarioRepository usuarioRepository, JwtUtil jwtUtil,
                            PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepository,
                            JavaMailSender mailSender) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
    }
    
    @Override
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Usuario usuario = usuarioRepository.findByCorreoAndEstado(loginRequestDTO.getCorreo(), EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));
        
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }
        
        String nombreRol = usuario.getRol().getNombre().name();
        String token = jwtUtil.generarToken(usuario.getCorreo(), nombreRol);
        
        return AuthResponseDTO.builder()
                .token(token)
                .tipoToken("Bearer")
                .correo(usuario.getCorreo())
                .rol(nombreRol)
                .build();
    }
    
    @Override
    @Transactional
    public void generarTokenRecuperacion(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoAndEstado(correo, EstadoUsuario.ACTIVO);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            tokenRepository.deleteByUsuarioId(usuario.getId());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsuario(usuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));

            tokenRepository.save(resetToken);
            enviarEmail(usuario.getCorreo(), token);
        }
    }

    @Override
    @Transactional
    public void cambiarContrasena(String token, String nuevaContrasena) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no existe"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("El token ha expirado. Solicite uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        tokenRepository.delete(resetToken);
    }

    private void enviarEmail(String destinatario, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperación de Contraseña - Territorio de Niños");
        mensaje.setText("Hola,\n\nHas solicitado restablecer tu contraseña. " +
                "Haz clic en el siguiente enlace para continuar (válido por 15 minutos):\n\n" +
                FRONTEND_URL + token + "\n\nSi no fuiste tú, ignora este mensaje.");
        
        mailSender.send(mensaje);
    }
}
