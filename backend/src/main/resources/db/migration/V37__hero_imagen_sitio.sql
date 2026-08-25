SET @sitio_hero_imagen_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'empresa_sitio_config'
      AND column_name = 'hero_imagen_url'
);

SET @sitio_hero_imagen_sql := IF(
    @sitio_hero_imagen_exists = 0,
    'ALTER TABLE empresa_sitio_config ADD COLUMN hero_imagen_url VARCHAR(500) NULL AFTER hero_subtitulo',
    'SELECT 1'
);

PREPARE sitio_hero_imagen_stmt FROM @sitio_hero_imagen_sql;
EXECUTE sitio_hero_imagen_stmt;
DEALLOCATE PREPARE sitio_hero_imagen_stmt;

UPDATE empresa_sitio_config
SET hero_imagen_url = COALESCE(hero_imagen_url, '/tenant-hero-demo.png')
WHERE empresa_id = 1;
