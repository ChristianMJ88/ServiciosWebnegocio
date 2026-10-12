ALTER TABLE sucursal ADD COLUMN zona_precio_id BIGINT NULL;
ALTER TABLE sucursal ADD CONSTRAINT fk_sucursal_zona_precio FOREIGN KEY (zona_precio_id) REFERENCES producto_zona_precio(id);
