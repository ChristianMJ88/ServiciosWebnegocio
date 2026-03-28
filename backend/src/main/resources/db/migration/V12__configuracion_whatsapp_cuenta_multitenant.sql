ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN tipo_cuenta_twilio VARCHAR(30) NULL AFTER auth_token,
    ADD COLUMN subaccount_sid VARCHAR(80) NULL AFTER tipo_cuenta_twilio;
