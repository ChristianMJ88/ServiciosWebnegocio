ALTER TABLE configuracion_correo_empresa
    ADD COLUMN gmail_user_id VARCHAR(150) NULL,
    ADD COLUMN gmail_oauth_refresh_token TEXT NULL,
    ADD COLUMN gmail_oauth_scopes VARCHAR(500) NULL,
    ADD COLUMN gmail_oauth_conectado_en DATETIME NULL;
