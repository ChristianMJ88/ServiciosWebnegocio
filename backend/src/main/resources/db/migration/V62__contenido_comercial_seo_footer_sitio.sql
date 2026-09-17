ALTER TABLE empresa_sitio_config
    ADD COLUMN inicio_etiqueta_servicios VARCHAR(40) NULL AFTER inicio_descripcion_servicios,
    ADD COLUMN inicio_texto_ver_servicios VARCHAR(40) NULL AFTER inicio_etiqueta_servicios,
    ADD COLUMN inicio_texto_reservar VARCHAR(40) NULL AFTER inicio_texto_ver_servicios,
    ADD COLUMN inicio_etiqueta_contacto VARCHAR(40) NULL AFTER inicio_descripcion_contacto,
    ADD COLUMN inicio_texto_agenda VARCHAR(40) NULL AFTER inicio_etiqueta_contacto,
    ADD COLUMN inicio_texto_whatsapp VARCHAR(40) NULL AFTER inicio_texto_agenda,
    ADD COLUMN seo_titulo VARCHAR(70) NULL AFTER inicio_orden_secciones,
    ADD COLUMN seo_descripcion VARCHAR(170) NULL AFTER seo_titulo,
    ADD COLUMN footer_texto VARCHAR(300) NULL AFTER menu_accion_cta,
    ADD COLUMN footer_mostrar_navegacion BOOLEAN NOT NULL DEFAULT TRUE AFTER footer_texto,
    ADD COLUMN footer_mostrar_contacto BOOLEAN NOT NULL DEFAULT TRUE AFTER footer_mostrar_navegacion,
    ADD COLUMN footer_mostrar_redes BOOLEAN NOT NULL DEFAULT TRUE AFTER footer_mostrar_contacto;
