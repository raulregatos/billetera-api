package com.upm.tech.billetera;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.repository.CuentaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class BilleteraApplication {

	public static void main(String[] args) {
		SpringApplication.run(BilleteraApplication.class, args);
	}


	// Este Bean se ejecuta automáticamente justo después de que arranque Spring Boot
	@Bean
    CommandLineRunner iniciarDatos(CuentaRepository repository) {
		return args -> {
			// Guardamos dos cuentas solo si la base de datos está vacía
			if (repository.count() == 0) {
				repository.save(new Cuenta("Alice", new BigDecimal("500.00"))); // ID 1
				repository.save(new Cuenta("Bob", new BigDecimal("200.00")));   // ID 2
				System.out.println("Cuentas de prueba creadas en la base de datos.");
			}
		};
	}
}
