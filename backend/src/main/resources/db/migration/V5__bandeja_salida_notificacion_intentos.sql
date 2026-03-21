ALTER TABLE bandeja_salida_notificacion
    ADD COLUMN intentos INT NOT NULL DEFAULT 0 AFTER mensaje_error;
