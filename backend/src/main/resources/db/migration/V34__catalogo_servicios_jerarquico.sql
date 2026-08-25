CREATE TABLE IF NOT EXISTS grupo_servicio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    descripcion VARCHAR(300) NULL,
    imagen_url VARCHAR(255) NULL,
    icono VARCHAR(50) NULL,
    orden_publico INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_grupo_servicio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT uk_grupo_servicio_empresa_slug UNIQUE (empresa_id, slug)
);

CREATE TABLE IF NOT EXISTS subgrupo_servicio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    grupo_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    descripcion VARCHAR(300) NULL,
    orden_publico INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_subgrupo_servicio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_subgrupo_servicio_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_servicio(id),
    CONSTRAINT uk_subgrupo_servicio_empresa_grupo_slug UNIQUE (empresa_id, grupo_id, slug)
);

SET @servicio_grupo_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'grupo_id'
);

SET @servicio_grupo_id_sql := IF(
    @servicio_grupo_id_exists = 0,
    'ALTER TABLE servicio ADD COLUMN grupo_id BIGINT NULL AFTER sucursal_id',
    'SELECT 1'
);

PREPARE servicio_grupo_id_stmt FROM @servicio_grupo_id_sql;
EXECUTE servicio_grupo_id_stmt;
DEALLOCATE PREPARE servicio_grupo_id_stmt;

SET @servicio_subgrupo_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'subgrupo_id'
);

SET @servicio_subgrupo_id_sql := IF(
    @servicio_subgrupo_id_exists = 0,
    'ALTER TABLE servicio ADD COLUMN subgrupo_id BIGINT NULL AFTER grupo_id',
    'SELECT 1'
);

PREPARE servicio_subgrupo_id_stmt FROM @servicio_subgrupo_id_sql;
EXECUTE servicio_subgrupo_id_stmt;
DEALLOCATE PREPARE servicio_subgrupo_id_stmt;

SET @servicio_slug_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'slug'
);

SET @servicio_slug_sql := IF(
    @servicio_slug_exists = 0,
    'ALTER TABLE servicio ADD COLUMN slug VARCHAR(140) NULL AFTER nombre',
    'SELECT 1'
);

PREPARE servicio_slug_stmt FROM @servicio_slug_sql;
EXECUTE servicio_slug_stmt;
DEALLOCATE PREPARE servicio_slug_stmt;

SET @servicio_orden_publico_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'orden_publico'
);

SET @servicio_orden_publico_sql := IF(
    @servicio_orden_publico_exists = 0,
    'ALTER TABLE servicio ADD COLUMN orden_publico INT NOT NULL DEFAULT 0 AFTER moneda',
    'SELECT 1'
);

PREPARE servicio_orden_publico_stmt FROM @servicio_orden_publico_sql;
EXECUTE servicio_orden_publico_stmt;
DEALLOCATE PREPARE servicio_orden_publico_stmt;

SET @servicio_visible_publico_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'visible_publico'
);

SET @servicio_visible_publico_sql := IF(
    @servicio_visible_publico_exists = 0,
    'ALTER TABLE servicio ADD COLUMN visible_publico BOOLEAN NOT NULL DEFAULT TRUE AFTER orden_publico',
    'SELECT 1'
);

PREPARE servicio_visible_publico_stmt FROM @servicio_visible_publico_sql;
EXECUTE servicio_visible_publico_stmt;
DEALLOCATE PREPARE servicio_visible_publico_stmt;

SET @servicio_imagen_url_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'imagen_url'
);

SET @servicio_imagen_url_sql := IF(
    @servicio_imagen_url_exists = 0,
    'ALTER TABLE servicio ADD COLUMN imagen_url VARCHAR(255) NULL AFTER descripcion',
    'SELECT 1'
);

PREPARE servicio_imagen_url_stmt FROM @servicio_imagen_url_sql;
EXECUTE servicio_imagen_url_stmt;
DEALLOCATE PREPARE servicio_imagen_url_stmt;

SET @servicio_requiere_anticipo_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'requiere_anticipo'
);

SET @servicio_requiere_anticipo_sql := IF(
    @servicio_requiere_anticipo_exists = 0,
    'ALTER TABLE servicio ADD COLUMN requiere_anticipo BOOLEAN NOT NULL DEFAULT FALSE AFTER visible_publico',
    'SELECT 1'
);

PREPARE servicio_requiere_anticipo_stmt FROM @servicio_requiere_anticipo_sql;
EXECUTE servicio_requiere_anticipo_stmt;
DEALLOCATE PREPARE servicio_requiere_anticipo_stmt;

SET @servicio_anticipo_tipo_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'anticipo_tipo'
);

