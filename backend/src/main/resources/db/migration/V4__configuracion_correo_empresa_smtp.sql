ALTER TABLE configuracion_correo_empresa
    ADD COLUMN smtp_host VARCHAR(150) NULL AFTER responder_a,
    ADD COLUMN smtp_port INT NULL AFTER smtp_host,
    ADD COLUMN smtp_username VARCHAR(150) NULL AFTER smtp_port,
    ADD COLUMN smtp_password VARCHAR(255) NULL AFTER smtp_username,
    ADD COLUMN smtp_auth BOOLEAN NULL AFTER smtp_password,
    ADD COLUMN smtp_starttls BOOLEAN NULL AFTER smtp_auth;
