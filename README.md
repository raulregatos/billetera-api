# Billetera API

API REST de ejemplo para consultar cuentas, depositar, retirar y transferir saldo. Usa Java 21, Spring Boot, Spring Data JPA y PostgreSQL.

## Requisitos

- JDK 21
- Docker con Docker Compose

## Arranque local

1. Inicia PostgreSQL desde la raíz del proyecto:

   ```bash
   docker compose up -d
   ```

2. Inicia la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

   En Windows también puedes usar `mvnw.cmd spring-boot:run`.

   En el primer arranque, si no hay cuentas, se crean Alice (ID 1, saldo 500.00) y Bob (ID 2, saldo 200.00).

3. Abre la interfaz en [http://localhost:8080/](http://localhost:8080/) o Swagger UI en [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

Para detener PostgreSQL conserva los datos con `docker compose down`. Para borrar también los datos persistidos, ejecuta `docker compose down -v`.

## API

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/cuentas/{id}` | Consulta una cuenta |
| `POST` | `/api/cuentas/depositar` | Deposita en la cuenta indicada |
| `POST` | `/api/cuentas/retirar` | Retira de la cuenta indicada si tiene saldo suficiente |
| `POST` | `/api/cuentas/transferir` | Transfiere saldo entre dos cuentas distintas |

Ejemplo de depósito:

```http
POST /api/cuentas/depositar
Content-Type: application/json

{"idCuenta": 1, "monto": 25.00}
```

Los importes deben ser mayores que cero. Los errores de negocio se responden con HTTP 400 y una cuenta inexistente con HTTP 404.

## Concurrencia e integridad

Los depósitos, retiros y transferencias bloquean las filas afectadas con `PESSIMISTIC_WRITE` dentro de una transacción. Las transferencias adquieren los bloqueos por ID ascendente para reducir deadlocks. El cambio de saldo y el registro de su movimiento se confirman o revierten juntos.

## Configuración

La conexión local usa estos valores predeterminados, que coinciden con `compose.yaml`:

| Variable | Predeterminado |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/billetera` |
| `DB_USERNAME` | `admin` |
| `DB_PASSWORD` | `root` |

Sobrescribe estas variables para otros entornos. Los valores predeterminados son solo para desarrollo local. Hibernate mantiene `ddl-auto=update` para facilitar la ejecución local; en producción se recomienda gestionar el esquema con migraciones y desactivar la actualización automática.

## Pruebas

```bash
./mvnw test
```
