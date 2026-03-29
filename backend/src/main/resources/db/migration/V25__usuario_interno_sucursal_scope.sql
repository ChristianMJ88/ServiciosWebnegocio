CREATE TABLE IF NOT EXISTS usuario_interno_sucursal (
    usuario_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, sucursal_id),
    CONSTRAINT fk_usuario_interno_sucursal_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_interno_sucursal_sucursal
        FOREIGN KEY (sucursal_id) REFERENCES sucursal (id) ON DELETE CASCADE
);

CREATE INDEX idx_usuario_interno_sucursal_sucursal
    ON usuario_interno_sucursal (sucursal_id);
