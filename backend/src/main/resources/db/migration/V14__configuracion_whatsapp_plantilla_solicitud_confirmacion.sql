ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN plantilla_solicitud_confirmacion_sid VARCHAR(80) NULL
    AFTER status_callback_url;
