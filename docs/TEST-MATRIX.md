# Test Matrix
> Inventario completo de pruebas automatizadas — 9 microservicios, 388 tests

| Propiedad | Valor |
|-----------|-------|
| Proyecto | ETL-BACKEND |
| Spring Boot | 3.3.4 |
| Java | 21 |
| JUnit | 5 |
| Mockito | 5 |
| Total tests | 388 |
| Passing | 388 |
| Failing | 0 |
| Última Actualización | 2026-06-19 |

---

## identity-service (62 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `AuthControllerTest` | Controller | 15 | register/login/logout/refresh/validate — success + invalid token + duplicate user |
| `JwtServiceTest` | Service | 21 | generate/validate/refresh JWT — expired/malformed/invalid token edge cases |
| `AuthServiceTest` | Service | 18 | register/authenticate/refresh/logout — success + wrong password + user not found |
| `UserCredentialRepositoryTest` | Repository | 8 | findByUsername/findByEmail/existsByUsername/existsByEmail — found + not found |

---

## user-registry-ms (126 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `UserControllerTest` | Controller | 29 | CRUD users, search, activate/deactivate, pagination — success + 404 + 400 + 409 |
| `RoleControllerTest` | Controller | 10 | CRUD roles, assign/unassign — success + duplicate + not found |
| `UserServiceTest` | Service | 33 | CRUD, search, pagination, assign roles, activate/deactivate — all edge cases |
| `RoleServiceTest` | Service | 18 | CRUD roles, findByName, pagination — success + duplicate + not found |
| `UserRoleServiceTest` | Service | 14 | assign/unassign roles, get user roles, get role users — success + already assigned |
| `UserRepositoryTest` | Repository | 13 | findByUsername/findByEmail/search/exists — custom queries + pagination |
| `PasswordResetTokenRepositoryTest` | Repository | 8 | findByToken/findByUser/deleteByUser — found + expired + not found |

---

## api-register-ms (51 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `ApiControllerTest` | Controller | 13 | CRUD endpoints, execution — success + 404 + 400 + validation |
| `ApiServiceTest` | Service | 19 | CRUD, execute, search, pagination — success + not found + integration call |
| `LogServiceTest` | Service | 10 | CRUD logs, findByApiId, pagination — success + empty results |
| `ApisRepositoryTest` | Repository | 8 | CRUD, search, findByStatus, pagination — custom queries |

---

## integration-ms (35 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `IntegrationControllerTest` | Controller | 8 | CRUD integrations, execute — success + 404 + 400 |
| `LogControllerTest` | Controller | 7 | CRUD logs, findByIntegrationId — success + not found |
| `IntegrationServiceTest` | Service | 16 | CRUD, execute, search, pagination — success + not found + edge cases |
| `IntegrationRepositoryTest` | Repository | 3 | findByStatus, findBySource — custom queries |

---

## schema-matching-ms (35 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `SchemaMatchControllerTest` | Controller | 13 | CRUD schema matches, execute matching — success + 404 + 400 |
| `SchemaMatchServiceTest` | Service | 14 | CRUD, execute matching, search, pagination — success + not found |
| `SchemaMatchRepositoryTest` | Repository | 7 | findBySourceType, findByTargetType, findByStatus — custom queries |

---

## ETL-CONFIG-LLM-MS (30 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `LlmConfigControllerTest` | Controller | 8 | CRUD LLM configs — success + 404 + 400 + validation |
| `LlmConfigServiceTest` | Service | 16 | CRUD, pagination, default config — success + not found + edge cases |
| `LlmConfigRepositoryTest` | Repository | 5 | findByProvider, findByIsDefault — custom queries |

---

## demo (13 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `WorkspaceProjectControllerTest` | Controller | 4 | CRUD workspace projects — success + 404 |
| `WorkspaceProjectServiceTest` | Service | 4 | CRUD — success + not found |
| `WorkspaceProjectRepositoryTest` | Repository | 4 | CRUD — basic persistence |

---

## swiggy-gateway (35 tests)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `JwtUtilTest` | Util | 15 | validate token, extract claims — valid/expired/malformed/empty token |
| `RouteValidatorTest` | Filter | 13 | isSecured/isAdminOnly — open vs protected routes, admin routes |
| `AuthenticationFilterTest` | Filter | 6 | filter chain with valid/invalid/missing token, admin role check |

---

## swiggy-service-registry (1 test)

| Test class | Layer | Tests | Covers |
|-----------|-------|-------|--------|
| `SwiggyServiceRegistryApplicationTests` | Context | 1 | Application context loads |

---

## Notas de configuración

- **H2 in-memory database** usada para todos los tests repository (`@DataJpaTest`)
- **Test slices** (`@WebMvcTest`, `@DataJpaTest`) con `@MockBean` (Boot 3.3.x)
- **Eureka deshabilitado** en perfiles de test — warnings de connection refused son esperados e inofensivos
- **3 entidades** modificadas en `api-register-ms` para compatibilidad H2: `ApiEndpoint`, `AuthCredential`, `ExecutionLog` (`columnDefinition` → `@Lob`)
- Todos los servicios usan `mvnw.cmd` (Maven wrapper) — no requiere Maven global
