package com.upm.tech.billetera.repository;

import com.upm.tech.billetera.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
}
