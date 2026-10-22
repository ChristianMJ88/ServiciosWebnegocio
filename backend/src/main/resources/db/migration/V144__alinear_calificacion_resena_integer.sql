-- V76 creó TINYINT; el mapeo Java Integer requiere INT al validar el esquema.
-- No cambia la política de calificación ni las filas. Acepta el destino exacto
-- para bases que ya fueron alineadas durante el desarrollo con Hibernate.
SET @v144_ddl = (
    SELECT CASE
        WHEN IS_NULLABLE = 'NO' AND COLUMN_DEFAULT IS NULL
             AND COLUMN_COMMENT = '' AND EXTRA = ''
             AND COLUMN_TYPE IN ('tinyint', 'tinyint(4)', 'int', 'int(11)') THEN
            CASE DATA_TYPE
                WHEN 'tinyint' THEN 'ALTER TABLE empresa_sitio_resena MODIFY COLUMN calificacion INT NOT NULL'
                WHEN 'int' THEN 'DO 0'
            END
    END
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'empresa_sitio_resena' AND COLUMN_NAME = 'calificacion'
);
PREPARE v144_stmt FROM @v144_ddl;
EXECUTE v144_stmt;
DEALLOCATE PREPARE v144_stmt;
SET @v144_ddl = NULL;
