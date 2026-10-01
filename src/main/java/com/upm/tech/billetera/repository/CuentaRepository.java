package com.upm.tech.billetera.repository;

import com.upm.tech.billetera.model.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    List<Cuenta> findAllByUsuarioNombreUsuarioOrderByIdAsc(String nombreUsuario);

    Optional<Cuenta> findByIdAndUsuarioNombreUsuario(Long id, String nombreUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.id = :id AND c.usuario.nombreUsuario = :nombreUsuario")
    Optional<Cuenta> findByIdAndUsuarioConBloqueo(@Param("id") Long id,
                                                   @Param("nombreUsuario") String nombreUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.id = :id")
    Optional<Cuenta> findByIdConBloqueo(Long id);
}
