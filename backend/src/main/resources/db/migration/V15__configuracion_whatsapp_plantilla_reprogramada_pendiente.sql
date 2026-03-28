ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN plantilla_reprogramada_pendiente_sid VARCHAR(80) NULL
    AFTER plantilla_solicitud_confirmacion_sid;
