ALTER TABLE cita
    ADD COLUMN check_in_en DATETIME NULL AFTER reprogramada_desde_id,
    ADD COLUMN check_in_por_usuario_id BIGINT NULL AFTER check_in_en;
