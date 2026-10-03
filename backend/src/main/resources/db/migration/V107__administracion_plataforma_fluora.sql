CREATE TABLE plataforma_permiso (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(140) NOT NULL,
    descripcion VARCHAR(255) NULL
);

CREATE TABLE plataforma_rol (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE plataforma_rol_permiso (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_plataforma_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES plataforma_rol(id),
    CONSTRAINT fk_plataforma_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES plataforma_permiso(id)
);

CREATE TABLE plataforma_usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    correo VARCHAR(150) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_acceso_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE plataforma_usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_plataforma_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES plataforma_usuario(id),
    CONSTRAINT fk_plataforma_usuario_rol_rol FOREIGN KEY (rol_id) REFERENCES plataforma_rol(id)
);

CREATE TABLE plataforma_auditoria (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    accion VARCHAR(100) NOT NULL,
    recurso VARCHAR(100) NOT NULL,
    recurso_id VARCHAR(120) NULL,
    detalle JSON NULL,
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_plataforma_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES plataforma_usuario(id),
    INDEX idx_plataforma_auditoria_fecha (creada_en),
    INDEX idx_plataforma_auditoria_recurso (recurso, recurso_id)
);

INSERT INTO plataforma_permiso (codigo, nombre, descripcion) VALUES
    ('PLATAFORMA_ACCESO', 'Acceder a Fluora Plataforma', 'Permite iniciar y utilizar el panel de operación SaaS'),
    ('PLATAFORMA_TENANTS_VER', 'Consultar negocios', 'Permite consultar el directorio y estado de los tenants'),
    ('PLATAFORMA_FINANZAS_VER', 'Consultar finanzas', 'Permite consultar indicadores financieros consolidados'),
    ('PLATAFORMA_COMISIONES_GESTIONAR', 'Gestionar comisiones Fluora', 'Permite configurar reglas de comisión de plataforma'),
    ('PLATAFORMA_SUSCRIPCIONES_GESTIONAR', 'Gestionar suscripciones', 'Permite administrar planes y suscripciones SaaS'),
    ('PLATAFORMA_USUARIOS_GESTIONAR', 'Gestionar equipo Fluora', 'Permite administrar usuarios y roles de plataforma'),
    ('PLATAFORMA_AUDITORIA_VER', 'Consultar auditoría', 'Permite consultar las acciones realizadas en plataforma')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT INTO plataforma_rol (codigo, nombre, descripcion, activo) VALUES
    ('ADMINISTRADOR_PLATAFORMA', 'Administrador de plataforma', 'Administración integral de Fluora SaaS', TRUE),
    ('OPERACIONES_PLATAFORMA', 'Operaciones de plataforma', 'Consulta y atención operativa de tenants', TRUE),
    ('FINANZAS_PLATAFORMA', 'Finanzas de plataforma', 'Consulta financiera y gestión de suscripciones', TRUE)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion), activo = VALUES(activo);

INSERT IGNORE INTO plataforma_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM plataforma_rol r CROSS JOIN plataforma_permiso p
WHERE r.codigo = 'ADMINISTRADOR_PLATAFORMA';

INSERT IGNORE INTO plataforma_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM plataforma_rol r JOIN plataforma_permiso p
WHERE r.codigo = 'OPERACIONES_PLATAFORMA'
  AND p.codigo IN ('PLATAFORMA_ACCESO', 'PLATAFORMA_TENANTS_VER', 'PLATAFORMA_AUDITORIA_VER');

INSERT IGNORE INTO plataforma_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM plataforma_rol r JOIN plataforma_permiso p
WHERE r.codigo = 'FINANZAS_PLATAFORMA'
  AND p.codigo IN ('PLATAFORMA_ACCESO', 'PLATAFORMA_FINANZAS_VER', 'PLATAFORMA_COMISIONES_GESTIONAR', 'PLATAFORMA_SUSCRIPCIONES_GESTIONAR');
