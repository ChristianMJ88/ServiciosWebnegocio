SET @sitio_fuente_titulos_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'empresa_sitio_config'
      AND column_name = 'fuente_titulos'
);

SET @sitio_fuente_titulos_sql := IF(
    @sitio_fuente_titulos_exists = 0,
    'ALTER TABLE empresa_sitio_config ADD COLUMN fuente_titulos VARCHAR(40) NULL AFTER color_secundario',
    'SELECT 1'
);

PREPARE sitio_fuente_titulos_stmt FROM @sitio_fuente_titulos_sql;
EXECUTE sitio_fuente_titulos_stmt;
DEALLOCATE PREPARE sitio_fuente_titulos_stmt;

SET @sitio_fuente_cuerpo_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'empresa_sitio_config'
      AND column_name = 'fuente_cuerpo'
);

SET @sitio_fuente_cuerpo_sql := IF(
    @sitio_fuente_cuerpo_exists = 0,
    'ALTER TABLE empresa_sitio_config ADD COLUMN fuente_cuerpo VARCHAR(40) NULL AFTER fuente_titulos',
    'SELECT 1'
);

PREPARE sitio_fuente_cuerpo_stmt FROM @sitio_fuente_cuerpo_sql;
EXECUTE sitio_fuente_cuerpo_stmt;
DEALLOCATE PREPARE sitio_fuente_cuerpo_stmt;

UPDATE empresa_sitio_config
SET fuente_titulos = COALESCE(fuente_titulos, 'JAKARTA'),
    fuente_cuerpo = COALESCE(fuente_cuerpo, 'INTER');
