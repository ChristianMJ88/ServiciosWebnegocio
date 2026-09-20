ALTER TABLE empresa_sitio_config
    ADD COLUMN hero_tipo_medio VARCHAR(20) NOT NULL DEFAULT 'IMAGEN' AFTER hero_imagen_url,
    ADD COLUMN hero_video_url VARCHAR(500) NULL AFTER hero_tipo_medio,
    ADD COLUMN hero_altura VARCHAR(20) NOT NULL DEFAULT 'PANTALLA' AFTER hero_video_url,
    ADD COLUMN hero_enlace_cta VARCHAR(500) NULL AFTER hero_accion_cta,
    ADD COLUMN hero_texto_secundario VARCHAR(40) NULL AFTER hero_enlace_cta,
    ADD COLUMN hero_accion_secundaria VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP' AFTER hero_texto_secundario,
    ADD COLUMN hero_enlace_secundario VARCHAR(500) NULL AFTER hero_accion_secundaria,
    ADD COLUMN hero_mostrar_secundario BOOLEAN NOT NULL DEFAULT TRUE AFTER hero_enlace_secundario;
