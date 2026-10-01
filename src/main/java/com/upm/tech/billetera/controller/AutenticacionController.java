package com.upm.tech.billetera.controller;

import com.upm.tech.billetera.dto.AutenticacionResponse;
import com.upm.tech.billetera.dto.CsrfTokenResponse;
import com.upm.tech.billetera.dto.LoginRequest;
import com.upm.tech.billetera.dto.RegistroRequest;
import com.upm.tech.billetera.dto.UsuarioResponse;
import com.upm.tech.billetera.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse obtenerTokenCsrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AutenticacionResponse registrar(@Valid @RequestBody RegistroRequest request,
                                          HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse) {
        return autenticacionService.registrar(request, servletRequest, servletResponse);
    }

    @PostMapping("/login")
    public AutenticacionResponse login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        return autenticacionService.iniciarSesion(
                request.usuario(), request.contrasena(), servletRequest, servletResponse);
    }

    @GetMapping("/me")
    public UsuarioResponse usuarioActual(@AuthenticationPrincipal UserDetails usuario) {
        return new UsuarioResponse(usuario.getUsername());
    }
}
