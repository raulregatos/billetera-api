package com.upm.tech.billetera.service;

import com.upm.tech.billetera.dto.AutenticacionResponse;
import com.upm.tech.billetera.dto.RegistroRequest;
import com.upm.tech.billetera.dto.UsuarioResponse;
import com.upm.tech.billetera.exception.UsuarioYaExisteException;
import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.model.Usuario;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;

    public AutenticacionService(UsuarioRepository usuarioRepository, CuentaRepository cuentaRepository,
                                PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                                SessionAuthenticationStrategy sessionAuthenticationStrategy,
                                SecurityContextRepository securityContextRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public AutenticacionResponse registrar(RegistroRequest request, HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse) {
        validarBytesContrasena(request.contrasena());
        String username = normalizarUsuario(request.usuario());
        if (usuarioRepository.existsByNombreUsuario(username)) {
            throw new UsuarioYaExisteException(username);
        }

        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(
                username, passwordEncoder.encode(request.contrasena()), true));
        cuentaRepository.save(new Cuenta(request.titular().trim(), BigDecimal.ZERO, usuario));
        iniciarSesion(username, request.contrasena(), servletRequest, servletResponse);
        return new AutenticacionResponse(new UsuarioResponse(username));
    }

    public AutenticacionResponse iniciarSesion(String username, String password,
                                               HttpServletRequest request, HttpServletResponse response) {
        validarBytesContrasena(password);
        String usuarioNormalizado = normalizarUsuario(username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(usuarioNormalizado, password));
            request.getSession(true);
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            return new AutenticacionResponse(new UsuarioResponse(usuarioNormalizado));
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            throw new CredencialesInvalidasException();
        }
    }

    private String normalizarUsuario(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validarBytesContrasena(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña no puede superar 72 bytes en UTF-8");
        }
    }

    public static class CredencialesInvalidasException extends AuthenticationServiceException {
        public CredencialesInvalidasException() {
            super("Usuario o contraseña incorrectos");
        }
    }
}
