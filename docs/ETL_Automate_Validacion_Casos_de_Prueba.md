# Validación de Casos de Prueba
> Plan de pruebas automatizadas — ETL Automate, Ciclo 1

| Propiedad | Valor |
|-----------|-------|
| Proyecto | ETL Automate |
| Módulos | identity-service, api-register-ms, integration-ms, MS-SAVE-DATA, MATCHER-MS, semantic-field-mapper, frontend |
| Jefe de Proyecto | Jeremy Parada |
| Analista Testing | Felipe Duarte |
| Ciclo | 1 |
| Fecha | 2026-06-05 |

---

## Instrucciones para el Agente Validador

Para cada caso de prueba, verifica que el test asociado cumpla con los criterios indicados.
Marca el resultado con uno de los siguientes estados:

| Estado | Significado |
|--------|-------------|
| ✅ OK | El test cumple completamente el caso de prueba |
| ❌ NC | No cumple el ítem en forma óptima |
| ⚠️ P | Parcial — cumple solo parte del criterio |
| ➖ NA | No aplica al componente/módulo evaluado |

---

## Sección 1 — Pruebas Estándar de Interfaz Gráfica

### 1.1 Interfaz Gráfica

| ID | Descripción | Estado Esperado | Estado del Test | Observaciones |
|----|-------------|-----------------|-----------------|---------------|
| E-1.1 | Existe un ícono en todos los mensajes desplegados por la aplicación | OK | | |
| E-1.2 | Los botones tienen un ícono o frase adecuado a su función | OK | | |
| E-1.3 | En aplicaciones con múltiples pantallas existe la opción "Retornar" a la pantalla anterior | OK | | |
| E-1.4 | Al intentar acceder a secciones internas sin sesión iniciada, el usuario es redirigido al Login | — | | Verificar implementación |
| E-1.5 | El menú lateral resalta visualmente la sección actual del usuario | — | | Verificar implementación |
| E-1.6 | Los íconos se renderizan correctamente con estilo y tamaño uniforme | — | | Verificar implementación |
| E-1.7 | Acciones con procesamiento deshabilitan el botón temporalmente y muestran animación de carga | — | | Verificar implementación |
| E-1.8 | Los mensajes de error y éxito aparecen como alertas temporales con colores adecuados | — | | Verificar implementación |
| E-1.9 | Las ventanas emergentes se cierran al presionar "Cancelar" o al hacer clic fuera | — | | Verificar implementación |
| E-1.10 | Al ingresar datos inválidos, los formularios marcan los campos con color diferente | — | | Verificar implementación |
| E-1.11 | Todos los campos de contraseña incluyen botón tipo ojo para revelar/ocultar el texto | — | | Verificar implementación |
| E-1.12 | Si falla la carga de un panel por pérdida de conexión, la interfaz muestra mensaje amigable | — | | Verificar implementación |
| E-1.13 | En secciones sin registros creados, la interfaz muestra ilustración o texto de lista vacía | — | | Verificar implementación |

### 1.2 Validación de Datos de Entrada

| ID | Descripción | Estado Esperado | Estado del Test | Observaciones |
|----|-------------|-----------------|-----------------|---------------|
| E-2.1 | Correcta lectura de la información ingresada | OK | | |
| E-2.2 | No se pueden hacer modificaciones en pantallas de Consultas | NA | | No aplica |
| E-2.3 | La eliminación de información funciona correctamente | OK | | |
| E-2.4 | Campo "Usuario" en creación de cuenta requiere mínimo 3 y máximo 50 caracteres | — | | Verificar implementación |
| E-2.5 | Campo "Usuario" solo acepta letras, números y guiones bajos | — | | Verificar implementación |
| E-2.6 | Campos "Nombre" y "Apellido" requieren entre 1 y 50 caracteres, solo letras | — | | Verificar implementación |
| E-2.7 | Campo "Email" exige formato estándar, máx. 100 caracteres | — | | Verificar implementación |
| E-2.8 | Campo "Contraseña" requiere mínimo 6 y máximo 100 caracteres | — | | Verificar implementación |
| E-2.9 | Campo "Confirmar Contraseña" debe coincidir exactamente con la contraseña inicial | — | | Verificar implementación |
| E-2.10 | No se puede enviar el formulario de Login si "Usuario" o "Contraseña" están vacíos | — | | Verificar implementación |
| E-2.11 | Al crear/editar una conexión/API, el botón de guardado queda deshabilitado si no hay URL Base | — | | Verificar implementación |

