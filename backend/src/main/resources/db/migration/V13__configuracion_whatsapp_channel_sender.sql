ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN channel_sender_sid VARCHAR(80) NULL AFTER messaging_service_sid;
