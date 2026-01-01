# Planifi MCP actions v1

## Alcance

Este documento define el set mínimo de acciones MCP para v0.1 y sus esquemas
versionados. Las acciones describen la capa entre el LLM y la API `/api/v1`.

- **Versión de contrato MCP:** `v1`
- **Nombre de acción:** `planifi.<action>.v1`
- **Fuente de verdad de datos:** `docs/openapi/planifi-api-v1.yaml`
- **Fuera de alcance:** no se exponen acciones de configuración de seguridad
  (API keys, roles o credenciales) vía MCP.

## Seguridad y headers obligatorios

- **Autenticación:** `X-MCP-API-Key` (API key del MCP Server).
- **Correlación:** `correlation-id` (opcional, se propaga end-to-end).
- **Idempotencia:** `Idempotency-Key` en operaciones mutables.

> Nota: la API key y headers se envían en el transporte HTTP del MCP Server y
> **no forman parte del payload del action**.

## Convenciones de schema

- JSON object con `additionalProperties: false`.
- Strings no vacíos para `Idempotency-Key` (min 8, max 255).
- Salidas exitosas y errores están versionados y documentados por acción.

### Error schema (común)

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "amount must be greater than 0",
  "traceId": "9f2c3e3a3cdb4a2e"
}
```

## Acciones MCP v1

### 1) `planifi.createExpense.v1`

- **Backend:** `POST /api/v1/expenses`
- **Idempotencia:** requerida (`Idempotency-Key`)

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["idempotencyKey", "accountId", "amount", "occurredOn", "description"],
  "properties": {
    "idempotencyKey": {
      "type": "string",
      "minLength": 8,
      "maxLength": 255
    },
    "correlationId": {
      "type": "string"
    },
    "accountId": {
      "type": "string",
      "format": "uuid"
    },
    "amount": {
      "type": "number",
      "format": "decimal"
    },
    "occurredOn": {
      "type": "string",
      "format": "date"
    },
    "description": {
      "type": "string",
      "minLength": 1,
      "maxLength": 255
    },
    "tags": {
      "type": "array",
      "maxItems": 25,
      "items": {
        "type": "string",
        "maxLength": 80
      }
    },
    "createMissingTags": {
      "type": "boolean",
      "default": false
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["expense"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "expense": {
      "$ref": "#/definitions/Expense"
    }
  },
  "definitions": {
    "Expense": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "accountId", "amount", "occurredOn", "description", "createdAt", "tags"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "accountId": {"type": ["string", "null"], "format": "uuid"},
        "amount": {"type": "number", "format": "decimal"},
        "occurredOn": {"type": "string", "format": "date"},
        "description": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"},
        "tags": {
          "type": "array",
          "items": {"$ref": "#/definitions/Tag"}
        }
      }
    },
    "Tag": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 2) `planifi.listAccounts.v1`

- **Backend:** `GET /api/v1/accounts`
- **Idempotencia:** no aplica

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "correlationId": {
      "type": "string"
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["accounts"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "accounts": {
      "type": "array",
      "items": {"$ref": "#/definitions/Account"}
    }
  },
  "definitions": {
    "Account": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "type", "currency", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "type": {
          "type": "string",
          "enum": ["CASH", "BANK", "DEBIT_CARD", "CREDIT_CARD"]
        },
        "currency": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 3) `planifi.listExpenses.v1`

- **Backend:** `GET /api/v1/expenses`
- **Idempotencia:** no aplica

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "correlationId": {
      "type": "string"
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["expenses"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "expenses": {
      "type": "array",
      "items": {"$ref": "#/definitions/Expense"}
    }
  },
  "definitions": {
    "Expense": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "accountId", "amount", "occurredOn", "description", "createdAt", "tags"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "accountId": {"type": ["string", "null"], "format": "uuid"},
        "amount": {"type": "number", "format": "decimal"},
        "occurredOn": {"type": "string", "format": "date"},
        "description": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"},
        "tags": {
          "type": "array",
          "items": {"$ref": "#/definitions/Tag"}
        }
      }
    },
    "Tag": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 4) `planifi.listTags.v1`

- **Backend:** `GET /api/v1/tags`
- **Idempotencia:** no aplica

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "correlationId": {
      "type": "string"
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["tags"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "tags": {
      "type": "array",
      "items": {"$ref": "#/definitions/Tag"}
    }
  },
  "definitions": {
    "Tag": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 5) `planifi.createTag.v1`

- **Backend:** `POST /api/v1/tags`
- **Idempotencia:** requerida (`Idempotency-Key`)

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["idempotencyKey", "name"],
  "properties": {
    "idempotencyKey": {
      "type": "string",
      "minLength": 8,
      "maxLength": 255
    },
    "correlationId": {
      "type": "string"
    },
    "name": {
      "type": "string",
      "maxLength": 80
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["tag"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "tag": {
      "$ref": "#/definitions/Tag"
    }
  },
  "definitions": {
    "Tag": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 6) `planifi.createAccount.v1`

- **Backend:** `POST /api/v1/accounts`
- **Idempotencia:** requerida (`Idempotency-Key`)

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["idempotencyKey", "name", "type"],
  "properties": {
    "idempotencyKey": {
      "type": "string",
      "minLength": 8,
      "maxLength": 255
    },
    "correlationId": {
      "type": "string"
    },
    "name": {
      "type": "string",
      "maxLength": 150
    },
    "type": {
      "type": "string",
      "enum": ["CASH", "BANK", "DEBIT_CARD", "CREDIT_CARD"]
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["account"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "account": {
      "$ref": "#/definitions/Account"
    }
  },
  "definitions": {
    "Account": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "type", "currency", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "type": {
          "type": "string",
          "enum": ["CASH", "BANK", "DEBIT_CARD", "CREDIT_CARD"]
        },
        "currency": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 7) `planifi.disableAccount.v1`

- **Backend:** `POST /api/v1/accounts/{accountId}/disable`
- **Idempotencia:** requerida (`Idempotency-Key`)

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["idempotencyKey", "accountId"],
  "properties": {
    "idempotencyKey": {
      "type": "string",
      "minLength": 8,
      "maxLength": 255
    },
    "correlationId": {
      "type": "string"
    },
    "accountId": {
      "type": "string",
      "format": "uuid"
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["accountId", "status"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "accountId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": ["disabled"]
    }
  }
}
```