### 1.3 Pruebas Funcionales

| ID | Descripción | Estado Esperado | Estado del Test | Observaciones |
|----|-------------|-----------------|-----------------|---------------|
| E-3.1 | La aplicación es compatible con el resto de aplicaciones del Ambiente de Pruebas | OK | | |
| E-3.2 | Aspectos técnicos validados (Tablas, Bases de Datos, perfiles) | OK | | |
| E-3.3 | Correcto acceso a submódulos o programas | OK | | |
| E-3.4 | Registrar cuenta con usuario/email duplicado aborta la creación y muestra mensaje claro | — | | Verificar implementación |
| E-3.5 | Usuario recién registrado no puede iniciar sesión hasta que la cuenta sea activada | — | | Verificar implementación |
| E-3.6 | Si la sesión caduca (token JWT) o credenciales de refresco son inválidas, el sistema fuerza logout | — | | Verificar implementación |
| E-3.7 | Intentar cargar detalle de API/Conexión inexistente muestra estado vacío/error | — | | Verificar implementación |
| E-3.8 | "Test Connection" contra endpoint caído captura el error y notifica visualmente al usuario | — | | Verificar implementación |

### 1.4 Verificación de Informes

| ID | Descripción | Estado Esperado | Estado del Test | Observaciones |
|----|-------------|-----------------|-----------------|---------------|
| E-4.1 | Correcto formato de los informes generados | NA | | No aplica |
| E-4.2 | Numeración secuencial lógica en los informes | NA | | No aplica |
| E-4.3 | El proceso de emisión del informe se realiza correctamente | OK | | |

### 1.5 Impresiones

| ID | Descripción | Estado Esperado | Estado del Test | Observaciones |
|----|-------------|-----------------|-----------------|---------------|
| E-5.1 | En listados los márgenes están correctos y el ancho no sobrepasa la hoja | NA | | No aplica |
| E-5.2 | El formato de la información en impresiones es comprensible | NA | | No aplica |

---

## Sección 2 — Casos de Prueba Automatizados (144 pruebas)

### Módulo: identity-service

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 1.0 | HU-00.2 | CP-01 | JWT | Generación y validación de token JWT | 1. Generar token 2. Extraer claims 3. Validar firma | Usuario válido; clave de firma configurada | Token firmado, username extraído coincide, validación exitosa | | Ref: JwtServiceTest, 9 tests |
| 2.0 | HU-00.2 | CP-02 | JWT | Rechazo de token inválido o expirado | 1. Construir token inválido/expirado 2. Validar | Token manipulado/expirado | Validación retorna falso | | Ref: JwtServiceTest |
| 3.0 | HU-00.2 | CP-03 | Logout | Invalidación de token por blacklist (logout) | 1. Logout (blacklist) 2. Reintentar validación | Token válido emitido | Token invalidado tras logout | | Ref: JwtServiceTest |
| 4.0 | HU-00.2 | CP-04 | Login | Autenticación de credenciales (login) | 1. POST /api/v1/auth con credenciales 2. Verificar respuesta | @WebMvcTest con AuthService mockeado | Credenciales correctas → token; incorrectas → error | | Ref: AuthControllerTest, 6 tests |
| 5.0 | HU-00.2 | CP-05 | Sesión | Desactivar usuario invalida su token vigente | 1. Desactivar usuario 2. Validar token | Usuario con token activo | Token deja de ser válido | | Ref: AuthServiceTest, 4 tests |

### Módulo: frontend

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 6.0 | HU-09 | CP-06 | Manejo de errores | API caída muestra mensaje mapeado | 1. Simular API caída 2. Renderizar | Vitest + jsdom; fetch simulado | Mensaje de conexión comprensible, UI no se rompe | | Ref: apiError.test.ts, 7 tests |
| 7.0 | HU-09 | CP-07 | Iconografía/Color | ErrorState con ícono, color y reintento | 1. Renderizar ErrorState 2. Verificar ícono/clase 3. Click reintentar | React Testing Library | Ícono y color correctos; callback se dispara | | Ref: ErrorState.test.tsx, 4 tests |
| 8.0 | HU-05 | CP-08 | Navegación | Menú superior consistente entre vistas | 1. Renderizar Header en distintas rutas 2. Comparar | MemoryRouter + AuthProvider | Menú idéntico en todas las vistas | | Ref: Header.test.tsx, 4 tests |

