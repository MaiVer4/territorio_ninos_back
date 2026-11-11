package com.software.territoriodeninos.security;

import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserProvider {

    private final UsuarioRepository usuarioRepository;

    public Optional<Usuario> resolverUsuario(Authentication auth) {
        Authentication effectiveAuth = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
        if (effectiveAuth != null && effectiveAuth.isAuthenticated() && !"anonymousUser".equals(effectiveAuth.getPrincipal())) {
            return usuarioRepository.findByCorreo(effectiveAuth.getName());
        }
        return Optional.empty();
    }
}
