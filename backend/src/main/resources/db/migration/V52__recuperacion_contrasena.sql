CREATE TABLE recuperacion_contrasena (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    expira_en DATETIME(6) NOT NULL,
    utilizada_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recuperacion_contrasena_token (token_hash),
    KEY idx_recuperacion_usuario_estado (usuario_id, estado, creado_en),
    CONSTRAINT fk_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_recuperacion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
