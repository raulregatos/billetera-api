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

2. Configura la contraseña local de la cuenta `demo`, que es propietaria de las cuentas Alice y Bob heredadas. No existe una contraseña por defecto:

   ```powershell
   $env:DEMO_USER_PASSWORD = 'una-frase-secreta-de-12-caracteres-o-mas'
   ```

   Sin esta variable, puedes registrarte y usar cuentas nuevas, pero la cuenta `demo` permanece deshabilitada y las cuentas heredadas no se pueden consultar.

3. Inicia la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

   En Windows también puedes usar `mvnw.cmd spring-boot:run`.

   En el primer arranque, si no hay cuentas, se crean Alice (ID 1, saldo 500.00) y Bob (ID 2, saldo 200.00).

4. Abre la interfaz en [http://localhost:8080/](http://localhost:8080/) o Swagger UI en [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

La web permite registrarse con un nombre de usuario de 3 a 32 caracteres y una contraseña de al menos 12 caracteres. El usuario recibe una cuenta con saldo cero y puede crear más. Para iniciar sesión en los datos de demostración usa el usuario `demo` y la contraseña que configuraste arriba.

Para detener PostgreSQL conserva los datos con `docker compose down`. Para borrar también los datos persistidos, ejecuta `docker compose down -v`.

## API

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/auth/csrf` | Obtiene el token CSRF necesario para peticiones que modifican datos |
| `POST` | `/api/auth/register` | Registra un usuario y abre su primera cuenta |
| `POST` | `/api/auth/login` | Inicia sesión |
| `POST` | `/api/auth/logout` | Cierra la sesión actual |
| `GET` | `/api/auth/me` | Consulta el usuario autenticado |
| `GET` | `/api/cuentas` | Lista las cuentas propias |
| `POST` | `/api/cuentas` | Crea una cuenta propia con saldo cero |
| `GET` | `/api/cuentas/{id}` | Consulta una cuenta propia |
| `GET` | `/api/cuentas/{id}/transacciones` | Consulta el historial paginado de una cuenta propia |
| `POST` | `/api/cuentas/depositar` | Deposita en la cuenta indicada |
| `POST` | `/api/cuentas/retirar` | Retira de la cuenta indicada si tiene saldo suficiente |
| `POST` | `/api/cuentas/transferir` | Transfiere saldo entre dos cuentas distintas |

Las rutas de cuentas requieren sesión. El usuario solo puede consultar y operar sus propias cuentas; en una transferencia, el origen debe ser propio y el destino puede ser de otro usuario. Una cuenta ajena se responde como inexistente (`404`) para no revelar su existencia. Las contraseñas se almacenan como hashes BCrypt y no se devuelven en respuestas.

La autenticación usa una cookie de sesión `HttpOnly` y `SameSite=Lax`. Tras obtener el token con `GET /api/auth/csrf`, envíalo en el encabezado `X-CSRF-TOKEN` en cada `POST`. El token debe volver a consultarse después de iniciar o cerrar sesión. El perfil local permite HTTP; los perfiles no locales mantienen `Secure=true` para la cookie.

Ejemplo de depósito:

```http
POST /api/cuentas/depositar
Content-Type: application/json

{"idCuenta": 1, "monto": 25.00}
```

Los importes deben ser mayores que cero. Los errores de negocio se responden con HTTP 400 y una cuenta inexistente con HTTP 404.

El historial acepta `tipo`, `desde` y `hasta` como filtros opcionales. Las fechas usan `yyyy-MM-dd` y ambos límites son inclusivos. `page` empieza en cero y `size` usa 20 por defecto (máximo 100). Los resultados incluyen entradas y salidas de la cuenta y se ordenan por fecha e ID, ambos descendentes.

Ejemplo: `/api/cuentas/1/transacciones?tipo=TRANSFERENCIA&desde=2026-10-01&hasta=2026-10-31&page=0&size=20`.

## Concurrencia e integridad

Los depósitos, retiros y transferencias bloquean las filas afectadas con `PESSIMISTIC_WRITE` dentro de una transacción. Las transferencias adquieren los bloqueos por ID ascendente para reducir deadlocks. El cambio de saldo y el registro de su movimiento se confirman o revierten juntos.

## Migraciones y datos heredados

Flyway administra el esquema y Hibernate lo valida al iniciar. La migración inicial refleja el esquema anterior; la siguiente crea el usuario `demo`, vincula a él todas las cuentas existentes y conserva saldos e historial. En una base nueva, si se define `DEMO_USER_PASSWORD`, se crean Alice y Bob como cuentas de `demo`. La contraseña se convierte a BCrypt al arrancar y no se guarda en los scripts SQL.

## Configuración

La conexión local usa estos valores predeterminados, que coinciden con `compose.yaml` y son solo para desarrollo:

| Variable | Predeterminado |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/billetera` |
| `DB_USERNAME` | `admin` |
| `DB_PASSWORD` | `root` |
| `DEMO_USER_PASSWORD` | Sin valor predeterminado; habilita el usuario local `demo` |

Sobrescribe estas variables para otros entornos. Los valores predeterminados de conexión son solo para desarrollo local. Flyway administra el esquema en todos los perfiles y Hibernate usa `ddl-auto=validate`; en entornos no locales la cookie de sesión conserva el atributo `Secure`.

## Pruebas

La suite habitual ejecuta las pruebas unitarias y de integración existentes. Requiere una base PostgreSQL configurada con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` (por defecto, la base local de desarrollo):

```bash
./mvnw test
```

Las pruebas de extremo a extremo se ejecutan aparte en el perfil `e2e`. Requieren Docker, porque el test de navegador inicia su propio PostgreSQL temporal con Testcontainers; esa base se elimina al finalizar y no utiliza los datos locales. Playwright necesita descargar Chromium una vez:

```bash
./mvnw -Pe2e exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  "-Dexec.args=install chromium" \
  -Dexec.classpathScope=test
./mvnw verify -Pe2e
```

En Windows, sustituye `./mvnw` por `mvnw.cmd`. El perfil `e2e` incluye tanto la suite normal (que usa la base definida por `DB_URL`) como las pruebas del navegador (que usan un contenedor independiente). Las capturas de pantalla de pruebas de navegador fallidas se guardan en `target/e2e-artifacts/`.
