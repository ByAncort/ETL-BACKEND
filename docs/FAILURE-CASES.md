# Failure Cases
> Catálogo de fallos resueltos en QA — causa raíz, síntoma y resolución

| Propiedad | Valor |
|-----------|-------|
| Proyecto | ETL-BACKEND |
| Rama | qa |
| Última Actualización | 2026-06-20 |

---

## FC-01: Aserción incorrecta en test de rate limit

| Campo | Detalle |
|-------|---------|
| Síntoma | `rateLimit_shouldAddHeaders_onSuccess` falla porque `setStatusCode` nunca se invoca |
| Archivo | `swiggy-gateway/.../filter/AuthenticationFilterMoreTest.java:220` |
| Causa | El test usaba `then(response).should().setStatusCode(any())` en el caso exitoso. El filtro de rate limit NO modifica el status cuando la request es válida — solo pasa al `chain.filter()`. |
| Resolución | Cambiar aserción: `should(never()).setStatusCode(...)` + verificar que `chain.filter(exchange)` sí se ejecuta |
| Commit | `53687da` |

---

## FC-02: `@MockBean` faltante en AuthControllerTest

| Campo | Detalle |
|-------|---------|
| Síntoma | `AuthControllerTest` no carga contexto — `NoSuchBeanDefinitionException` para `SessionLogService` |
| Archivo | `identity-service/.../controller/AuthControllerTest.java` |
| Causa | `AuthController` inyecta `SessionLogService` via `@Autowired`. El test no proveía mock, Spring no encontraba el bean al cargar `@WebMvcTest`. |
| Resolución | Agregar `@MockBean private SessionLogService sessionLogService` |
| Commit | `42415f6` |

---

## FC-03: Mensaje de excepción desincronizado

| Campo | Detalle |
|-------|---------|
| Síntoma | Test `runMatching_shouldThrowException_whenMatcherFails` falla: `hasMessageContaining("Matcher returned error")` no coincide con el mensaje real |
| Archivo | `integration-ms/.../service/IntegrationServiceMoreTest.java:234` |
| Causa | El mensaje real lanzado por el servicio era `"Failed to run matcher"`. El test se escribió con un mensaje distinto al real. |
| Resolución | Cambiar `.hasMessageContaining("Matcher returned error")` → `.hasMessageContaining("Failed to run matcher")` |
| Commit | `33b7751` |

---

## FC-04: `columnDefinition` MySQL incompatible con H2 en tests

| Campo | Detalle |
|-------|---------|
| Síntoma | Tests de `api-register-ms` fallan con `JdbcSQLSyntaxErrorException` — H2 no reconoce `TEXT` ni `LONGTEXT` |
| Archivos | `ApiEndpoint.java`, `AuthCredential.java`, `ExecutionLog.java` |
| Causa | Usaban `@Column(columnDefinition = "TEXT")` y `@Column(columnDefinition = "LONGTEXT")`, sintaxis específica de MySQL. Al correr tests con H2 (modo MySQL simulado), H2 no interpreta esos tipos. |
| Resolución | Reemplazar por `@Lob` — anotación JPA portable entre H2 y PostgreSQL |
| Commit | `a7d6b8d` |

---

## FC-05: Cambio de contraseña no sincronizado entre microservicios

| Campo | Detalle |
|-------|---------|
| Síntoma | Usuario cambia contraseña en `user-registry-ms` pero no puede iniciar sesión — `identity-service` mantiene el hash anterior |
| Archivos | `identity-service/.../controller/AuthController.java`, `AuthService.java`, `dto/UpdatePasswordRequest.java` + `user-registry-ms/.../client/IdentityServiceClient.java`, `service/UserService.java` |
| Causa | `identity-service` no tenía endpoint para actualizar contraseña. `user-registry-ms` hasheaba y guardaba localmente pero nunca notificaba a `identity-service`. |
| Resolución | 1. Nuevo endpoint `PUT /update-password` en `AuthController`<br>2. DTO `UpdatePasswordRequest`<br>3. `AuthService.updatePassword()` hashea y persiste<br>4. `IdentityServiceClient.updatePassword()` llama al endpoint via HTTP<br>5. `UserService.updateUser()` invoca al client después de hashear |
| Commit | `e31ad27` |

---

## FC-06: Query `findByUsername` inconsistente

| Campo | Detalle |
|-------|---------|
| Síntoma | Error de tipo: `findByUsername` retorna `Optional<User>` pero el código esperaba `User` directo; no hay búsqueda case-insensitive |
| Archivo | `user-registry-ms/.../repository/UserRepository.java` |
| Causa | Spring Data JPA genera `findByUsername` como `Optional<User>`. Código cliente esperaba `User` directo. Además login fallaba si el username tenía distinto casing. |
| Resolución | Cambiar a `findUserByUsername` (retorna `User` directo) + agregar `findByUsernameIgnoreCase` para login case-insensitive |
| Commit | `f70098d` |

---

## FC-07: Null safety + rutas abiertas faltantes en RouteValidator

| Campo | Detalle |
|-------|---------|
| Síntoma | `NullPointerException` al acceder a `request.getURI().getPath()` si la URI es null. `/api/users/register` y `/api/users/login` devuelven 401. |
| Archivos | `swiggy-gateway/.../filter/RouteValidator.java`, `exception/GlobalErrorWebExceptionHandler.java` |
| Causa | Gateway no validaba null en URI/path. Las rutas del nuevo `user-registry-ms` no estaban en `OPEN_API_ENDPOINTS`. |
| Resolución | 1. Null checks en `isSecured` y `isPublicEndpoint`<br>2. Agregar rutas faltantes a `OPEN_API_ENDPOINTS`<br>3. Logger en `GlobalErrorWebExceptionHandler` para tracking |
| Commit | `aae0341` |

