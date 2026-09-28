CREATE TABLE envio_cotizacion (
    id VARCHAR(36) NOT NULL,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    proveedor VARCHAR(40) NOT NULL,
    ambiente VARCHAR(20) NOT NULL,
    carrito_hash VARCHAR(64) NOT NULL,
    destino_hash VARCHAR(64) NOT NULL,
    paqueteria VARCHAR(80) NOT NULL,
    paqueteria_nombre VARCHAR(120) NULL,
    servicio VARCHAR(120) NOT NULL,
    servicio_nombre VARCHAR(180) NULL,
    entrega_estimada VARCHAR(120) NULL,
    costo_proveedor DECIMAL(12,2) NOT NULL,
    costo_cliente DECIMAL(12,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    expira_en DATETIME(6) NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_envio_cotizacion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_envio_cotizacion_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    INDEX idx_envio_cotizacion_vigencia (empresa_id, expira_en),
    INDEX idx_envio_cotizacion_busqueda (empresa_id, sucursal_id, carrito_hash, destino_hash)
);

ALTER TABLE checkout_orden
    ADD COLUMN envio_cotizacion_id VARCHAR(36) NULL AFTER costo_envio,
    ADD CONSTRAINT fk_checkout_orden_envio_cotizacion FOREIGN KEY (envio_cotizacion_id) REFERENCES envio_cotizacion(id),
    ADD INDEX idx_checkout_orden_envio_cotizacion (empresa_id, envio_cotizacion_id);

ALTER TABLE envio_pedido
    ADD COLUMN envio_cotizacion_id VARCHAR(36) NULL AFTER checkout_orden_id,
    ADD CONSTRAINT fk_envio_pedido_cotizacion FOREIGN KEY (envio_cotizacion_id) REFERENCES envio_cotizacion(id);
