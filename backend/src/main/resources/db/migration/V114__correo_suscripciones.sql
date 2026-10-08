ALTER TABLE plataforma_suscripcion ADD COLUMN correo_facturacion VARCHAR(150) NULL;
ALTER TABLE plataforma_suscripcion ADD COLUMN aviso_vencimiento_en DATETIME(6) NULL;
ALTER TABLE bandeja_salida_notificacion ADD COLUMN clave_deduplicacion VARCHAR(200) NULL;
CREATE UNIQUE INDEX uk_outbox_clave_deduplicacion ON bandeja_salida_notificacion(clave_deduplicacion);
