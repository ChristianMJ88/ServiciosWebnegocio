ALTER TABLE empresa_sitio_config
    ADD COLUMN menu_estilo VARCHAR(20) NOT NULL DEFAULT 'FLOTANTE' AFTER tema,
    ADD COLUMN menu_mostrar_inicio BOOLEAN NOT NULL DEFAULT TRUE AFTER menu_estilo,
    ADD COLUMN menu_mostrar_servicios BOOLEAN NOT NULL DEFAULT TRUE AFTER menu_mostrar_inicio,
    ADD COLUMN menu_mostrar_contacto BOOLEAN NOT NULL DEFAULT TRUE AFTER menu_mostrar_servicios,
    ADD COLUMN menu_mostrar_registro BOOLEAN NOT NULL DEFAULT TRUE AFTER menu_mostrar_contacto,
    ADD COLUMN menu_mostrar_descripcion BOOLEAN NOT NULL DEFAULT TRUE AFTER menu_mostrar_registro,
    ADD COLUMN menu_texto_cta VARCHAR(40) NULL AFTER menu_mostrar_descripcion,
    ADD COLUMN menu_accion_cta VARCHAR(20) NOT NULL DEFAULT 'AGENDAR' AFTER menu_texto_cta,
    ADD COLUMN hero_alineacion VARCHAR(20) NOT NULL DEFAULT 'IZQUIERDA' AFTER hero_etiqueta,
    ADD COLUMN hero_overlay VARCHAR(20) NOT NULL DEFAULT 'MEDIO' AFTER hero_alineacion,
    ADD COLUMN hero_texto_cta VARCHAR(40) NULL AFTER hero_overlay,
    ADD COLUMN hero_accion_cta VARCHAR(20) NOT NULL DEFAULT 'AGENDAR' AFTER hero_texto_cta,
    ADD COLUMN hero_mostrar_whatsapp BOOLEAN NOT NULL DEFAULT TRUE AFTER hero_accion_cta,
    ADD COLUMN inicio_mostrar_contacto BOOLEAN NOT NULL DEFAULT TRUE AFTER inicio_descripcion_contacto,
    ADD COLUMN inicio_mostrar_servicios BOOLEAN NOT NULL DEFAULT TRUE AFTER inicio_mostrar_contacto,
    ADD COLUMN inicio_orden_secciones VARCHAR(30) NOT NULL DEFAULT 'CONTACTO_SERVICIOS' AFTER inicio_mostrar_servicios;

UPDATE empresa_sitio_config
SET tema = 'BASE'
WHERE tema IS NULL OR tema NOT IN ('BASE', 'MINIMAL', 'ELEGANTE', 'VIBRANTE');
