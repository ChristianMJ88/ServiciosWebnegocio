ALTER TABLE whatsapp_conversacion
    ADD COLUMN grupo_id BIGINT NULL AFTER sucursal_id,
    ADD COLUMN subgrupo_id BIGINT NULL AFTER grupo_id;
