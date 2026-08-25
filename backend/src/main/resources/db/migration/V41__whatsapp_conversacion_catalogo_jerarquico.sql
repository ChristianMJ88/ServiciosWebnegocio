ALTER TABLE whatsapp_conversacion
    ADD COLUMN grupo_id BIGINT NULL AFTER sucursal_id,
    ADD COLUMN subgrupo_id BIGINT NULL AFTER grupo_id,
    ADD CONSTRAINT fk_whatsapp_conversacion_grupo
        FOREIGN KEY (grupo_id) REFERENCES grupo_servicio(id),
    ADD CONSTRAINT fk_whatsapp_conversacion_subgrupo
        FOREIGN KEY (subgrupo_id) REFERENCES subgrupo_servicio(id),
    ADD INDEX idx_whatsapp_conversacion_grupo (grupo_id),
    ADD INDEX idx_whatsapp_conversacion_subgrupo (subgrupo_id);
