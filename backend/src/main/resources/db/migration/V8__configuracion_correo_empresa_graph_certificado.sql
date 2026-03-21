ALTER TABLE configuracion_correo_empresa
    ADD COLUMN graph_certificate_thumbprint VARCHAR(100) NULL AFTER graph_user_id,
    ADD COLUMN graph_private_key_pem TEXT NULL AFTER graph_certificate_thumbprint;
