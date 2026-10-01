ALTER TABLE empresa_metodo_pago_checkout
    DROP CHECK chk_empresa_metodo_pago_vigencia;

UPDATE empresa_metodo_pago_checkout
SET vigencia_horas = CEIL(vigencia_horas / 24);

ALTER TABLE empresa_metodo_pago_checkout
    CHANGE COLUMN vigencia_horas vigencia_dias INT NOT NULL DEFAULT 3,
    ADD CONSTRAINT chk_empresa_metodo_pago_vigencia CHECK (vigencia_dias BETWEEN 1 AND 30);
