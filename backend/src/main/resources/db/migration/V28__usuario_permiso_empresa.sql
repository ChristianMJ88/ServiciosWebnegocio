CREATE TABLE IF NOT EXISTS usuario_permiso_empresa (
    usuario_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, permiso_id),
    CONSTRAINT fk_usuario_permiso_empresa_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_permiso_empresa_permiso
        FOREIGN KEY (permiso_id) REFERENCES permiso(id) ON DELETE CASCADE
);
