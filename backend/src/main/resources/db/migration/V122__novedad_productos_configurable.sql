ALTER TABLE producto ADD COLUMN nuevo_desde DATE NULL, ADD COLUMN nuevo_hasta DATE NULL;
-- Cero desactiva la duración automática hasta que el negocio la configure.
ALTER TABLE producto_tienda_config ADD COLUMN dias_nuevo INT NOT NULL DEFAULT 0;
