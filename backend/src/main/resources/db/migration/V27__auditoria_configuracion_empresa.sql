CREATE TABLE auditoria_configuracion_empresa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    usuario_actor_id BIGINT NULL,
    actor_correo VARCHAR(150) NULL,
    modulo VARCHAR(30) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    resumen VARCHAR(255) NOT NULL,
    detalle_antes_json LONGTEXT NULL,
    detalle_despues_json LONGTEXT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auditoria_config_empresa_fecha (empresa_id, creado_en DESC)
);
