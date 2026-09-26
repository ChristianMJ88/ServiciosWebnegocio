CREATE TABLE proveedor_pago (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    pagos_unicos BOOLEAN NOT NULL DEFAULT TRUE,
    suscripciones BOOLEAN NOT NULL DEFAULT FALSE,
    marketplace BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_proveedor_pago_codigo UNIQUE (codigo)
);

INSERT INTO proveedor_pago (codigo, nombre, pagos_unicos, suscripciones, marketplace, activo)
VALUES
    ('MERCADO_PAGO', 'Mercado Pago', TRUE, TRUE, TRUE, TRUE),
    ('STRIPE', 'Stripe', TRUE, TRUE, TRUE, TRUE)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    pagos_unicos = VALUES(pagos_unicos),
    suscripciones = VALUES(suscripciones),
    marketplace = VALUES(marketplace),
    activo = VALUES(activo);

CREATE TABLE empresa_proveedor_pago (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    proveedor_pago_id BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'NO_CONECTADO',
    ambiente VARCHAR(20) NOT NULL DEFAULT 'PRUEBA',
    cuenta_externa_id VARCHAR(160) NULL,
    correo_cuenta VARCHAR(180) NULL,
    llave_publica VARCHAR(500) NULL,
    token_acceso_cifrado MEDIUMTEXT NULL,
    token_actualizacion_cifrado MEDIUMTEXT NULL,
    token_expira_en DATETIME(6) NULL,
    conectado_en DATETIME(6) NULL,
    desconectado_en DATETIME(6) NULL,
    ultima_sincronizacion_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_empresa_proveedor_pago_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_empresa_proveedor_pago_proveedor FOREIGN KEY (proveedor_pago_id) REFERENCES proveedor_pago(id),
    CONSTRAINT uk_empresa_proveedor_pago UNIQUE (empresa_id, proveedor_pago_id, ambiente),
    INDEX idx_empresa_proveedor_pago_estado (empresa_id, estado, ambiente)
);

CREATE TABLE empresa_checkout_config (
    empresa_id BIGINT NOT NULL,
    proveedor_predeterminado_id BIGINT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    aceptar_tarjeta BOOLEAN NOT NULL DEFAULT TRUE,
    aceptar_transferencia BOOLEAN NOT NULL DEFAULT TRUE,
    aceptar_efectivo_referenciado BOOLEAN NOT NULL DEFAULT FALSE,
    permitir_pago_productos BOOLEAN NOT NULL DEFAULT FALSE,
    permitir_pago_servicios BOOLEAN NOT NULL DEFAULT FALSE,
    modalidad_servicios VARCHAR(30) NOT NULL DEFAULT 'PAGO_COMPLETO',
    anticipo_porcentaje DECIMAL(7,4) NULL,
    anticipo_importe DECIMAL(12,2) NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'MXN',
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (empresa_id),
    CONSTRAINT fk_empresa_checkout_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_empresa_checkout_proveedor FOREIGN KEY (proveedor_predeterminado_id) REFERENCES proveedor_pago(id)
);

CREATE TABLE regla_comision_plataforma (
    id BIGINT NOT NULL AUTO_INCREMENT,
    proveedor_pago_id BIGINT NOT NULL,
    empresa_id BIGINT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor DECIMAL(12,4) NOT NULL,
    importe_minimo DECIMAL(12,2) NULL,
    importe_maximo DECIMAL(12,2) NULL,
    moneda VARCHAR(10) NULL,
    prioridad INT NOT NULL DEFAULT 0,
    vigente_desde DATETIME(6) NOT NULL,
    vigente_hasta DATETIME(6) NULL,
    activa BOOLEAN NOT NULL DEFAULT FALSE,
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_regla_comision_proveedor FOREIGN KEY (proveedor_pago_id) REFERENCES proveedor_pago(id),
    CONSTRAINT fk_regla_comision_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_regla_comision_resolucion (proveedor_pago_id, empresa_id, activa, vigente_desde, vigente_hasta, prioridad)
);

CREATE TABLE evento_webhook_pago (
    id BIGINT NOT NULL AUTO_INCREMENT,
    proveedor_pago_id BIGINT NOT NULL,
    empresa_id BIGINT NULL,
    evento_externo_id VARCHAR(180) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    modo_produccion BOOLEAN NOT NULL DEFAULT FALSE,
    estado_procesamiento VARCHAR(30) NOT NULL DEFAULT 'RECIBIDO',
    intentos INT NOT NULL DEFAULT 0,
    payload MEDIUMTEXT NOT NULL,
    error_ultimo VARCHAR(1000) NULL,
    recibido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    procesado_en DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evento_webhook_proveedor FOREIGN KEY (proveedor_pago_id) REFERENCES proveedor_pago(id),
    CONSTRAINT fk_evento_webhook_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT uk_evento_webhook_proveedor_externo UNIQUE (proveedor_pago_id, evento_externo_id),
    INDEX idx_evento_webhook_pendiente (estado_procesamiento, recibido_en)
);