### 8) `planifi.listTransactions.v1`

- **Backend:** `GET /api/v1/transactions`
- **Idempotencia:** no aplica

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["accountId", "from", "to"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "accountId": {
      "type": "string",
      "format": "uuid"
    },
    "from": {
      "type": "string",
      "format": "date"
    },
    "to": {
      "type": "string",
      "format": "date"
    },
    "page": {
      "type": "integer",
      "minimum": 0,
      "default": 0
    },
    "size": {
      "type": "integer",
      "minimum": 1,
      "maximum": 200,
      "default": 50
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["page"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "page": {
      "$ref": "#/definitions/TransactionPage"
    }
  },
  "definitions": {
    "TransactionPage": {
      "type": "object",
      "additionalProperties": false,
      "required": ["items", "page", "size", "totalItems", "totalPages"],
      "properties": {
        "items": {
          "type": "array",
          "items": {"$ref": "#/definitions/Transaction"}
        },
        "page": {"type": "integer", "minimum": 0},
        "size": {"type": "integer", "minimum": 1},
        "totalItems": {"type": "integer", "minimum": 0},
        "totalPages": {"type": "integer", "minimum": 0}
      }
    },
    "Transaction": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "accountId", "amount", "occurredOn", "description", "createdAt", "tags"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "accountId": {"type": "string", "format": "uuid"},
        "amount": {"type": "number", "format": "decimal"},
        "occurredOn": {"type": "string", "format": "date"},
        "description": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"},
        "tags": {
          "type": "array",
          "items": {"$ref": "#/definitions/Tag"}
        }
      }
    },
    "Tag": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```

### 9) `planifi.createTransaction.v1`

- **Backend:** `POST /api/v1/transactions`
- **Idempotencia:** requerida (`Idempotency-Key`)

**Input schema**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["idempotencyKey", "accountId", "amount", "occurredOn", "description"],
  "properties": {
    "idempotencyKey": {
      "type": "string",
      "minLength": 8,
      "maxLength": 255
    },
    "correlationId": {
      "type": "string"
    },
    "accountId": {
      "type": "string",
      "format": "uuid"
    },
    "amount": {
      "type": "number",
      "format": "decimal"
    },
    "occurredOn": {
      "type": "string",
      "format": "date"
    },
    "description": {
      "type": "string",
      "minLength": 1,
      "maxLength": 255
    },
    "tags": {
      "type": "array",
      "maxItems": 25,
      "items": {
        "type": "string",
        "maxLength": 80
      }
    },
    "createMissingTags": {
      "type": "boolean",
      "default": false
    }
  }
}
```

**Output schema (success)**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["transaction"],
  "properties": {
    "correlationId": {
      "type": "string"
    },
    "transaction": {
      "$ref": "#/definitions/Transaction"
    }
  },
  "definitions": {
    "Transaction": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "accountId", "amount", "occurredOn", "description", "createdAt", "tags"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "accountId": {"type": "string", "format": "uuid"},
        "amount": {"type": "number", "format": "decimal"},
        "occurredOn": {"type": "string", "format": "date"},
        "description": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"},
        "tags": {
          "type": "array",
          "items": {"$ref": "#/definitions/Tag"}
        }
      }
    },
    "Tag": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "name", "createdAt"],
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"},
        "createdAt": {"type": "string", "format": "date-time"}
      }
    }
  }
}
```
