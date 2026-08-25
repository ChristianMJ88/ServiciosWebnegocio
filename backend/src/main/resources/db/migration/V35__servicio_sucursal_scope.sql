CREATE TABLE IF NOT EXISTS servicio_sucursal (
    servicio_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (servicio_id, sucursal_id),
    CONSTRAINT fk_servicio_sucursal_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id),
    CONSTRAINT fk_servicio_sucursal_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    CONSTRAINT fk_servicio_sucursal_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE INDEX idx_servicio_sucursal_empresa_servicio
    ON servicio_sucursal (empresa_id, servicio_id, activo);

CREATE INDEX idx_servicio_sucursal_empresa_sucursal
    ON servicio_sucursal (empresa_id, sucursal_id, activo);

INSERT INTO servicio_sucursal (servicio_id, sucursal_id, empresa_id, activo)
SELECT s.id, s.sucursal_id, s.empresa_id, TRUE
FROM servicio s
WHERE s.sucursal_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    empresa_id = VALUES(empresa_id),
    activo = VALUES(activo),
    actualizado_en = CURRENT_TIMESTAMP;
