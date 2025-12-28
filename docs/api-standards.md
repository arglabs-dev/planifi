# Estándares de API REST (Planifi)

## Alcance

Estos lineamientos aplican a la API REST pública de Planifi bajo `/api/v1`.
Son la referencia para nuevas rutas y para contratos OpenAPI.

## Versionado

- Todas las rutas públicas deben vivir bajo `/api/v1/...`.
- Cambios incompatibles se publican en una nueva versión (`/api/v2`).
- `X-API-Version` es opcional y sirve como etiqueta informativa para clientes.

## Naming y convenciones REST

- Sustantivos en plural para colecciones: `/expenses`, `/tags`.
- HTTP verbs estándares:
  - `GET` para lectura.
  - `POST` para creación.
  - `PUT`/`PATCH` para actualización.
  - `DELETE` para eliminación.
- El JSON debe usar `camelCase`.

## Autenticación

- Todas las rutas `/api/v1/**` requieren autenticación.
- MCP se autentica con API key en la cabecera `X-MCP-API-Key`.
- Las rutas `/api/v1/auth/**` son públicas para registro/login.
- Las rutas `/api/v1/api-keys/**` requieren JWT (`Authorization: Bearer ...`).
- Las rutas abiertas se limitan a `/swagger-ui.html` y `/v3/api-docs/**`.

## Idempotencia

- **Obligatoria** en operaciones mutables (`POST`, `PUT`, `DELETE`).
- El cliente debe enviar `Idempotency-Key`.
- El backend persiste la clave junto con un hash del payload para devolver
  respuestas repetibles y detectar reuso indebido.
- Reusar una `Idempotency-Key` con payload diferente debe responder `409`.

## Correlación y trazabilidad

- Los clientes pueden enviar `correlation-id`.
- El backend propaga y responde el mismo valor en la cabecera
  `correlation-id` para trazabilidad end-to-end.

## Envelope de errores

Todas las respuestas de error deben usar el siguiente formato JSON:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "amount must be greater than 0",
  "traceId": "9f2c3e3a3cdb4a2e"
}
```

- `errorCode`: identificador estable y único del error.
- `message`: descripción breve para el cliente.
- `traceId`: identificador de trazas (correlacionado con observabilidad).

## Códigos HTTP

- `200` OK para lecturas exitosas.
- `201` Created para creaciones exitosas.
- `400` Bad Request para validaciones fallidas.
- `401` Unauthorized si falta o es inválida la API key.
- `409` Conflict para reutilización inválida de `Idempotency-Key`.
- `500` Internal Server Error para fallas inesperadas.

## Contratos OpenAPI

- OpenAPI es la fuente de verdad para `/api/v1`.
- Cambios en endpoints deben reflejarse en el archivo OpenAPI y en
  DTOs/controladores.
- Archivo de contrato v1: `docs/openapi/planifi-api-v1.yaml`.
