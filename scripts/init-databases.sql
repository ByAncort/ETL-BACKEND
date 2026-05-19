-- ============================================================================
-- SCRIPT DE CREACIÓN DE BASES DE DATOS - ETL BACKEND
-- ============================================================================
-- Generado a partir de las entidades JPA de todos los microservicios
-- ============================================================================
-- Bases de datos a crear:
--   1. identity_db       (identity-service + user-registry-ms)
--   2. registry_api      (api-register-ms)
--   3. integration_db    (integration-ms)
--   4. schema_matching   (schema-matching-ms)
--   5. llm_config_db     (ETL-CONFIG-LLM-MS)
-- ============================================================================
-- NOTA: Las relaciones cross-database entre microservicios son lógicas.
--       MySQL/InnoDB no soporta FOREIGN KEY entre bases de datos distintas,
--       por lo que se documentan como COMMENT en columnas y tablas para que
--       herramientas de visualización (DBeaver, dbdiagram.io, MySQL Workbench)
--       puedan interpretarlas.
--
-- MAPA DE RELACIONES LÓGICAS CROSS-MS:
--   identity_db.user_credentials.user_ref_id    → identity_db.user.id
--   identity_db.user.created_by                 → identity_db.user.id
--   identity_db.user_role.usuario_id            → identity_db.user.id
--   identity_db.user_role.rol_id                → identity_db.role.id
--   identity_db.user_role.assigned_by           → identity_db.user.id
--   identity_db.password_reset_token.user_id    → identity_db.user.id
--   integration_db.integrations.api_a           → registry_api.apis.id
--   integration_db.integrations.api_b           → registry_api.apis.id
--   integration_db.integrations.created_by      → identity_db.user.id
--   integration_db.integrations.updated_by      → identity_db.user.id
--   schema_matching.schema_match.integration_id → integration_db.integrations.id
--   schema_matching.schema_match.reviewed_by    → identity_db.user.id
--   schema_matching.schema_match.created_by     → identity_db.user.id
--   schema_matching.match_feedback.match_id     → schema_matching.schema_match.id
--   schema_matching.match_feedback.reviewed_by  → identity_db.user.id
--   llm_config_db.llm_config.created_by         → identity_db.user.id
--   llm_config_db.llm_config.updated_by         → identity_db.user.id
--   *.execution_log.parent_id                   → *.execution_log.id (auto-referencia por DB)
--   *.execution_log.integration_id              → integration_db.integrations.id
-- ============================================================================

CREATE DATABASE IF NOT EXISTS identity_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS registry_api
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS integration_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS schema_matching
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS llm_config_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- ============================================================================
-- DATABASE: identity_db
-- Microservicios: identity-service (port 9898), user-registry-ms (port 9090)
-- ============================================================================