### Módulo: api-register-ms

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 9.0 | HU-01 | CP-09 | Normalización URL | Normalización de slashes en URL | 1. Normalizar URLs con // y / 2. Comparar | — | URL canónica única | | Ref: UrlNormalizerTest, 8 tests |
| 10.0 | HU-01 | CP-10 | Validación @Valid | Payload inválido devuelve 400 (no 500) | 1. POST sin url o method 2. Verificar código | @WebMvcTest (MockMvc) | 400 Bad Request | | Ref: ApiControllerTest, 4 tests |
| 11.0 | HU-01 | CP-11 | Registro API | Registro de API válida persiste | 1. POST con method+url válidos 2. Verificar persistencia | H2 en memoria (MODE=MySQL) | 200 OK; API registrada y recuperable | | Ref: ApiControllerTest |

### Módulo: MS-SAVE-DATA

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 12.0 | HU-06 | CP-12 | Transformación | Fase TRANSFORM: mapeo y conversiones | 1. Aplicar matches 2. Verificar mapeo | pytest | Registros transformados según matches | | Ref: test_transform_service.py, 12 tests |
| 13.0 | HU-06, HU-08 | CP-13 | Orquestación ETL | Flujo ETL: orden de fases y conteos | 1. Ejecutar run_etl 2. Verificar orden | AsyncMock | Fases en orden; total/transformed/loaded correctos | | Ref: test_etl_orchestrator.py, 9 tests |
| 14.0 | HU-09 | CP-14 | Manejo de fallos | Error por fase se notifica al log service | 1. Forzar excepción 2. Verificar EtlResponse | Mock | Respuesta con errores; log ERROR con fase | | Ref: test_etl_orchestrator.py |
| 15.0 | HU-06 | CP-15 | Precondición | Sin matches ACCEPTED corta antes de extraer | 1. run_etl sin matches 2. Verificar corte | — | Mensaje; no se llama a extract | | Ref: test_etl_orchestrator.py |
| 16.0 | HU-03, HU-07 | CP-16 | Notificar fin | Notificación de fin del proceso ETL | 1. run_etl exitoso 2. Verificar log | — | Una notificación con resumen y durationMs | | Ref: test_notifica_fin_exitoso |
| 17.0 | HU-07 | CP-17 | Integración logs | LogClient apunta al endpoint real de logs | 1. Enviar log 2. Verificar URL | httpx simulado | POST a `{base}/api/integrations/logs` | | Ref: test_clients.py, 2 tests |
| 18.0 | HU-06 | CP-18 | API ETL | Endpoint /api/etl/run delega y valida | 1. POST válido/inválido/error 2. Verificar códigos | FastAPI TestClient | 200 al delegar; 502 error; 422 inválido | | Ref: test_endpoints.py, 5 tests |

### Módulo: integration-ms

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 19.0 | HU-07 | CP-19 | Registro de proceso | LogService registra ejecución (parent/child) | 1. createParentLog/createChildLog 2. updateParentLog | Mockito; H2 | Logs persistidos; fallo no propaga excepción | | Ref: LogServiceTest, 6 tests |
| 20.0 | HU-07, HU-03 | CP-20 | API logs | Endpoint de logs persiste y consulta | 1. POST log 2. GET ?integrationId | @WebMvcTest | 200; logLevel normalizado a enum | | Ref: LogControllerTest, 3 tests |
| 21.0 | HU-08 | CP-21 | Crear integración | createIntegration valida APIs, guarda y dispara matching | 1. POST válida 2. Verificar validaciones + save + POST | RestTemplate mockeado | Integración persistida; matching disparado | | Ref: IntegrationServiceTest, 6 tests |
| 22.0 | HU-09 | CP-22 | Validación integración | API inválida no persiste integración | 1. POST con API inválida 2. Verificar no persistencia | — | Excepción 'Invalid API ID'; sin save | | Ref: IntegrationServiceTest |
| 23.0 | HU-08 | CP-23 | Re-ejecución matching | runMatching reactiva integración eliminada | 1. runMatching sobre DELETED 2. Verificar estado | — | Estado → ACTIVE; se consulta matcher | | Ref: IntegrationServiceTest |
| 24.0 | HU-02 | CP-24 | Eliminar integración | Eliminación lógica de integración | 1. deleteIntegration 2. Verificar status | — | status=DELETED; registro conservado | | Ref: IntegrationServiceTest |
| 25.0 | HU-05 | CP-25 | Listar integraciones | Listado excluye integraciones eliminadas | 1. getAllIntegrations 2. Verificar exclusión | — | Solo integraciones activas | | Ref: IntegrationServiceTest |

