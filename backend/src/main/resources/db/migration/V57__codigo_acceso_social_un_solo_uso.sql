CREATE TABLE codigo_acceso_social (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor VARCHAR(30) NOT NULL,
    subject_proveedor VARCHAR(255) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expira_en DATETIME(6) NOT NULL,
    usado_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL,
    CONSTRAINT uk_codigo_acceso_social_token UNIQUE (token_hash),
    INDEX idx_codigo_acceso_social_identidad (proveedor, subject_proveedor)
);