-- Tabla: user_credentials (identity-service)
-- LOGICAL FK: user_ref_id → identity_db.user.id
CREATE TABLE IF NOT EXISTS identity_db.user_credentials (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL,
    email       VARCHAR(100)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     DEFAULT 'USER',
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME,
    updated_at  DATETIME,
    user_ref_id BIGINT          COMMENT 'LOGICAL FK → identity_db.user.id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_credentials_username (username),
    UNIQUE KEY uk_user_credentials_email (email),
    KEY idx_user_credentials_user_ref_id (user_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='identity-service | LOGICAL FK: user_ref_id → identity_db.user.id';

-- Tabla: user (user-registry-ms)
-- LOGICAL FK: created_by → identity_db.user.id (auto-referencia)
CREATE TABLE IF NOT EXISTS identity_db.`user` (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    username            VARCHAR(100)    NOT NULL,
    email               VARCHAR(255)    NOT NULL,
    password_hash       VARCHAR(255)    NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    status              VARCHAR(30)     NOT NULL DEFAULT 'pending_verification',
    email_verified_at   DATETIME,
    last_login_at       DATETIME,
    last_login_ip       VARCHAR(45),
    created_at          DATETIME,
    updated_at          DATETIME,
    created_by          BIGINT          COMMENT 'LOGICAL FK → identity_db.user.id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    KEY idx_user_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='user-registry-ms | LOGICAL FK: created_by → identity_db.user.id';

-- Tabla: role (user-registry-ms)
CREATE TABLE IF NOT EXISTS identity_db.role (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255)    NOT NULL,
    description  VARCHAR(255)    NOT NULL,
    level_role   BIGINT,
    is_system    BOOLEAN         DEFAULT FALSE,
    created_at   DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='user-registry-ms';

-- Tabla: user_role (user-registry-ms)
-- LOGICAL FKs: usuario_id → user.id | rol_id → role.id | assigned_by → user.id
CREATE TABLE IF NOT EXISTS identity_db.user_role (
    usuario_id   BIGINT       NOT NULL    COMMENT 'LOGICAL FK → identity_db.user.id',
    rol_id       BIGINT       NOT NULL    COMMENT 'LOGICAL FK → identity_db.role.id',
    assigned_by  BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    assigned_at  DATETIME,
    PRIMARY KEY (usuario_id, rol_id),
    KEY idx_user_role_usuario_id (usuario_id),
    KEY idx_user_role_rol_id (rol_id),
    KEY idx_user_role_assigned_by (assigned_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='user-registry-ms | LOGICAL FKs: usuario_id → user.id, rol_id → role.id, assigned_by → user.id';

-- Tabla: password_reset_token (user-registry-ms)
-- LOGICAL FK: user_id → identity_db.user.id
CREATE TABLE IF NOT EXISTS identity_db.password_reset_token (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    token        VARCHAR(255) NOT NULL,
    user_id      BIGINT       NOT NULL    COMMENT 'LOGICAL FK → identity_db.user.id',
    expiry_date  DATETIME     NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_token_token (token),
    KEY idx_password_reset_token_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='user-registry-ms | LOGICAL FK: user_id → identity_db.user.id';

-- Datos semilla para identity_db
-- Admin por defecto: etladmin / etladmin (password hasheada con BCrypt)
INSERT INTO identity_db.user_credentials (username, email, password, role, enabled, created_at, updated_at)
SELECT 'etladmin', 'etladmin@etladmin.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM identity_db.user_credentials WHERE username = 'etladmin');

-- ============================================================================
-- DATABASE: registry_api
-- Microservicio: api-register-ms (port 8083)
-- ============================================================================

-- Tabla: http_methods
CREATE TABLE IF NOT EXISTS registry_api.http_methods (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_http_methods_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms';

-- Tabla: apis
-- LOGICAL FK (inversa): id ← integration_db.integrations.api_a / api_b
CREATE TABLE IF NOT EXISTS registry_api.apis (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    method_id    BIGINT                  COMMENT 'FK → registry_api.http_methods.id',
    url          VARCHAR(255),
    description  VARCHAR(255),
    created_at   DATETIME,
    auth_api_id  BIGINT                  COMMENT 'FK → registry_api.apis.id',
    PRIMARY KEY (id),
    KEY fk_apis_method_id (method_id),
    KEY fk_apis_auth_api_id (auth_api_id),
    CONSTRAINT fk_apis_method FOREIGN KEY (method_id) REFERENCES registry_api.http_methods (id),
    CONSTRAINT fk_apis_auth_api FOREIGN KEY (auth_api_id) REFERENCES registry_api.apis (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms | LOGICAL FK (inversa): id ← integration_db.integrations.api_a / api_b';

-- Tabla: api_endpoint (hereda de apis mediante JOINED)
CREATE TABLE IF NOT EXISTS registry_api.api_endpoint (
    id           BIGINT  NOT NULL        COMMENT 'FK → registry_api.apis.id (herencia JOINED)',
    path_params  VARCHAR(255),
    query_params VARCHAR(255),
    body         TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_api_endpoint_apis FOREIGN KEY (id) REFERENCES registry_api.apis (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms | Herencia JOINED de registry_api.apis';

-- Tabla: api_headers
CREATE TABLE IF NOT EXISTS registry_api.api_headers (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    value  VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms';

-- Tabla: auth_credential
CREATE TABLE IF NOT EXISTS registry_api.auth_credential (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    credential_value TEXT,
    created_at       DATETIME,
    updated_at       DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms';

-- Tabla: auth_config
CREATE TABLE IF NOT EXISTS registry_api.auth_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    api_id              BIGINT                  COMMENT 'FK → registry_api.apis.id',
    auth_type           VARCHAR(255),
    header_id           BIGINT                  COMMENT 'FK → registry_api.api_headers.id',
    auth_credential_id  BIGINT                  COMMENT 'FK → registry_api.auth_credential.id',
    username            VARCHAR(255),
    password            VARCHAR(255),
    token_endpoint      VARCHAR(255),
    token_expiry        DATETIME,
    created_at          DATETIME,
    PRIMARY KEY (id),
    KEY fk_auth_config_api_id (api_id),
    KEY fk_auth_config_header_id (header_id),
    KEY fk_auth_config_auth_credential_id (auth_credential_id),
    CONSTRAINT fk_auth_config_api FOREIGN KEY (api_id) REFERENCES registry_api.apis (id),
    CONSTRAINT fk_auth_config_header FOREIGN KEY (header_id) REFERENCES registry_api.api_headers (id),
    CONSTRAINT fk_auth_config_credential FOREIGN KEY (auth_credential_id) REFERENCES registry_api.auth_credential (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms';

-- Tabla: execution_log
-- LOGICAL FKs: parent_id → execution_log.id | integration_id → integration_db.integrations.id
CREATE TABLE IF NOT EXISTS registry_api.execution_log (
    id              VARCHAR(36)   NOT NULL,
    parent_id       VARCHAR(36)             COMMENT 'LOGICAL FK → registry_api.execution_log.id',
    execution_id    VARCHAR(36)   NOT NULL,
    service_name    VARCHAR(255)  NOT NULL,
    class_name      VARCHAR(255)  NOT NULL,
    method_name     VARCHAR(255)  NOT NULL,
    log_level       VARCHAR(10)   NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    detail          LONGTEXT,
    timestamp       DATETIME      NOT NULL,
    duration_ms     BIGINT,
    integration_id  VARCHAR(255)            COMMENT 'LOGICAL FK → integration_db.integrations.id',
    PRIMARY KEY (id),
    KEY idx_execution_log_execution_id (execution_id),
    KEY idx_execution_log_timestamp (timestamp),
    KEY idx_execution_log_integration_id (integration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='api-register-ms | LOGICAL FKs: parent_id → execution_log.id, integration_id → integration_db.integrations.id';

-- Métodos HTTP por defecto
INSERT INTO registry_api.http_methods (name) VALUES ('GET'), ('POST'), ('PUT'), ('DELETE'), ('PATCH'), ('HEAD'), ('OPTIONS');

-- ============================================================================
-- DATABASE: integration_db
-- Microservicio: integration-ms (port 8082)
-- ============================================================================

-- Tabla: integrations
-- LOGICAL FKs: api_a → registry_api.apis.id | api_b → registry_api.apis.id
--              created_by → identity_db.user.id | updated_by → identity_db.user.id
CREATE TABLE IF NOT EXISTS integration_db.integrations (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    api_a        VARCHAR(255) NOT NULL    COMMENT 'LOGICAL FK → registry_api.apis.id (API origen)',
    api_b        VARCHAR(255) NOT NULL    COMMENT 'LOGICAL FK → registry_api.apis.id (API destino)',
    description  VARCHAR(255),
    status       VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    updated_by   BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    PRIMARY KEY (id),
    KEY idx_integrations_api_a (api_a),
    KEY idx_integrations_api_b (api_b),
    KEY idx_integrations_created_by (created_by),
    KEY idx_integrations_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='integration-ms | LOGICAL FKs: api_a → registry_api.apis.id, api_b → registry_api.apis.id, created_by / updated_by → identity_db.user.id';

-- Tabla: execution_log
-- LOGICAL FKs: parent_id → execution_log.id | integration_id → integration_db.integrations.id
CREATE TABLE IF NOT EXISTS integration_db.execution_log (
    id              VARCHAR(36)   NOT NULL,
    parent_id       VARCHAR(36)             COMMENT 'LOGICAL FK → integration_db.execution_log.id',
    execution_id    VARCHAR(36)   NOT NULL,
    service_name    VARCHAR(255)  NOT NULL,
    class_name      VARCHAR(255)  NOT NULL,
    method_name     VARCHAR(255)  NOT NULL,
    log_level       VARCHAR(10)   NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    detail          LONGTEXT,
    timestamp       DATETIME      NOT NULL,
    duration_ms     BIGINT,
    integration_id  VARCHAR(255)            COMMENT 'LOGICAL FK → integration_db.integrations.id',
    PRIMARY KEY (id),
    KEY idx_execution_log_execution_id (execution_id),
    KEY idx_execution_log_timestamp (timestamp),
    KEY idx_execution_log_integration_id (integration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='integration-ms | LOGICAL FKs: parent_id → execution_log.id, integration_id → integration_db.integrations.id';

-- ============================================================================
-- DATABASE: schema_matching
-- Microservicio: schema-matching-ms (port 8085)
-- ============================================================================

-- Tabla: schema_match
-- LOGICAL FKs: integration_id → integration_db.integrations.id
--              reviewed_by / created_by → identity_db.user.id
CREATE TABLE IF NOT EXISTS schema_matching.schema_match (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    integration_id  BIGINT                  COMMENT 'LOGICAL FK → integration_db.integrations.id',
    source_field    VARCHAR(500)  NOT NULL,
    target_field    VARCHAR(500)  NOT NULL,
    confidence      DECIMAL(5,4)  NOT NULL,
    status          VARCHAR(255)  NOT NULL DEFAULT 'PENDING',
    transformation  TEXT,
    reviewed_by     BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    reviewed_at     DATETIME,
    created_at      DATETIME,
    created_by      BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    PRIMARY KEY (id),
    KEY idx_schema_match_integration_id (integration_id),
    KEY idx_schema_match_status (status),
    KEY idx_schema_match_reviewed_by (reviewed_by),
    KEY idx_schema_match_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='schema-matching-ms | LOGICAL FKs: integration_id → integration_db.integrations.id, reviewed_by / created_by → identity_db.user.id';

-- Tabla: match_feedback
-- LOGICAL FKs: match_id → schema_matching.schema_match.id | reviewed_by → identity_db.user.id
CREATE TABLE IF NOT EXISTS schema_matching.match_feedback (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    match_id       BIGINT                  COMMENT 'LOGICAL FK → schema_matching.schema_match.id',
    user_approved  BOOLEAN      NOT NULL,
    actual_target  VARCHAR(500),
    created_at     DATETIME,
    reviewed_by    BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    PRIMARY KEY (id),
    KEY idx_match_feedback_match_id (match_id),
    KEY idx_match_feedback_reviewed_by (reviewed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='schema-matching-ms | LOGICAL FKs: match_id → schema_matching.schema_match.id, reviewed_by → identity_db.user.id';

-- Tabla: execution_log
-- LOGICAL FKs: parent_id → execution_log.id | integration_id → integration_db.integrations.id
CREATE TABLE IF NOT EXISTS schema_matching.execution_log (
    id              VARCHAR(36)   NOT NULL,
    parent_id       VARCHAR(36)             COMMENT 'LOGICAL FK → schema_matching.execution_log.id',
    execution_id    VARCHAR(36)   NOT NULL,
    service_name    VARCHAR(255)  NOT NULL,
    class_name      VARCHAR(255)  NOT NULL,
    method_name     VARCHAR(255)  NOT NULL,
    log_level       VARCHAR(10)   NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    detail          LONGTEXT,
    timestamp       DATETIME      NOT NULL,
    duration_ms     BIGINT,
    integration_id  VARCHAR(255)            COMMENT 'LOGICAL FK → integration_db.integrations.id',
    PRIMARY KEY (id),
    KEY idx_execution_log_execution_id (execution_id),
    KEY idx_execution_log_timestamp (timestamp),
    KEY idx_execution_log_integration_id (integration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='schema-matching-ms | LOGICAL FKs: parent_id → execution_log.id, integration_id → integration_db.integrations.id';

-- ============================================================================
-- DATABASE: llm_config_db
-- Microservicio: ETL-CONFIG-LLM-MS (port 8086)
-- ============================================================================

-- Tabla: llm_config
-- LOGICAL FKs: created_by / updated_by → identity_db.user.id
CREATE TABLE IF NOT EXISTS llm_config_db.llm_config (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)  NOT NULL,
    provider     VARCHAR(50)   NOT NULL,
    api_key      VARCHAR(1000) NOT NULL,
    base_url     VARCHAR(500)  NOT NULL,
    model_name   VARCHAR(200),
    is_default   BOOLEAN       NOT NULL DEFAULT FALSE,
    status       VARCHAR(255)  NOT NULL DEFAULT 'active',
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    updated_by   BIGINT                  COMMENT 'LOGICAL FK → identity_db.user.id',
    PRIMARY KEY (id),
    KEY idx_llm_config_created_by (created_by),
    KEY idx_llm_config_updated_by (updated_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='ETL-CONFIG-LLM-MS | LOGICAL FKs: created_by / updated_by → identity_db.user.id';

-- Tabla: execution_log
-- LOGICAL FKs: parent_id → execution_log.id | integration_id → integration_db.integrations.id
CREATE TABLE IF NOT EXISTS llm_config_db.execution_log (
    id              VARCHAR(36)   NOT NULL,
    parent_id       VARCHAR(36)             COMMENT 'LOGICAL FK → llm_config_db.execution_log.id',
    execution_id    VARCHAR(36)   NOT NULL,
    service_name    VARCHAR(255)  NOT NULL,
    class_name      VARCHAR(255)  NOT NULL,
    method_name     VARCHAR(255)  NOT NULL,
    log_level       VARCHAR(10)   NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    detail          LONGTEXT,
    timestamp       DATETIME      NOT NULL,
    duration_ms     BIGINT,
    integration_id  VARCHAR(255)            COMMENT 'LOGICAL FK → integration_db.integrations.id',
    PRIMARY KEY (id),
    KEY idx_execution_log_execution_id (execution_id),
    KEY idx_execution_log_timestamp (timestamp),
    KEY idx_execution_log_integration_id (integration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='ETL-CONFIG-LLM-MS | LOGICAL FKs: parent_id → execution_log.id, integration_id → integration_db.integrations.id';