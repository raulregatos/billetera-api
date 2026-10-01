package com.upm.tech.billetera.controller;

import com.upm.tech.billetera.dto.MontoRequest;
import com.upm.tech.billetera.dto.TransferenciaRequest;
import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Indica que esta clase maneja peticiones HTTP y devuelve JSON
@RequestMapping("/api/cuentas") // Todas las URLs empezarán por /api/cuentas
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    // Endpoint 1: Consultar cuenta
    // GET http://localhost:8080/api/cuentas/1
    @GetMapping("/{id}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable Long id) {
        Cuenta cuenta = cuentaService.obtenerCuenta(id);
        return ResponseEntity.ok(cuenta);
    }

    // Endpoint 2: Hacer transferencia
    // POST http://localhost:8080/api/cuentas/transferir
    @PostMapping("/transferir")
    public ResponseEntity<String> transferir(@Valid @RequestBody TransferenciaRequest request) {
        cuentaService.transferir(request.idOrigen(), request.idDestino(), request.monto());
        return ResponseEntity.ok("Transferencia realizada con éxito");
    }

    // Endpoint 3: Depositar
    // POST http://localhost:8080/api/cuentas/depositar
    @PostMapping("/depositar")
    public ResponseEntity<Cuenta> depositar(@Valid @RequestBody MontoRequest request) {
        Cuenta cuenta = cuentaService.depositar(request.idCuenta(), request.monto());
        return ResponseEntity.ok(cuenta);
    }

    // Endpoint 4: Retirar
    // POST http://localhost:8080/api/cuentas/retirar
    @PostMapping("/retirar")
    public ResponseEntity<Cuenta> retirar(@Valid @RequestBody MontoRequest request) {
        Cuenta cuenta = cuentaService.retirar(request.idCuenta(), request.monto());
        return ResponseEntity.ok(cuenta);
    }
}
