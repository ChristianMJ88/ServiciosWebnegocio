CREATE TABLE verificacion_correo_onboarding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    correo VARCHAR(190) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL,
    expira_en DATETIME(6) NOT NULL,
    confirmado_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_verificacion_correo_usuario (empresa_id, usuario_id, estado),
    CONSTRAINT fk_verificacion_correo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_verificacion_correo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
