ALTER TABLE configuracion_correo_empresa
    ADD COLUMN proveedor VARCHAR(20) NULL AFTER habilitado,
    ADD COLUMN graph_tenant_id VARCHAR(100) NULL AFTER smtp_starttls,
    ADD COLUMN graph_client_id VARCHAR(100) NULL AFTER graph_tenant_id,
    ADD COLUMN graph_client_secret VARCHAR(255) NULL AFTER graph_client_id,
    ADD COLUMN graph_user_id VARCHAR(150) NULL AFTER graph_client_secret;
