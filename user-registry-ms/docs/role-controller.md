# RoleController — Documentación para Frontend

Base URL: `http://localhost:8083/api`

---

## Headers de autenticación

El API Gateway inyecta automáticamente estos headers para requests autenticados:

| Header | Descripción |
|---|---|
| `X-User-Name` | Username del usuario autenticado |
| `X-User-Roles` | Roles del usuario autenticado (separados por coma) |

> ⚠️ Todos los endpoints de escritura (`POST`, `PUT`, `DELETE`) requieren `X-User-Name`.
> No es necesario enviarlos manualmente desde el frontend — el gateway los agrega.

---

## Jerarquía de roles

Cada rol tiene un `levelRole` numérico. **Menor número = mayor jerarquía.**

| ID | Rol | levelRole | Descripción |
|---|---|---|---|
| `1` | `ROLE_ADMIN` | `1` | Administrador del sistema |
| `2` | `ROLE_USER` | `2` | Usuario estándar |
| `3` | `ROLE_MODERATOR` | `3` | Moderador |
| `4` | `ROLE_GUEST` | `4` | Invitado |

### Reglas de jerarquía

1. **Un usuario solo puede gestionar roles de nivel inferior al suyo.**
   - `ROLE_ADMIN` (level 1) puede asignar/remover `ROLE_USER` (2), `ROLE_MODERATOR` (3), `ROLE_GUEST` (4)
   - `ROLE_MODERATOR` (level 3) solo puede gestionar `ROLE_GUEST` (4)
   - `ROLE_USER` (level 2) solo puede gestionar `ROLE_MODERATOR` (3) y `ROLE_GUEST` (4)
   - `ROLE_GUEST` (level 4) **no puede gestionar ningún rol**

2. **Nadie puede gestionar roles superiores o iguales a su propio nivel.**
   - Un `ROLE_MODERATOR` NO puede asignar `ROLE_ADMIN`
   - Un `ROLE_ADMIN` NO puede modificar/eliminar el rol `ROLE_ADMIN`

3. **Un usuario no puede asignarse/removerse roles a sí mismo.**

4. **Los roles de sistema (`isSystem = true`) no pueden ser modificados ni eliminados.**

5. **No se puede eliminar un rol que esté asignado a uno o más usuarios.**

---

## 1. RoleController (`/api/roles`)

### 1.1 Crear rol

```
POST /api/roles
```

Headers requeridos: `X-User-Name`

**Request body:**
```json
{
  "name": "ROLE_MODERATOR",
  "description": "Moderador del sistema",
  "levelRole": 3,
  "isSystem": false
}
```

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `name` | string | sí | Nombre del rol (único) |
| `description` | string | sí | Descripción del rol |
| `levelRole` | number | no | Nivel jerárquico (menor = mayor jerarquía) |
| `isSystem` | boolean | no | Si es rol del sistema (default: `false`) |

**Response `201 Created`:**
```json
{
  "id": 5,
  "name": "ROLE_MODERATOR",
  "description": "Moderador del sistema",
  "levelRole": 3,
  "isSystem": false,
  "createdAt": "2025-05-18T14:30:00"
}
```

**Errores:**
- `403 Forbidden` — no tienes permiso para crear roles
- `409 Conflict` — ya existe un rol con ese nombre
- `400 Bad Request` — validación fallida

---

### 1.2 Obtener rol por ID

```
GET /api/roles/{id}
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "ROLE_ADMIN",
  "description": "Administrador del sistema",
  "levelRole": 1,
  "isSystem": true,
  "createdAt": "2025-01-01T00:00:00"
}
```

**Errores:**
- `404 Not Found` — no existe el rol

---

### 1.3 Listar todos los roles

```
GET /api/roles
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "name": "ROLE_ADMIN",
    "description": "Administrador del sistema",
    "levelRole": 1,
    "isSystem": true,
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "id": 2,
    "name": "ROLE_USER",
    "description": "Usuario estándar",
    "levelRole": 2,
    "isSystem": true,
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

---

### 1.4 Obtener roles por username

```
GET /api/roles/user/{username}
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "name": "ROLE_ADMIN",
    "description": "Administrador del sistema",
    "levelRole": 1,
    "isSystem": true,
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

**Errores:**
- `404 Not Found` — no existe el usuario

---

