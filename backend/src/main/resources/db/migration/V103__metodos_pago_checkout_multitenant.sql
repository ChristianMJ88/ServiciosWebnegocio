CREATE TABLE empresa_metodo_pago_checkout (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    proveedor_pago_id BIGINT NOT NULL,
    metodo_externo_id VARCHAR(80) NOT NULL,
    tipo_externo VARCHAR(40) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    solo_productos BOOLEAN NOT NULL DEFAULT TRUE,
    vigencia_dias INT NOT NULL DEFAULT 3,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_empresa_metodo_pago (empresa_id, proveedor_pago_id, metodo_externo_id),
    KEY idx_empresa_metodo_pago_activo (empresa_id, activo),
    CONSTRAINT fk_empresa_metodo_pago_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_empresa_metodo_pago_proveedor FOREIGN KEY (proveedor_pago_id) REFERENCES proveedor_pago(id),
    CONSTRAINT chk_empresa_metodo_pago_vigencia CHECK (vigencia_dias BETWEEN 1 AND 30)
);

ALTER TABLE intento_pago_checkout
    ADD COLUMN metodo_pago_id VARCHAR(80) NULL AFTER proveedor_pago_id,
    ADD COLUMN tipo_pago_id VARCHAR(40) NULL AFTER metodo_pago_id;
