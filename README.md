# Planifi backend

## Requisitos

- Docker y Docker Compose
- Puertos disponibles: `8080` (API), `5432` (Postgres), `27017` (MongoDB)

## Ejecución con Docker Compose

Levanta la API junto con Postgres y MongoDB:

```bash
docker compose up --build
```

La API queda disponible en:

- Health check: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Usa la API key `dev-default-api-key` en la cabecera `X-API-Key` para
`/api/v1/*`.

## Construir la imagen del backend

Para construir la imagen localmente:

```bash
docker build -t planifi-backend:dev ./backend
```

La imagen expone el puerto `8080` y lee la configuración desde variables de
entorno. Las variables clave están detalladas en `docs/README.md`.

## Documentación adicional

- Guía de desarrollo del backend: `docs/README.md`
- Contrato OpenAPI: `docs/openapi/planifi-api-v1.yaml`
- Estándares de API: `docs/api-standards.md`
