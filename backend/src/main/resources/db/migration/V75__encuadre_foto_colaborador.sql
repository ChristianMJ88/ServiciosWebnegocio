ALTER TABLE prestador_servicio
    ADD COLUMN foto_encuadre VARCHAR(20) NOT NULL DEFAULT 'ROSTRO' AFTER foto_url,
    ADD COLUMN foto_zoom DECIMAL(3,2) NOT NULL DEFAULT 1.00 AFTER foto_encuadre;
