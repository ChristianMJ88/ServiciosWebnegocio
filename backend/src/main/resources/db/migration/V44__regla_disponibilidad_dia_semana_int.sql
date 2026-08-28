SET @regla_dia_semana_necesita_int := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'regla_disponibilidad'
      AND column_name = 'dia_semana'
      AND data_type <> 'int'
);

SET @regla_dia_semana_sql := IF(
    @regla_dia_semana_necesita_int > 0,
    'ALTER TABLE regla_disponibilidad MODIFY COLUMN dia_semana INT NOT NULL',
    'SELECT 1'
);

PREPARE regla_dia_semana_stmt FROM @regla_dia_semana_sql;
EXECUTE regla_dia_semana_stmt;
DEALLOCATE PREPARE regla_dia_semana_stmt;
