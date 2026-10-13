ALTER TABLE empresa_sitio_promocion ADD COLUMN eliminada_en DATETIME NULL;
ALTER TABLE cita
  ADD COLUMN precio_lista DECIMAL(10,2) NULL,
  ADD COLUMN descuento DECIMAL(10,2) NOT NULL DEFAULT 0,
  ADD COLUMN promocion_id BIGINT NULL,
  ADD COLUMN promocion_snapshot_json JSON NULL,
  ADD CONSTRAINT fk_cita_promocion FOREIGN KEY (promocion_id) REFERENCES empresa_sitio_promocion(id),
  ADD INDEX idx_cita_promocion (empresa_id, promocion_id);
UPDATE cita SET precio_lista = precio WHERE precio_lista IS NULL;
