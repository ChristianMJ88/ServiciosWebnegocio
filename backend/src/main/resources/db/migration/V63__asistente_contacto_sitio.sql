ALTER TABLE empresa_sitio_config
    ADD COLUMN whatsapp_mensaje VARCHAR(255) NULL AFTER whatsapp,
    ADD COLUMN chat_habilitado BOOLEAN NOT NULL DEFAULT TRUE AFTER tiktok_habilitado,
    ADD COLUMN chat_titulo VARCHAR(80) NULL AFTER chat_habilitado,
    ADD COLUMN chat_saludo VARCHAR(255) NULL AFTER chat_titulo;
