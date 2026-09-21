ALTER TABLE empresa_sitio_resena
    ADD COLUMN media_id BIGINT NULL AFTER comentario,
    ADD COLUMN foto_url VARCHAR(1000) NULL AFTER media_id,
    ADD CONSTRAINT fk_sitio_resena_media FOREIGN KEY (media_id) REFERENCES empresa_archivo_media(id) ON DELETE SET NULL;
