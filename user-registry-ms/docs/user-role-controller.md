# UserRoleController — Documentación para Frontend

Base URL: `http://localhost:8083/api/user-roles`

---

## Headers requeridos

El API Gateway inyecta `X-User-Name` automáticamente. No es necesario enviarlo desde el frontend.

| Header | Descripción |
|---|---|
| `X-User-Name` | Username del usuario autenticado (lo usa el backend para autorización y registro) |

> ⚠️ Sin `X-User-Name` ambos endpoints devuelven `403 Forbidden`.

---

## Reglas de jerarquía

La asignación/remoción de roles sigue estas reglas basadas en `levelRole` (menor número = mayor jerarquía):

| levelRole | Roles ejemplo |
|---|---|
| 1 | ROLE_ADMIN |
| 2 | ROLE_USER |
| 3 | ROLE_MODERATOR |
| 4 | ROLE_GUEST |

- Solo puedes gestionar roles con `levelRole` **mayor o igual** al tuyo
  - ADMIN (level 1) puede asignar/remover USER (2), MODERATOR (3), GUEST (4)
  - MODERATOR (level 3) solo puede gestionar GUEST (4)
- No puedes auto-asignarte o auto-removerte roles
- `assignedBy` enviado en el body es **ignorado**; se usa el `X-User-Name` del header

---

## Endpoints

### Asignar rol a usuario

```
POST /api/user-roles/assign
```

**Request body:**
```json
{
  "userId": 1,
  "roleId": 2,
  "assignedBy": null
}
```

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `userId` | number | sí | ID del usuario que recibirá el rol |
| `roleId` | number | sí | ID del rol a asignar |
| `assignedBy` | number | no | **Ignorado** — se usa el usuario autenticado |

**Response `200 OK`** (sin contenido)

**Errores:**
| Código | Mensaje | Causa |
|---|---|---|
| `403` | `Authenticated user not found` | Header `X-User-Name` ausente o usuario no existe |
| `403` | `You cannot assign roles to yourself` | `userId` == ID del usuario autenticado |
| `403` | `You don't have permission to manage role: X` | Tu `levelRole` es mayor (menor jerarquía) que el del rol destino |
| `404` | `Role not found with id: X` | El `roleId` no existe |
| `404` | `User not found with id: X` | El `userId` no existe |
| `409` | `User already has this role` | El usuario ya tiene ese rol asignado |

---

### Remover rol de usuario

```
DELETE /api/user-roles/remove?userId=1&roleId=2
```

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `userId` | number | sí | ID del usuario |
| `roleId` | number | sí | ID del rol a remover |

**Response `204 No Content`** (sin contenido)

**Errores:**
| Código | Mensaje | Causa |
|---|---|---|
| `403` | `Authenticated user not found` | Header `X-User-Name` ausente o usuario no existe |
| `403` | `You cannot remove roles from yourself` | `userId` == ID del usuario autenticado |
| `403` | `You don't have permission to manage role: X` | Tu `levelRole` es mayor (menor jerarquía) que el del rol |
| `404` | `Role not found with id: X` | El `roleId` no existe |
| `404` | `User does not have this role` | El usuario no tiene ese rol asignado |

---

## TypeScript Interfaces

```typescript
export interface AssignRoleRequest {
  userId: number;
  roleId: number;
  assignedBy?: number | null; // Ignorado, se usa X-User-Name
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  path: string;
}
```

---

## Flujo típico de uso

1. Obtener lista de roles disponibles → `GET /api/roles`
2. Obtener lista de usuarios → `GET /api/users`
3. Asignar rol → `POST /api/user-roles/assign` con `userId` y `roleId`
4. Refrescar datos del usuario para ver el nuevo rol → `GET /api/users/{userId}`

---

## Notas

- El campo `assignedBy` del DTO se ignora completamente; el backend siempre usa el `X-User-Name` del gateway para registrar quién hizo la operación.
- Para conocer qué roles puede asignar el usuario logueado, compará el `levelRole` de sus roles contra el `levelRole` del rol destino.
