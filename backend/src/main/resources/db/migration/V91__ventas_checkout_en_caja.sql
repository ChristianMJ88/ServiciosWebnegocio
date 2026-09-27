ALTER TABLE venta
    MODIFY caja_sesion_id BIGINT NULL,
    MODIFY creada_por_usuario_id BIGINT NULL,
    ADD COLUMN canal VARCHAR(20) NOT NULL DEFAULT 'CAJA' AFTER estado,
    ADD COLUMN checkout_orden_id BIGINT NULL AFTER cita_id,
    ADD CONSTRAINT fk_venta_checkout_orden FOREIGN KEY (checkout_orden_id) REFERENCES checkout_orden(id),
    ADD CONSTRAINT uk_venta_checkout_orden UNIQUE (empresa_id, checkout_orden_id),
    ADD INDEX idx_venta_canal_fecha (empresa_id, canal, creada_en);

ALTER TABLE pago_venta
    MODIFY caja_sesion_id BIGINT NULL,
    MODIFY registrado_por_usuario_id BIGINT NULL,
    ADD COLUMN intento_pago_checkout_id BIGINT NULL AFTER venta_id,
    ADD CONSTRAINT fk_pago_venta_intento_checkout FOREIGN KEY (intento_pago_checkout_id) REFERENCES intento_pago_checkout(id),
    ADD CONSTRAINT uk_pago_venta_intento_checkout UNIQUE (empresa_id, intento_pago_checkout_id);
