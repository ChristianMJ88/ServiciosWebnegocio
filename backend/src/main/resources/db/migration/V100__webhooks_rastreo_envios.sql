ALTER TABLE empresa_envio_config
    ADD COLUMN webhook_token_hash VARCHAR(64) NULL AFTER token_cifrado,
    ADD COLUMN webhook_token_cifrado TEXT NULL AFTER webhook_token_hash,
    ADD CONSTRAINT uk_empresa_envio_webhook_token UNIQUE (webhook_token_hash);

CREATE TABLE evento_webhook_envio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    proveedor VARCHAR(40) NOT NULL,
    evento_externo_id VARCHAR(190) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    estado_procesamiento VARCHAR(30) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    payload MEDIUMTEXT NOT NULL,
    error_ultimo VARCHAR(1000) NULL,
    recibido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    procesado_en DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evento_webhook_envio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT uk_evento_webhook_envio UNIQUE (empresa_id, proveedor, evento_externo_id),
    INDEX idx_evento_webhook_envio_pendiente (estado_procesamiento, recibido_en)
);
