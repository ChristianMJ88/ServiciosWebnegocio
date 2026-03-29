CREATE TABLE auditoria_rol_empresa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    rol_empresa_id BIGINT NULL,
    usuario_actor_id BIGINT NULL,
    actor_correo VARCHAR(150) NULL,
    rol_codigo VARCHAR(80) NULL,
    rol_nombre VARCHAR(120) NULL,
    accion VARCHAR(50) NOT NULL,
    resumen VARCHAR(255) NOT NULL,
    detalle_antes_json LONGTEXT NULL,
    detalle_despues_json LONGTEXT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auditoria_rol_empresa_empresa_fecha (empresa_id, creado_en DESC),
    INDEX idx_auditoria_rol_empresa_rol (rol_empresa_id)
);
