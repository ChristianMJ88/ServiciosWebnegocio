ALTER TABLE whatsapp_plantilla_empresa
    ADD CONSTRAINT fk_whatsapp_plantilla_empresa_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id);

ALTER TABLE whatsapp_conversacion
    ADD CONSTRAINT fk_whatsapp_conversacion_grupo
        FOREIGN KEY (grupo_id) REFERENCES grupo_servicio(id),
    ADD CONSTRAINT fk_whatsapp_conversacion_subgrupo
        FOREIGN KEY (subgrupo_id) REFERENCES subgrupo_servicio(id),
    ADD INDEX idx_whatsapp_conversacion_grupo (grupo_id),
    ADD INDEX idx_whatsapp_conversacion_subgrupo (subgrupo_id);
