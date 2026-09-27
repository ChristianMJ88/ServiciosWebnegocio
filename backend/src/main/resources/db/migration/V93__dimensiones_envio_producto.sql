ALTER TABLE producto
    ADD COLUMN peso_kg DECIMAL(10,3) NULL AFTER costo,
    ADD COLUMN largo_cm DECIMAL(10,2) NULL AFTER peso_kg,
    ADD COLUMN ancho_cm DECIMAL(10,2) NULL AFTER largo_cm,
    ADD COLUMN alto_cm DECIMAL(10,2) NULL AFTER ancho_cm;
