ALTER TABLE empresa_sitio_config
    ADD COLUMN hero_etiqueta VARCHAR(80) NULL AFTER hero_imagen_url,
    ADD COLUMN inicio_titulo_servicios VARCHAR(180) NULL AFTER hero_etiqueta,
    ADD COLUMN inicio_descripcion_servicios VARCHAR(500) NULL AFTER inicio_titulo_servicios,
    ADD COLUMN inicio_titulo_contacto VARCHAR(180) NULL AFTER inicio_descripcion_servicios,
    ADD COLUMN inicio_descripcion_contacto VARCHAR(500) NULL AFTER inicio_titulo_contacto;
