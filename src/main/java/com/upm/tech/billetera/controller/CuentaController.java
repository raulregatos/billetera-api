package com.upm.tech.billetera.controller;

import com.upm.tech.billetera.dto.CrearCuentaRequest;
import com.upm.tech.billetera.dto.CuentaResponse;
import com.upm.tech.billetera.dto.MontoRequest;
import com.upm.tech.billetera.dto.TransaccionesPageResponse;
import com.upm.tech.billetera.dto.TransferenciaRequest;
import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public List<CuentaResponse> listarCuentas(@AuthenticationPrincipal UserDetails usuario) {
        return cuentaService.listarCuentas(usuario.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaResponse crearCuenta(@Valid @RequestBody CrearCuentaRequest request,
                                      @AuthenticationPrincipal UserDetails usuario) {
        return cuentaService.crearCuenta(request.titular(), usuario.getUsername());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponse> obtenerCuenta(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(cuentaService.obtenerCuenta(id, usuario.getUsername()));
    }

    @GetMapping("/{id}/transacciones")
    public ResponseEntity<TransaccionesPageResponse> obtenerHistorial(
            @PathVariable Long id,
            @RequestParam(required = false) TipoTransaccion tipo,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails usuario
    ) {
        return ResponseEntity.ok(cuentaService.obtenerHistorial(
                id, tipo, desde, hasta, page, size, usuario.getUsername()));
    }

    @PostMapping("/transferir")
    public ResponseEntity<String> transferir(@Valid @RequestBody TransferenciaRequest request,
                                             @AuthenticationPrincipal UserDetails usuario) {
        cuentaService.transferir(request.idOrigen(), request.idDestino(), request.monto(), usuario.getUsername());
        return ResponseEntity.ok("Transferencia realizada con éxito");
    }

    @PostMapping("/depositar")
    public ResponseEntity<CuentaResponse> depositar(@Valid @RequestBody MontoRequest request,
                                                    @AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(cuentaService.depositar(
                request.idCuenta(), request.monto(), usuario.getUsername()));
    }

    @PostMapping("/retirar")
    public ResponseEntity<CuentaResponse> retirar(@Valid @RequestBody MontoRequest request,
                                                  @AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(cuentaService.retirar(
                request.idCuenta(), request.monto(), usuario.getUsername()));
    }
}