SET @servicio_anticipo_tipo_sql := IF(
    @servicio_anticipo_tipo_exists = 0,
    'ALTER TABLE servicio ADD COLUMN anticipo_tipo VARCHAR(20) NULL AFTER requiere_anticipo',
    'SELECT 1'
);

PREPARE servicio_anticipo_tipo_stmt FROM @servicio_anticipo_tipo_sql;
EXECUTE servicio_anticipo_tipo_stmt;
DEALLOCATE PREPARE servicio_anticipo_tipo_stmt;

SET @servicio_anticipo_valor_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND column_name = 'anticipo_valor'
);

SET @servicio_anticipo_valor_sql := IF(
    @servicio_anticipo_valor_exists = 0,
    'ALTER TABLE servicio ADD COLUMN anticipo_valor DECIMAL(10,2) NULL AFTER anticipo_tipo',
    'SELECT 1'
);

PREPARE servicio_anticipo_valor_stmt FROM @servicio_anticipo_valor_sql;
EXECUTE servicio_anticipo_valor_stmt;
DEALLOCATE PREPARE servicio_anticipo_valor_stmt;

SET @servicio_fk_grupo_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND constraint_name = 'fk_servicio_grupo'
);

SET @servicio_fk_grupo_sql := IF(
    @servicio_fk_grupo_exists = 0,
    'ALTER TABLE servicio ADD CONSTRAINT fk_servicio_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_servicio(id)',
    'SELECT 1'
);

PREPARE servicio_fk_grupo_stmt FROM @servicio_fk_grupo_sql;
EXECUTE servicio_fk_grupo_stmt;
DEALLOCATE PREPARE servicio_fk_grupo_stmt;

SET @servicio_fk_subgrupo_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'servicio'
      AND constraint_name = 'fk_servicio_subgrupo'
);

SET @servicio_fk_subgrupo_sql := IF(
    @servicio_fk_subgrupo_exists = 0,
    'ALTER TABLE servicio ADD CONSTRAINT fk_servicio_subgrupo FOREIGN KEY (subgrupo_id) REFERENCES subgrupo_servicio(id)',
    'SELECT 1'
);

PREPARE servicio_fk_subgrupo_stmt FROM @servicio_fk_subgrupo_sql;
EXECUTE servicio_fk_subgrupo_stmt;
DEALLOCATE PREPARE servicio_fk_subgrupo_stmt;

CREATE INDEX idx_servicio_empresa_grupo ON servicio (empresa_id, grupo_id);
CREATE INDEX idx_servicio_empresa_subgrupo ON servicio (empresa_id, subgrupo_id);
CREATE INDEX idx_servicio_empresa_visible ON servicio (empresa_id, visible_publico, activo);
CREATE INDEX idx_servicio_sucursal_activo_orden ON servicio (sucursal_id, activo, orden_publico, nombre);

CREATE UNIQUE INDEX uk_servicio_empresa_slug ON servicio (empresa_id, slug);

INSERT INTO grupo_servicio (empresa_id, nombre, slug, descripcion, imagen_url, icono, orden_publico, activo)
SELECT 1, 'Uñas', 'unas', 'Servicios de manicure, gel y diseño.', '/NailArt_logo.jpeg', 'spa', 10, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM grupo_servicio WHERE empresa_id = 1 AND slug = 'unas'
);

INSERT INTO subgrupo_servicio (empresa_id, grupo_id, nombre, slug, descripcion, orden_publico, activo)
SELECT 1, gs.id, 'Manicura', 'manicura', 'Servicios principales de manicure.', 10, TRUE
FROM grupo_servicio gs
WHERE gs.empresa_id = 1
  AND gs.slug = 'unas'
  AND NOT EXISTS (
      SELECT 1
      FROM subgrupo_servicio ss
      WHERE ss.empresa_id = 1
        AND ss.grupo_id = gs.id
        AND ss.slug = 'manicura'
  );

UPDATE servicio s
JOIN grupo_servicio gs ON gs.empresa_id = s.empresa_id AND gs.slug = 'unas'
LEFT JOIN subgrupo_servicio ss ON ss.empresa_id = s.empresa_id AND ss.grupo_id = gs.id AND ss.slug = 'manicura'
SET
    s.grupo_id = COALESCE(s.grupo_id, gs.id),
    s.subgrupo_id = COALESCE(s.subgrupo_id, ss.id),
    s.slug = COALESCE(s.slug, LOWER(REPLACE(REPLACE(TRIM(s.nombre), ' ', '-'), '--', '-'))),
    s.orden_publico = CASE
        WHEN s.orden_publico IS NULL OR s.orden_publico = 0 THEN s.id * 10
        ELSE s.orden_publico
    END,
    s.visible_publico = TRUE
WHERE s.empresa_id = 1;
