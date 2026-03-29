CREATE TABLE IF NOT EXISTS rol_empresa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    rol_base_id BIGINT NULL,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    editable BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rol_empresa_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_rol_empresa_rol_base FOREIGN KEY (rol_base_id) REFERENCES rol(id),
    CONSTRAINT uk_rol_empresa_codigo UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS rol_empresa_permiso (
    rol_empresa_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_empresa_id, permiso_id),
    CONSTRAINT fk_rol_empresa_permiso_rol FOREIGN KEY (rol_empresa_id) REFERENCES rol_empresa(id),
    CONSTRAINT fk_rol_empresa_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id)
);

CREATE TABLE IF NOT EXISTS usuario_rol_empresa (
    usuario_id BIGINT NOT NULL,
    rol_empresa_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_empresa_id),
    CONSTRAINT fk_usuario_rol_empresa_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_usuario_rol_empresa_rol FOREIGN KEY (rol_empresa_id) REFERENCES rol_empresa(id)
);

INSERT IGNORE INTO rol_empresa (empresa_id, rol_base_id, codigo, nombre, descripcion, activo, editable)
SELECT
    e.id,
    r.id,
    r.codigo,
    CASE r.codigo
        WHEN 'ADMIN' THEN 'Administrador'
        WHEN 'RECEPCIONISTA' THEN 'Recepción'
        WHEN 'CAJERO' THEN 'Caja'
        WHEN 'STAFF' THEN 'Staff'
        WHEN 'CLIENTE' THEN 'Cliente'
        ELSE r.codigo
    END,
    CONCAT('Rol inicial migrado desde el catálogo global: ', r.codigo),
    TRUE,
    FALSE
FROM empresa e
CROSS JOIN rol r;

INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id, permiso_id)
SELECT re.id, rp.permiso_id
FROM rol_empresa re
JOIN rol_permiso rp ON rp.rol_id = re.rol_base_id;

INSERT IGNORE INTO usuario_rol_empresa (usuario_id, rol_empresa_id)
SELECT ur.usuario_id, re.id
FROM usuario_rol ur
JOIN rol r ON r.id = ur.rol_id
JOIN rol_empresa re ON re.empresa_id = ur.empresa_id AND re.codigo = r.codigo;
