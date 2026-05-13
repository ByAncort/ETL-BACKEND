# Documentación de Servicios Disponibles

## Tabla de Contenidos

- [API Register MS](#api-register-ms)
- [Identity Service](#identity-service)
- [User Registry MS](#user-registry-ms)
- [Integration MS](#integration-ms)
- [Schema Matching MS](#schema-matching-ms)

---

## API Register MS

**Base URL:** `/api-registry`

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api-registry` | Registrar una nueva API |
| GET | `/api-registry/{id}` | Obtener API por ID |
| GET | `/api-registry/list` | Listar todas las APIs |
| POST | `/api-registry/{id}/test` | Probar una API |
| GET | `/api-registry/{id}/auth-api` | Obtener API de autenticación |
| PUT | `/api-registry/{id}` | Actualizar una API |
| PUT | `/api-registry/{id}/auth-api` | Actualizar API de autenticación |

### DTOs

#### Request DTOs

**ApiRegisterRequest**
```json
{
  "method": "GET",
  "url": "https://api.example.com/users",
  "description": "Obtener lista de usuarios",
  "pathParams": "",
  "queryParams": "page=1&limit=10",
  "authType": "BEARER",
  "authHeader": "Authorization",
  "authValue": "Bearer token123",
  "username": "user",
  "password": "pass",
  "tokenEndpoint": "https://api.example.com/oauth/token",
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "{\"key\": \"value\"}",
  "apiAuth": null
}
```

**ApiUpdateRequest**
```json
{
  "method": "GET",
  "url": "https://api.example.com/users",
  "description": "Actualizar descripción",
  "pathParams": "",
  "queryParams": "page=1",
  "body": "{\"key\": \"value\"}",
  "authType": "BEARER",
  "authHeader": "Authorization",
  "authValue": "Bearer token456",
  "username": "user",
  "password": "pass",
  "tokenEndpoint": "https://api.example.com/oauth/token"
}
```

**TestRequest**
```json
{
  "pathParams": "/123",
  "queryParams": "name=test",
  "body": "{\"test\": \"data\"}"
}
```

#### Response DTOs

- `ApiResponse` - Respuesta de API
- `TestResponse` - Respuesta de prueba

---

## Identity Service

**Base URL:** `/api/v1/auth`

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Registrar nuevo usuario |
| POST | `/api/v1/auth/token` | Generar token de acceso (login) |
| GET | `/api/v1/auth/validate` | Validar token |
| POST | `/api/v1/auth/refresh` | Refrescar token |
| POST | `/api/v1/auth/logout` | Cerrar sesión |

### DTOs

#### Request DTOs

**RegisterRequest**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123"
}
```

**AuthRequest**
```json
{
  "username": "johndoe",
  "password": "password123"
}
```

#### Response DTOs

- `AuthResponse` - Respuesta de autenticación (message, accessToken, refreshToken)

---

## User Registry MS

### UserController

**Base URL:** `/api/users`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/users` | Crear usuario |
| GET | `/api/users/{id}` | Obtener usuario por ID |
| GET | `/api/users/username/{username}` | Obtener usuario por username |
| GET | `/api/users` | Listar todos los usuarios |
| PUT | `/api/users/{id}` | Actualizar usuario |
| DELETE | `/api/users/{id}` | Eliminar usuario |
| POST | `/api/users/{id}/verify-email` | Verificar email |

### RoleController

**Base URL:** `/api/roles`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/roles` | Crear rol |
| GET | `/api/roles/{id}` | Obtener rol por ID |
| GET | `/api/roles` | Listar todos los roles |
| PUT | `/api/roles/{id}` | Actualizar rol |
| DELETE | `/api/roles/{id}` | Eliminar rol |

### UserRoleController

**Base URL:** `/api/user-roles`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/user-roles/assign` | Asignar rol a usuario |
| DELETE | `/api/user-roles/remove` | Remover rol de usuario |

### DTOs

#### Request DTOs

**UserRequest**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**RoleRequest**
```json
{
  "name": "ADMIN",
  "description": "Administrator role",
  "levelRole": 1,
  "isSystem": true
}
```

**AssignRoleRequest**
```json
{
  "userId": 1,
  "roleId": 2,
  "assignedBy": 1
}
```

#### Response DTOs

- `UserResponse` - Respuesta de usuario
- `RoleResponse` - Respuesta de rol

---

## Integration MS

**Base URL:** `/api/integrations`

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/integrations/connections` | Crear integración |
| GET | `/api/integrations/connections` | Listar todas las integraciones |
| GET | `/api/integrations/connections/{id}` | Obtener integración por ID |
| PUT | `/api/integrations/connections/{id}` | Actualizar integración |
| DELETE | `/api/integrations/connections/{id}` | Eliminar integración |

### DTOs

#### Request DTOs

**IntegrationRequest**
```json
{
  "apiA": "https://api.alpha.com/users",
  "apiB": "https://api.beta.com/clients",
  "description": "Mapping between Alpha and Beta APIs"
}
```

#### Response DTOs

- `IntegrationResponse` - Respuesta de integración

---

## Schema Matching MS

### SchemaMatchController

**Base URL:** `/api/schema-matches`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/schema-matches` | Listar todos los matches |
| GET | `/api/schema-matches/integration/{integrationId}` | Listar matches por integración |
| GET | `/api/schema-matches/integration/{integrationId}/status/{status}` | Listar matches por estado |
| GET | `/api/schema-matches/{id}` | Obtener match por ID |
| POST | `/api/schema-matches` | Crear match |
| PUT | `/api/schema-matches/{id}` | Actualizar match |
| PATCH | `/api/schema-matches/{id}/status` | Actualizar estado del match |
| DELETE | `/api/schema-matches/{id}` | Eliminar match |
| POST | `/api/schema-matches/feedback` | Agregar feedback |
| GET | `/api/schema-matches/{id}/feedback` | Obtener feedback de un match |

### IntegrationController

**Base URL:** `/api/integrations`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/integrations/connections/{connectionId}` | Obtener conexión por ID |

### DTOs

#### Request DTOs

**SchemaMatchRequestDTO**
```json
{
  "integrationId": 1,
  "sourceField": "user_id",
  "targetField": "client_id",
  "confidence": 0.95,
  "status": "PENDING",
  "transformation": "Integer.parseInt(value)",
  "reviewedBy": 1
}
```

**MatchFeedbackRequestDTO**
```json
{
  "matchId": 1,
  "userApproved": true,
  "actualTarget": "client_id"
}
```

#### Response DTOs

- `SchemaMatchResponseDTO` - Respuesta de match
- `MatchFeedbackResponseDTO` - Respuesta de feedback
- `ConnectionResponseDTO` - Respuesta de conexión

---

## Estados de Match (MatchStatus)

| Estado | Descripción |
|-------|-------------|
| PENDING | Pendiente de revisión |
| APPROVED | Aprobado |
| REJECTED | Rechazado |
| AUTO_APPROVED | Aprobado automáticamente |

---

## ETL Config LLM MS

**Base URL:** `/api/llm-configs`

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/llm-configs` | Registrar un nuevo LLM |
| GET | `/api/llm-configs` | Listar todos los LLMs configurados |
| GET | `/api/llm-configs/{id}` | Obtener LLM por ID |
| GET | `/api/llm-configs/default` | Obtener el LLM configurado por defecto |
| PUT | `/api/llm-configs/{id}` | Actualizar configuración de LLM |
| DELETE | `/api/llm-configs/{id}` | Eliminar configuración de LLM |
| PATCH | `/api/llm-configs/{id}/default` | Establecer LLM como default |

### DTOs

#### Request DTOs

**LlmConfigRequest**
```json
{
  "name": "GPT-4 Production",
  "provider": "openai",
  "apiKey": "sk-proj-xxxxx",
  "baseUrl": "https://api.openai.com/v1",
  "modelName": "gpt-4",
  "isDefault": true
}
```

#### Response DTOs

- `LlmConfigResponse` - Respuesta con los datos del LLM configurado

---

## Notas

- Todos los endpoints soportan CORS cuando está habilitado (ver anotaciones `@CrossOrigin`)
- Algunos endpoints requieren autenticación via JWT token
- Los IDs en los path son de tipo `Long`
- Los valores de `authType` pueden ser: `NONE`, `BEARER`, `BASIC`, `API_KEY`, `OAUTH2`