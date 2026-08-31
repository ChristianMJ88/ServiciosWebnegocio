ALTER TABLE usuario
    ADD COLUMN intentos_login_fallidos INT NOT NULL DEFAULT 0,
    ADD COLUMN login_bloqueado_hasta DATETIME(6) NULL;
