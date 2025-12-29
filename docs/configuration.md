# Configuración por archivos (bootstrap)

Planifi permite inicializar datos y parámetros base usando un archivo JSON o
YAML. El backend lee la configuración al arrancar y crea/actualiza las
entidades correspondientes.

## Activación

Define la ruta del archivo mediante `planifi.bootstrap.config-path` o su
equivalente en variables de entorno:

```bash
export PLANIFI_BOOTSTRAP_CONFIG_PATH=/ruta/planifi-bootstrap.yml
```

Para deshabilitar la carga:

```bash
export PLANIFI_BOOTSTRAP_ENABLED=false
```

## Especificación (v1)

```yaml
version: v1
storage:
  provider: local
  local:
    basePath: /var/lib/planifi
security:
  rateLimit:
    enabled: true
    requestsPerMinute: 120
    burst: 20
users:
  - email: admin@example.com
    fullName: Admin
    password: "change-me"
    # Alternativa segura: passwordHash con bcrypt ya calculado
    # passwordHash: "$2a$10$..."
    accounts:
      - name: Principal
        currency: MXN
      - name: Ahorros
        currency: USD
```

## Reglas de carga

- `version` debe ser `v1`.
- `storage.provider` solo admite `local` y requiere `storage.local.basePath`.
- `security.rateLimit` es obligatorio (aunque el rate limiting se implemente
  en el gateway/backend más adelante).
- `users` es opcional, pero si existe:
  - `email` es obligatorio y único.
  - Debe existir `password` o `passwordHash`.
  - Cada cuenta se **upserta** por `(userId, name)` ignorando mayúsculas.
- Las configuraciones de `storage` y `security` se persisten como settings
  del sistema para uso posterior.

## Errores de validación

Si falta un campo requerido o el archivo tiene errores, el backend aborta el
arranque con un mensaje claro en logs indicando el campo inválido.
