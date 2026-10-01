package com.upm.tech.billetera.repository;

import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.model.Transaccion;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

public final class TransaccionSpecifications {

    private TransaccionSpecifications() {
    }

    public static Specification<Transaccion> deCuenta(
            Long cuentaId,
            TipoTransaccion tipo,
            LocalDateTime desdeInclusivo,
            LocalDateTime hastaExclusivo
    ) {
        return (root, query, criteriaBuilder) -> {
            var condiciones = new ArrayList<Predicate>();
            condiciones.add(criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("cuentaOrigen").get("id"), cuentaId),
                    criteriaBuilder.equal(root.get("cuentaDestino").get("id"), cuentaId)
            ));
            if (tipo != null) {
                condiciones.add(criteriaBuilder.equal(root.get("tipo"), tipo));
            }
            if (desdeInclusivo != null) {
                condiciones.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), desdeInclusivo));
            }
            if (hastaExclusivo != null) {
                condiciones.add(criteriaBuilder.lessThan(root.get("fecha"), hastaExclusivo));
            }
            return criteriaBuilder.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
