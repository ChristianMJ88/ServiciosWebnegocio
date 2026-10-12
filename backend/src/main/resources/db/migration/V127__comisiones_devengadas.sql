ALTER TABLE venta_detalle ADD COLUMN prestador_responsable_id BIGINT NULL;
CREATE TABLE comision_devengada (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 empresa_id BIGINT NOT NULL, venta_id BIGINT NOT NULL, detalle_id BIGINT NOT NULL,
 regla_id BIGINT NOT NULL, usuario_id BIGINT NOT NULL, moneda VARCHAR(10) NOT NULL,
 base_calculo VARCHAR(30) NOT NULL, tipo_calculo VARCHAR(20) NOT NULL,
 valor DECIMAL(12,4) NOT NULL, base_importe DECIMAL(14,2) NULL, importe DECIMAL(14,2) NULL,
 creada_en DATETIME NOT NULL,
 UNIQUE KEY uk_comision_devengada (empresa_id, venta_id, detalle_id, usuario_id),
 INDEX idx_comision_persona (empresa_id, usuario_id, creada_en),
 FOREIGN KEY (venta_id) REFERENCES venta(id), FOREIGN KEY (regla_id) REFERENCES comision_regla(id)
);