---

## FC-08: Puertos incorrectos en Dockerfiles

| Campo | Detalle |
|-------|---------|
| Síntoma | Todos los Dockerfiles exponen `EXPOSE 8080`. Contenedores no rutean correctamente. `swiggy-service-registry` referencia `temurin:25-jre` (inexistente). |
| Archivos | `api-register-ms/Dockerfile`, `identity-service/Dockerfile`, `swiggy-service-registry/Dockerfile` |
| Causa | Se copió el mismo Dockerfile sin ajustar puertos al real de cada servicio. |
| Resolución | Corregir `EXPOSE`: `8083` (api-register), `9898` (identity), `8761` (registry). Cambiar `temurin:25-jre` → `temurin:21-jre`. |
| Commit | `6ccd4f4` |

---

## FC-09: jjwt 0.13.0 incompatible con API legacy

| Campo | Detalle |
|-------|---------|
| Síntoma | Error de compilación: `setSigningKey()`, `parseClaimsJws()`, `getBody()` no existen en jjwt 0.13.0 |
| Archivos | `swiggy-gateway/.../util/JwtUtil.java`, `identity-service/.../service/JwtService.java` + `pom.xml` de ambos |
| Causa | jjwt 0.13.0 removió la API legacy. El código usaba `verifyWith()`, `parseSignedClaims()`, `getPayload()` (API nueva) pero `SecretKey` no coincidía. |
| Resolución | Unificar a jjwt `0.12.6` en ambos `pom.xml`. En `JwtUtil` usar `setSigningKey(Key)`, `parseClaimsJws()`, `getBody()`. En `JwtService` safe call con `"refresh".equals(...)` primero. |
| Commits | `ed7e87f`, `78bd40b` |

---

## FC-10: `getUserByUsername` lanza NPE/500 cuando el usuario no existe

| Campo | Detalle |
|-------|---------|
| Síntoma | El dashboard del front spammeaba `Error fetching user data: 500` en loop y el login de `identity-service` devolvía roles vacíos. `GET /api/users/username/{u}` respondía `500 Internal Server Error` para usuarios inexistentes en `user-registry-ms`. |
| Archivo | `user-registry-ms/.../service/UserService.java:94` |
| Causa | `findUserByUsername` retorna `null` (no `Optional`) cuando no hay match. `getUserByUsername` lo pasaba directo a `mapToResponse`, que invoca `user.getId()` → `NullPointerException` → 500. A diferencia de `getUserById`, no validaba el caso nulo. |
| Resolución | Validar null y lanzar `ResponseStatusException(HttpStatus.NOT_FOUND)` (mismo patrón que el resto del controller). El front maneja el 404 con su `try/catch` sin romper el dashboard. |
| Regresión | `UserServiceTest#getUserByUsername_whenUserNotFound_shouldThrow404` (rojo→verde: antes NPE, ahora 404) |
| Detectado en | QA Experiencia 3 — flujo login + dashboard en navegador, config `etl_db` remota |
| Commit | `c89ab09` (fix) |

---

## FC-11: Aserción incorrecta en merge con record None

| Campo | Detalle |
|-------|---------|
| Síntoma | `test_merge_ignora_none_en_record` falla: espera `merged["age"] is None` pero el valor real es `0` |
| Archivo | `MS-SAVE-DATA/tests/test_load_service.py:238` |
| Causa | `_merge_record_with_template` (L30-31) solo sobreescribe si `record[key] is not None`. Con `record["age"] = None`, el template `"age": 0` se conserva. El test asumía que None reemplazaba al template. |
| Resolución | Cambiar `assert merged["age"] is None` → `assert merged["age"] == 0` |
| Commit | `70e3dc2` |

---

## FC-12: `raise_for_status` no-op en fake client de tests

| Campo | Detalle |
|-------|---------|
| Síntoma | 4 tests fallan: `test_create_match`, `test_create_matches_batch`, `test_predict_batch`, `test_run_etl` — `AttributeError: 'SimpleNamespace' object has no attribute 'raise_for_status'` |
| Archivo | `MATCHER-MS/tests/test_clients.py` |
| Causa | `_FakeAsyncClient` definía `raise_for_status=lambda: None` como no-op, pero en realidad necesita lanzar `httpx.HTTPStatusError` cuando `status >= 400`. Además `get()` usaba `SimpleNamespace` sin `raise_for_status`. |
| Resolución | Refactorizar a `_make_resp()` que asigna un closure `raise_for_status` que lanza `HTTPStatusError` si `status >= 400`. Aplica a GET y POST. |
| Commit | `70e3dc2` |

---

## Resumen por severidad

| FC | Severidad | Tipo | Servicio |
|----|-----------|------|----------|
| FC-01 | Baja | Test assertion | swiggy-gateway |
| FC-02 | Alta | Contexto Spring | identity-service |
| FC-03 | Baja | Test assertion | integration-ms |
| FC-04 | Alta | DDL incompatibilidad | api-register-ms |
| FC-05 | Crítica | Sincronización datos | identity + user-registry |
| FC-06 | Media | Query inconsistency | user-registry-ms |
| FC-07 | Alta | Null safety + routing | swiggy-gateway |
| FC-08 | Media | Configuración Docker | Todos |
| FC-09 | Alta | Dependencias | swiggy-gateway + identity |
| FC-10 | Alta | Null safety (NPE→404) | user-registry-ms |
| FC-11 | Baja | Test assertion | MS-SAVE-DATA |
| FC-12 | Alta | Test double incompleto | MATCHER-MS |
