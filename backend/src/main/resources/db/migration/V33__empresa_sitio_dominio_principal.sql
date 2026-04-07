SET @dominio_principal_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'empresa_sitio_config'
      AND column_name = 'dominio_principal'
);

SET @dominio_principal_column_sql := IF(
    @dominio_principal_column_exists = 0,
    'ALTER TABLE empresa_sitio_config ADD COLUMN dominio_principal VARCHAR(255) NULL AFTER nombre_comercial',
    'SELECT 1'
);

PREPARE dominio_principal_column_stmt FROM @dominio_principal_column_sql;
EXECUTE dominio_principal_column_stmt;
DEALLOCATE PREPARE dominio_principal_column_stmt;

SET @dominio_principal_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'empresa_sitio_config'
      AND index_name = 'uk_empresa_sitio_config_dominio_principal'
);

SET @dominio_principal_index_sql := IF(
    @dominio_principal_index_exists = 0,
    'CREATE UNIQUE INDEX uk_empresa_sitio_config_dominio_principal ON empresa_sitio_config (dominio_principal)',
    'SELECT 1'
);
