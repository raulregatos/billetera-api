package com.upm.tech.billetera;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.model.Usuario;
import com.upm.tech.billetera.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@SpringBootApplication
public class BilleteraApplication {

	public static void main(String[] args) {
		SpringApplication.run(BilleteraApplication.class, args);
	}


	// Este Bean se ejecuta automáticamente justo después de que arranque Spring Boot
	@Bean
    @Profile("local")
    CommandLineRunner iniciarDatos(CuentaRepository cuentas, UsuarioRepository usuarios,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${DEMO_USER_PASSWORD:}") String demoPassword) {
		return args -> {
			if (demoPassword == null || demoPassword.isBlank()) {
				System.out.println("Usuario demo deshabilitado: configura DEMO_USER_PASSWORD para habilitarlo.");
				return;
			}

			Usuario demo = usuarios.findByNombreUsuario("demo").orElseThrow();
			demo.setPasswordHash(passwordEncoder.encode(demoPassword));
			demo.setHabilitado(true);
			usuarios.save(demo);

			if (cuentas.count() == 0) {
				cuentas.save(new Cuenta("Alice", new BigDecimal("500.00"), demo));
				cuentas.save(new Cuenta("Bob", new BigDecimal("200.00"), demo));
				System.out.println("Cuentas de prueba creadas para el usuario demo.");
			}
		};
	}
}
