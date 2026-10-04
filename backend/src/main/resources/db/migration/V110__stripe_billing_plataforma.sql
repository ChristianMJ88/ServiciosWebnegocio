ALTER TABLE plataforma_plan
    ADD COLUMN stripe_producto_id VARCHAR(100) NULL AFTER vigente_hasta,
    ADD COLUMN stripe_precio_mensual_id VARCHAR(100) NULL AFTER stripe_producto_id,
    ADD COLUMN stripe_precio_anual_id VARCHAR(100) NULL AFTER stripe_precio_mensual_id,
    ADD COLUMN stripe_config_hash VARCHAR(64) NULL AFTER stripe_precio_anual_id;

ALTER TABLE plataforma_suscripcion
    ADD COLUMN stripe_cliente_id VARCHAR(100) NULL AFTER referencia_externa,
    ADD COLUMN stripe_suscripcion_id VARCHAR(100) NULL AFTER stripe_cliente_id,
    ADD COLUMN stripe_checkout_sesion_id VARCHAR(100) NULL AFTER stripe_suscripcion_id,
    ADD COLUMN stripe_ultima_factura_id VARCHAR(100) NULL AFTER stripe_checkout_sesion_id,
    ADD COLUMN stripe_estado VARCHAR(40) NULL AFTER stripe_ultima_factura_id,
    ADD COLUMN periodo_actual_hasta DATETIME(6) NULL AFTER stripe_estado,
    ADD COLUMN cancelar_al_final_periodo BOOLEAN NOT NULL DEFAULT FALSE AFTER periodo_actual_hasta,
    ADD UNIQUE INDEX uk_plataforma_suscripcion_stripe (stripe_suscripcion_id),
    ADD INDEX idx_plataforma_suscripcion_stripe_cliente (stripe_cliente_id);

CREATE TABLE plataforma_stripe_cliente (
    empresa_id BIGINT PRIMARY KEY,
    stripe_cliente_id VARCHAR(100) NOT NULL UNIQUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_plataforma_stripe_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE plataforma_stripe_evento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stripe_evento_id VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(120) NOT NULL,
    modo_produccion BOOLEAN NOT NULL,
    estado VARCHAR(30) NOT NULL,
    error_ultimo VARCHAR(1000) NULL,
    recibido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    procesado_en DATETIME(6) NULL,
    INDEX idx_plataforma_stripe_evento_estado (estado, recibido_en)
);

INSERT INTO permiso (codigo, nombre, descripcion)
VALUES ('SUSCRIPCION_EMPRESA_GESTIONAR', 'Gestionar suscripción de Fluora', 'Permite contratar y administrar el plan SaaS del negocio')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), descripcion=VALUES(descripcion);

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p
WHERE r.codigo='ADMIN' AND p.codigo='SUSCRIPCION_EMPRESA_GESTIONAR';

INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id, permiso_id)
SELECT re.id, p.id FROM rol_empresa re JOIN permiso p
WHERE re.codigo='ADMIN' AND p.codigo='SUSCRIPCION_EMPRESA_GESTIONAR';
