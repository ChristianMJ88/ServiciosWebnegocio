ALTER TABLE configuracion_correo_empresa
    ADD COLUMN graph_oauth_refresh_token TEXT NULL,
    ADD COLUMN graph_oauth_scopes VARCHAR(500) NULL,
    ADD COLUMN graph_oauth_conectado_en DATETIME(6) NULL;

