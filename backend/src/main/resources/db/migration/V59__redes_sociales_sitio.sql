ALTER TABLE empresa_sitio_config
    ADD COLUMN x_url VARCHAR(255) NULL AFTER facebook_url,
    ADD COLUMN tiktok_url VARCHAR(255) NULL AFTER x_url,
    ADD COLUMN whatsapp_habilitado BOOLEAN NOT NULL DEFAULT TRUE AFTER tiktok_url,
    ADD COLUMN instagram_habilitado BOOLEAN NOT NULL DEFAULT TRUE AFTER whatsapp_habilitado,
    ADD COLUMN facebook_habilitado BOOLEAN NOT NULL DEFAULT TRUE AFTER instagram_habilitado,
    ADD COLUMN x_habilitado BOOLEAN NOT NULL DEFAULT TRUE AFTER facebook_habilitado,
    ADD COLUMN tiktok_habilitado BOOLEAN NOT NULL DEFAULT TRUE AFTER x_habilitado;
