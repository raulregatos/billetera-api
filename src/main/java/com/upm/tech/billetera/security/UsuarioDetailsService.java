package com.upm.tech.billetera.security;

import com.upm.tech.billetera.model.Usuario;
import com.upm.tech.billetera.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));
        return User.withUsername(usuario.getNombreUsuario())
                // El usuario demo heredado queda deshabilitado hasta configurar su contraseña.
                .password(usuario.getPasswordHash() == null ? "!disabled!" : usuario.getPasswordHash())
                .authorities("ROLE_USER")
                .disabled(!usuario.isHabilitado())
                .build();
    }
}
