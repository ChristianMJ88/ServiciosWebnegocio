ALTER TABLE empresa_sitio_promocion
    ADD COLUMN media_id BIGINT NULL AFTER descripcion,
    ADD CONSTRAINT fk_sitio_promocion_media FOREIGN KEY (media_id) REFERENCES empresa_archivo_media(id) ON DELETE SET NULL,
    ADD INDEX idx_sitio_promocion_media (media_id);
