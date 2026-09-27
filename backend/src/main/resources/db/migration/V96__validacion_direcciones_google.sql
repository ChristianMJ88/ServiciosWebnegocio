ALTER TABLE sucursal_envio_origen
    ADD COLUMN direccion_formateada VARCHAR(500) NULL AFTER referencias,
    ADD COLUMN direccion_validacion_estado VARCHAR(30) NOT NULL DEFAULT 'NO_CONFIGURADA' AFTER direccion_formateada,
    ADD COLUMN google_place_id VARCHAR(255) NULL AFTER direccion_validacion_estado,
    ADD COLUMN latitud DECIMAL(10,7) NULL AFTER google_place_id,
    ADD COLUMN longitud DECIMAL(10,7) NULL AFTER latitud,
    ADD COLUMN direccion_validada_en DATETIME NULL AFTER longitud;

ALTER TABLE checkout_orden
    ADD COLUMN codigo_pais VARCHAR(2) NULL AFTER codigo_postal,
    ADD COLUMN direccion_formateada VARCHAR(500) NULL AFTER referencias_direccion,
    ADD COLUMN direccion_validacion_estado VARCHAR(30) NOT NULL DEFAULT 'NO_APLICA' AFTER direccion_formateada,
    ADD COLUMN google_place_id VARCHAR(255) NULL AFTER direccion_validacion_estado,
    ADD COLUMN latitud DECIMAL(10,7) NULL AFTER google_place_id,
    ADD COLUMN longitud DECIMAL(10,7) NULL AFTER latitud,
    ADD COLUMN direccion_validada_en DATETIME NULL AFTER longitud;
