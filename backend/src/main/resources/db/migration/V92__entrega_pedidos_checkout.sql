ALTER TABLE empresa_checkout_config
    ADD COLUMN permitir_recoleccion BOOLEAN NOT NULL DEFAULT TRUE AFTER permitir_pago_servicios,
    ADD COLUMN permitir_entrega_local BOOLEAN NOT NULL DEFAULT FALSE AFTER permitir_recoleccion,
    ADD COLUMN costo_entrega_local DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER permitir_entrega_local,
    ADD COLUMN permitir_envio_nacional BOOLEAN NOT NULL DEFAULT FALSE AFTER costo_entrega_local,
    ADD COLUMN costo_envio_nacional DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER permitir_envio_nacional;

ALTER TABLE checkout_orden
    ADD COLUMN modalidad_entrega VARCHAR(30) NOT NULL DEFAULT 'RECOGE_EN_SUCURSAL' AFTER correo_cliente,
    ADD COLUMN nombre_destinatario VARCHAR(180) NULL AFTER modalidad_entrega,
    ADD COLUMN telefono_destinatario VARCHAR(30) NULL AFTER nombre_destinatario,
    ADD COLUMN calle VARCHAR(180) NULL AFTER telefono_destinatario,
    ADD COLUMN numero_exterior VARCHAR(30) NULL AFTER calle,
    ADD COLUMN numero_interior VARCHAR(30) NULL AFTER numero_exterior,
    ADD COLUMN colonia VARCHAR(150) NULL AFTER numero_interior,
    ADD COLUMN municipio VARCHAR(150) NULL AFTER colonia,
    ADD COLUMN estado_direccion VARCHAR(120) NULL AFTER municipio,
    ADD COLUMN codigo_postal VARCHAR(12) NULL AFTER estado_direccion,
    ADD COLUMN referencias_direccion VARCHAR(500) NULL AFTER codigo_postal,
    ADD COLUMN costo_envio DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER impuestos_total,
    ADD INDEX idx_checkout_orden_entrega (empresa_id, modalidad_entrega, estado);
