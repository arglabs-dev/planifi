# Guía de desarrollo backend (Spring Boot)

## Prerrequisitos

- Java 21
- Docker y Docker Compose
- Acceso a puerto 8080 para la API y 5432/27017 para servicios de datos

## Ejecución local rápida

1. Activar perfil `dev` con Postgres y Mongo en contenedores:

   ```sh
   docker compose up --build
   ```

2. La API expone `/actuator/health` y Swagger en
   `http://localhost:8080/swagger-ui.html`.
3. Usa la API key `dev-default-api-key` (cabecera `X-API-Key`) para llamadas a
   `/api/v1/*`.
4. El contrato OpenAPI vive en `docs/openapi/planifi-api-v1.yaml` y los
   estándares de API en `docs/api-standards.md`.

## Desarrollo sin contenedores

1. Levanta dependencias externas:
   - Postgres 16 en `jdbc:postgresql://localhost:5432/planifi`
   - MongoDB 7 en `mongodb://localhost:27017/planifi`
2. Carga variables de entorno mínimas:

   ```sh
   export SPRING_PROFILES_ACTIVE=dev
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/planifi
   export SPRING_DATASOURCE_USERNAME=planifi
   export SPRING_DATASOURCE_PASSWORD=planifi
   export PLANIFI_SECURITY_API_KEYS=dev-default-api-key
   ```

3. Compila y ejecuta:

   ```sh
   cd backend
   ./mvnw spring-boot:run
   ```

## Pruebas y calidad

- Ejecuta pruebas y migraciones contra H2 (perfil `test`):

  ```sh
  cd backend
  ./mvnw test
  ```

- Evita generar o subir binarios (por ejemplo, `target/` o
  `maven-wrapper.jar`) al repositorio; usa `git status` antes de commitear.

- Lint de documentación:

  ```sh
  npx markdownlint "docs/**/*.md"
  ```

## Estructura clave del backend

- `backend/src/main/java/com/planifi/backend/api`: controladores y DTOs HTTP.
- `backend/src/main/java/com/planifi/backend/application`: orquestación y servicios.
- `backend/src/main/java/com/planifi/backend/domain`: entidades de dominio.
- `backend/src/main/java/com/planifi/backend/infrastructure`: persistencia e integraciones.
- `backend/src/main/resources/db/migration`: migraciones Flyway.

## Variables y secretos

| Variable | Descripción | Ejemplo |
| --- | --- | --- |
| `PLANIFI_SECURITY_API_KEYS` | API keys separadas por comas | `dev-default-key` |
| `PLANIFI_SECURITY_API_KEY_HEADER` | Nombre de la cabecera para API key | `X-API-Key` |
| `SPRING_DATASOURCE_URL` | JDBC URL de Postgres | `jdbc:postgresql://db:5432/app` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base de datos | `planifi` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de base de datos | `planifi` |
| `SPRING_DATA_MONGODB_URI` | URI de MongoDB | `mongodb://mongo:27017/planifi` |

Mantén secretos fuera del repositorio y usa un vault gestionado en entornos no locales.
