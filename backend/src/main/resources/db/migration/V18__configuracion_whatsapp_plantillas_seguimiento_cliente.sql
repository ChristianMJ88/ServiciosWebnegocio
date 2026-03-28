ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN plantilla_liberada_sin_confirmacion_sid VARCHAR(80) NULL AFTER plantilla_cancelacion_sid,
    ADD COLUMN plantilla_gracias_visita_sid VARCHAR(80) NULL AFTER plantilla_liberada_sin_confirmacion_sid,
    ADD COLUMN plantilla_recordatorio_regreso_sid VARCHAR(80) NULL AFTER plantilla_gracias_visita_sid;