### Módulo: MATCHER-MS

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 26.0 | HU-04, HU-08 | CP-26 | Pipeline matching | match_and_register registra matches (LLM) | 1. match_and_register con LLM 2. Verificar registro | Mock | modelUsed=LLM; matches con ACCEPTED | | Ref: test_match_and_register.py, 4 tests |
| 27.0 | HU-04, HU-08 | CP-27 | Fallback ML | Fallback a modelo semántico si el LLM falla | 1. Forzar fallo LLM 2. Verificar SemanticService | — | modelUsed=semantic-model; matches registrados | | Ref: test_match_and_register.py |
| 28.0 | HU-04 | CP-28 | Extracción de campos | Extracción de campos a mapear | 1. Extraer campos según método 2. Verificar listas | pytest | Lista correcta; 502 ante body vacío | | Ref: test_matching_service.py, 9 tests |
| 29.0 | HU-04 | CP-29 | Cliente semántico | SemanticService: pares y confianza | 1. match_fields(a,b) 2. Verificar pares | httpx simulado | Solo pares similares; confidence = score | | Ref: test_semantic_service.py, 6 tests |
| 30.0 | HU-09 | CP-30 | Manejo de errores ML | Error del modelo semántico devuelve 502 | 1. Simular status 500 2. Verificar excepción | — | HTTPException 502 | | Ref: test_semantic_service.py |

### Módulo: semantic-field-mapper

| N° | Caso de Uso | ID Prueba | Funcionalidad | Qué Probar | Cómo Probarlo | Datos de Prueba | Resultado Esperado | ¿Cumple? | Observaciones |
|----|-------------|-----------|---------------|------------|---------------|-----------------|-------------------|----------|---------------|
| 31.0 | HU-04 | CP-31 | Features semánticos | Extracción de features de nombres de campo | 1. Tokenizar 2. Extraer vector | Datos embebidos (sin Mongo) | Vector de 9 features en rangos válidos | | Ref: test_semantic_features.py, 20 tests |
| 32.0 | HU-04 | CP-32 | Config robusta | Config sin MongoDB | 1. Instanciar sin Mongo 2. Leer datos | MongoClient deshabilitado | Datos de respaldo; índice correcto | | Ref: test_config_mongodb.py |
| 33.0 | HU-04 | CP-33 | Persistencia config | Config con MongoDB (mongomock) | 1. Insertar/leer tokens 2. buscar_token | mongomock | Lectura/escritura correctas | | Ref: test_config_mongodb.py, 13 tests |

---

## Sección 3 — Resumen de Totales

### Pruebas Estándar QA

| Métrica | Valor Esperado |
|---------|---------------|
| Total Pruebas Estándar OK | 9 |
| Total Pruebas Estándar NO CUMPLE | 0 |
| Total Pruebas Estándar NO APLICA | 5 |

### Casos de Prueba Automatizados

| Métrica | Valor Esperado | ¿Coincide? |
|---------|---------------|------------|
| Total casos de prueba | 33 | |
| Total casos OK | 33 | |
| Total casos NO OK | 0 | |
| Total casos NO Aplica | 0 | |
| Total casos no revisados | 0 | |

---

## Sección 4 — Criterios de Aprobación Global

El agente debe verificar que se cumplan **todos** los criterios siguientes para dar el ciclo de pruebas por aprobado:

- [ ] Todos los 33 casos de prueba automatizados tienen resultado **OK**
- [ ] No existe ningún caso de prueba con resultado **NO OK**
- [ ] Las pruebas estándar con estado esperado **OK** (E-1.1, E-1.2, E-1.3, E-2.1, E-2.3, E-3.1, E-3.2, E-3.3, E-4.3) están cubiertas
- [ ] Los ítems marcados como **NA** (E-2.2, E-4.1, E-4.2, E-5.1, E-5.2) no son evaluados
- [ ] Los ítems sin estado en la planilla original (E-1.4 a E-1.13, E-2.4 a E-2.11, E-3.4 a E-3.8) deben ser evaluados manualmente
- [ ] El total de pruebas automatizadas ejecutadas suma **144** (distribuidas en los 33 casos de prueba)
