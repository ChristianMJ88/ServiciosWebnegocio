-- V88 y V119 crearon CHAR; los mapeos de la aplicación usan VARCHAR.
-- Recuperar charset/collation de cada columna conserva también esquemas locales
-- con una collation diferente a la predeterminada de la tabla o la base.
SET @v143_ddl = (
    SELECT CASE
        WHEN CHARACTER_MAXIMUM_LENGTH = 36 AND IS_NULLABLE = 'NO'
             AND COLUMN_DEFAULT IS NULL AND COLUMN_COMMENT = '' AND EXTRA = '' THEN
            CASE DATA_TYPE
                WHEN 'char' THEN CONCAT('ALTER TABLE checkout_orden MODIFY COLUMN public_id VARCHAR(36) CHARACTER SET ',
                                       CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME, ' NOT NULL')
                WHEN 'varchar' THEN 'DO 0'
            END
    END
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkout_orden' AND COLUMN_NAME = 'public_id'
);
PREPARE v143_stmt FROM @v143_ddl;
EXECUTE v143_stmt;
DEALLOCATE PREPARE v143_stmt;

SET @v143_ddl = (
    SELECT CASE
        WHEN CHARACTER_MAXIMUM_LENGTH = 2 AND IS_NULLABLE = 'YES'
             AND COLUMN_DEFAULT IS NULL AND COLUMN_COMMENT = '' AND EXTRA = '' THEN
            CASE DATA_TYPE
                WHEN 'char' THEN CONCAT('ALTER TABLE sucursal MODIFY COLUMN pais_codigo VARCHAR(2) CHARACTER SET ',
                                       CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME, ' NULL')
                WHEN 'varchar' THEN 'DO 0'
            END
    END
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sucursal' AND COLUMN_NAME = 'pais_codigo'
);
PREPARE v143_stmt FROM @v143_ddl;
EXECUTE v143_stmt;
DEALLOCATE PREPARE v143_stmt;
SET @v143_ddl = NULL;
