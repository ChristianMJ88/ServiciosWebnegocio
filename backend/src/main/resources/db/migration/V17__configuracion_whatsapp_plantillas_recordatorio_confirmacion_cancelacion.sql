ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN plantilla_recordatorio_confirmacion_sid VARCHAR(80) NULL
    AFTER plantilla_reprogramada_pendiente_sid,
    ADD COLUMN plantilla_cancelacion_sid VARCHAR(80) NULL
    AFTER plantilla_recordatorio_sid;
