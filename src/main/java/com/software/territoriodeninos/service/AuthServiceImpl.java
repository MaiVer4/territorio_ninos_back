package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.entity.PasswordResetToken;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.CuentaBloqueadaException;
import com.software.territoriodeninos.exception.CuentaInactivaException;
import com.software.territoriodeninos.exception.CredencialesInvalidasException;
import com.software.territoriodeninos.exception.TokenInvalidoException;
import com.software.territoriodeninos.repository.PasswordResetTokenRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.security.JwtUtil;
import com.software.territoriodeninos.security.LoginAttemptService;
import com.software.territoriodeninos.util.EstadoUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del servicio de autenticación.
 * Responsabilidad única: gestionar el flujo de autenticación (login) y
 * la orquestación de la recuperación de contraseña.
 * El envío de emails se delega al {@link EmailService} (DIP/SRP).
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        String correo = loginRequestDTO.getCorreo();
        log.info("Intento de login para: {}", correo);

        if (loginAttemptService.isBlocked(correo)) {
            long minutos = loginAttemptService.getRemainingLockoutMinutes(correo);
            log.warn("Login bloqueado por fuerza bruta para: {} ({} minutos restantes)", correo, minutos);
            throw new CuentaBloqueadaException(
                    "Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intente en " + minutos + " minutos.");
        }

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null) {
            log.warn("Login fallido: usuario no encontrado - {}", correo);
            loginAttemptService.loginFailed(correo);
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            log.warn("Login fallido: cuenta inactiva - {}", correo);
            throw new CuentaInactivaException(
                    "Su cuenta ha sido desactivada. Por favor, comuníquese con el administrador.");
        }

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), usuario.getPassword())) {
            log.warn("Login fallido: contraseña incorrecta para - {}", correo);
            loginAttemptService.loginFailed(correo);
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        if (usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            log.error("Usuario {} tiene rol nulo en la base de datos", usuario.getId());
            throw new CredencialesInvalidasException("Error de configuración del usuario. Contacte al administrador.");
        }

        loginAttemptService.loginSucceeded(correo);

        String nombreRol = usuario.getRol().getNombre().name();
        String token = jwtUtil.generarToken(usuario);

        log.info("Login exitoso para: {}", correo);

        return AuthResponseDTO.builder()
                .token(token)
                .tipoToken("Bearer")
                .id(usuario.getId())
                .nombre(usuario.getNombre())
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

            String tokenPlano = UUID.randomUUID().toString();
            String tokenHash = sha256Hex(tokenPlano);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(tokenHash);
            resetToken.setUsuario(usuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));

            tokenRepository.save(resetToken);

            try {
                String resetUrl = frontendUrl + "/reset-password?token=" + tokenPlano;
                emailService.enviarCorreoRecuperacion(usuario.getCorreo(), resetUrl);
                log.info("Token de recuperación generado para: {}", correo);
            } catch (MailException e) {
                log.warn("No se pudo enviar el correo de recuperación a {}: {}", correo, e.getMessage());
            }
        } else {
            log.warn("Solicitud de recuperación para correo inexistente o inactivo: {}", correo);
        }
    }

    @Override
    @Transactional
    public void cambiarContrasena(String token, String nuevaContrasena) {
        String tokenHash = sha256Hex(token);
        PasswordResetToken resetToken = tokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> new TokenInvalidoException("Token inválido o no existe"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            log.warn("Intento de uso de token expirado");
            throw new TokenInvalidoException("El token ha expirado. Solicite uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));

        tokenRepository.delete(resetToken);
        log.info("Contraseña actualizada exitosamente para usuario ID: {}", usuario.getId());
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgarTokensExpirados() {
        tokenRepository.deleteByFechaExpiracionBefore(LocalDateTime.now());
        log.info("Purga de tokens de recuperación expirados ejecutada exitosamente.");
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error al inicializar algoritmo SHA-256", e);
        }
    }
}
