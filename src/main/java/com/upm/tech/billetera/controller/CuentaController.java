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
        // TODO 1: Llama a cuentaService.obtenerCuenta(id)
        Cuenta cuenta = cuentaService.obtenerCuenta(id);
        // TODO 2: Retorna la cuenta dentro de un ResponseEntity.ok(...)
        return ResponseEntity.ok(cuenta);
    }

    // Endpoint 2: Hacer transferencia
    // POST http://localhost:8080/api/cuentas/transferir
    @PostMapping("/transferir")
    public ResponseEntity<String> transferir(@Valid @RequestBody TransferenciaRequest request) {
        // Explicación mágica:
        // @RequestBody coge el JSON que manda el cliente y lo convierte a tu objeto TransferenciaRequest.
        // @Valid comprueba las reglas que pusimos (@NotNull, @Positive). Si fallan, Spring devuelve un 400 Bad Request automáticamente.

        // TODO 3: Llama al método transferir de cuentaService pasando los datos del 'request'
        // Pista: usa request.idOrigen(), request.idDestino(), request.monto()
        cuentaService.transferir(request.idOrigen(), request.idDestino(), request.monto());

        // TODO 4: Si todo va bien, devuelve ResponseEntity.ok("Transferencia realizada con éxito");
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
