# Billetera API
Backend Transaccional y Control de Concurrencia

Sistema backend diseñado para la gestión de operaciones financieras y control de saldo en una billetera virtual. La arquitectura del proyecto está enfocada en garantizar la integridad de los datos frente a transacciones concurrentes, evitando condiciones de carrera y bloqueos mutuos (deadlocks).

---

### Stack Técnico

*   **Núcleo:** Java 21, Spring Boot
*   **Persistencia:** PostgreSQL, SQL
*   **Infraestructura:** Docker
*   **Documentación:** OpenAPI (Swagger)

---

### Arquitectura y Concurrencia

El principal reto técnico resuelto en este proyecto es la gestión de la concurrencia. Para asegurar que las operaciones de retiro, depósito y transferencia mantengan la consistencia matemática del saldo bajo peticiones simultáneas, se ha implementado un control de concurrencia mediante **Bloqueo Pesimista (Pessimistic Locking)** a nivel de fila en la base de datos relacional.

---

### Ejecución y Despliegue (Entorno Local)

El proyecto está contenerizado para facilitar su levantamiento sin necesidad de instalar bases de datos locales.

**1. Levantar la infraestructura de datos**  
Inicia el contenedor de PostgreSQL ejecutando el siguiente comando en la raíz del proyecto:
```bash
docker compose up -d
