ALTER TABLE empresa_checkout_config
    ADD COLUMN mostrar_estado_pago BOOLEAN NOT NULL DEFAULT TRUE AFTER aceptar_efectivo_referenciado;

ALTER TABLE intento_pago_checkout
    ADD COLUMN referencia_pago_externa VARCHAR(180) NULL AFTER referencia_externa,
    ADD INDEX idx_intento_pago_referencia_pago (proveedor_pago_id, referencia_pago_externa);
