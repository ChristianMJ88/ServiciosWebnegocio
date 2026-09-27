ALTER TABLE empresa_envio_config
    ADD COLUMN modo_credencial VARCHAR(30) NOT NULL DEFAULT 'CUENTA_PROPIA' AFTER ambiente;
