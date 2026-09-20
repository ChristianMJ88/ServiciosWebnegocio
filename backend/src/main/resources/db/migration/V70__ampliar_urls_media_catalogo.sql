ALTER TABLE empresa_sitio_config
    MODIFY COLUMN logo_url VARCHAR(500) NULL;

ALTER TABLE grupo_servicio
    MODIFY COLUMN imagen_url VARCHAR(500) NULL;

ALTER TABLE servicio
    MODIFY COLUMN imagen_url VARCHAR(500) NULL;
