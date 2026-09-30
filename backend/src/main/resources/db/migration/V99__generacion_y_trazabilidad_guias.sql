ALTER TABLE envio_pedido
    ADD COLUMN url_rastreo VARCHAR(1000) NULL AFTER url_etiqueta,
    ADD COLUMN proveedor_envio_id VARCHAR(160) NULL AFTER numero_guia,
    ADD COLUMN guia_generada_en DATETIME(6) NULL AFTER ultimo_error,
    ADD COLUMN rastreo_actualizado_en DATETIME(6) NULL AFTER guia_generada_en;

CREATE TABLE envio_evento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    envio_pedido_id BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    descripcion VARCHAR(500) NULL,
    referencia_externa VARCHAR(190) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_envio_evento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_envio_evento_pedido FOREIGN KEY (envio_pedido_id) REFERENCES envio_pedido(id),
    CONSTRAINT uk_envio_evento_referencia UNIQUE (empresa_id, referencia_externa),
    INDEX idx_envio_evento_pedido (empresa_id, envio_pedido_id, creado_en)
);