### 1.5 Actualizar rol

```
PUT /api/roles/{id}
```

Headers requeridos: `X-User-Name`

**Request body:**
```json
{
  "name": "ROLE_MODERATOR",
  "description": "Moderador del sistema",
  "levelRole": 3,
  "isSystem": false
}
```

**Response `200 OK`:**
```json
{
  "id": 3,
  "name": "ROLE_MODERATOR",
  "description": "Moderador del sistema",
  "levelRole": 3,
  "isSystem": false,
  "createdAt": "2025-03-01T10:00:00"
}
```

**Errores:**
- `403 Forbidden` — no tienes permiso, o es un rol de sistema
- `404 Not Found` — no existe el rol
- `400 Bad Request` — nombre duplicado o validación fallida

---

### 1.6 Eliminar rol

```
DELETE /api/roles/{id}
```

Headers requeridos: `X-User-Name`

**Response `204 No Content`** (sin body)

**Errores:**
- `403 Forbidden` — no tienes permiso, o es un rol de sistema
- `404 Not Found` — no existe el rol
- `409 Conflict` — el rol está asignado a uno o más usuarios

---

## 2. UserRoleController (`/api/user-roles`)

### 2.1 Asignar rol a usuario

```
POST /api/user-roles/assign
```

Headers requeridos: `X-User-Name`

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
| `userId` | number | sí | ID del usuario objetivo |
| `roleId` | number | sí | ID del rol a asignar |
| `assignedBy` | number | no | **Ignorado** — se usa el usuario autenticado del header |

> ⚠️ El campo `assignedBy` en el body es ignorado. El sistema usa el `X-User-Name` del header para registrar quién asignó el rol.

**Response `200 OK`** (sin body)

**Errores:**
- `403 Forbidden` — no tienes permiso para asignar este rol (jerarquía), o intentas asignarte a ti mismo
- `404 Not Found` — usuario o rol no existen
- `409 Conflict` — el usuario ya tiene ese rol asignado

---

### 2.2 Remover rol de usuario

```
DELETE /api/user-roles/remove?userId=1&roleId=2
```

Headers requeridos: `X-User-Name`

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `userId` | number | sí | ID del usuario |
| `roleId` | number | sí | ID del rol |

**Response `204 No Content`** (sin body)

**Errores:**
- `403 Forbidden` — no tienes permiso, o intentas removértelo a ti mismo
- `404 Not Found` — el usuario no tiene ese rol asignado

---

## 3. TypeScript Interfaces

```typescript
// ---------- Role ----------

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
  levelRole: number | null;
  isSystem: boolean;
  createdAt: string; // ISO 8601
}

export interface RoleRequest {
  name: string;
  description: string;
  levelRole?: number | null;
  isSystem?: boolean; // default: false
}

// ---------- UserRole ----------

export interface AssignRoleRequest {
  userId: number;
  roleId: number;
  assignedBy?: number | null; // Ignored by backend, X-User-Name is used instead
}
```

---

## 4. Matriz de permisos

| Acción | ¿Quién puede hacerla? |
|---|---|
| Crear rol | Usuarios con `levelRole` menor al del nuevo rol |
| Ver roles | Todos (autenticados) |
| Actualizar rol | Usuarios con `levelRole` menor al del rol objetivo |
| Eliminar rol | Usuarios con `levelRole` menor al del rol objetivo |
| Asignar rol a usuario | Usuarios con `levelRole` menor o igual al del rol asignado |
| Remover rol de usuario | Usuarios con `levelRole` menor o igual al del rol removido |

> **Menor `levelRole` = mayor jerarquía.** Un ADMIN (level=1) puede gestionar MODERATOR (level=3), pero no al revés.

---

## 5. Notas importantes

- Los roles con `isSystem = true` **no pueden ser modificados ni eliminados** desde la API.
- El rol `ROLE_GUEST` (id: 4) se asigna automáticamente a todo usuario nuevo.
- No se puede eliminar un usuario que tenga `ROLE_MODERATOR` (id: 3).
- Los roles viajan en el JWT como claims y se usan para autorización en el gateway.
- `assignedBy` en la request de asignación es **ignorado** — siempre se usa el usuario autenticado vía `X-User-Name`.
- Si no se provee `X-User-Name`, el endpoint devuelve `403 Forbidden`.
