CREATE TABLE plataforma_modulo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0
);

CREATE TABLE plataforma_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    moneda VARCHAR(10) NOT NULL,
    precio_mensual DECIMAL(12,2) NOT NULL,
    precio_anual DECIMAL(12,2) NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    publico BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE plataforma_plan_modulo (
    plan_id BIGINT NOT NULL,
    modulo_id BIGINT NOT NULL,
    limite_uso BIGINT NULL,
    PRIMARY KEY (plan_id, modulo_id),
    CONSTRAINT fk_plataforma_plan_modulo_plan FOREIGN KEY (plan_id) REFERENCES plataforma_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_plataforma_plan_modulo_modulo FOREIGN KEY (modulo_id) REFERENCES plataforma_modulo(id)
);

CREATE TABLE plataforma_suscripcion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    ciclo VARCHAR(20) NOT NULL,
    precio_acordado DECIMAL(12,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    inicia_en DATETIME(6) NOT NULL,
    termina_en DATETIME(6) NULL,
    prueba_hasta DATETIME(6) NULL,
    cancelada_en DATETIME(6) NULL,
    proveedor_pago_id BIGINT NULL,
    referencia_externa VARCHAR(180) NULL,
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_plataforma_suscripcion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_plataforma_suscripcion_plan FOREIGN KEY (plan_id) REFERENCES plataforma_plan(id),
    CONSTRAINT fk_plataforma_suscripcion_proveedor FOREIGN KEY (proveedor_pago_id) REFERENCES proveedor_pago(id),
    INDEX idx_plataforma_suscripcion_empresa_estado (empresa_id, estado, inicia_en),
    INDEX idx_plataforma_suscripcion_plan_estado (plan_id, estado)
);

INSERT INTO plataforma_modulo (codigo, nombre, descripcion, orden) VALUES
    ('AGENDA', 'Agenda y citas', 'Reservas, disponibilidad y operación de citas', 10),
    ('SITIO_PUBLICO', 'Sitio público', 'Página pública multitenant y constructor de contenido', 20),
    ('PRODUCTOS', 'Productos', 'Catálogo y venta de productos', 30),
    ('INVENTARIO', 'Inventario', 'Existencias y movimientos de inventario', 40),
    ('CAJA', 'Caja', 'Ventas presenciales de servicios y productos', 50),
    ('PAGOS_ONLINE', 'Pagos en línea', 'Checkout y proveedores de pago conectados', 60),
    ('ENVIOS', 'Envíos', 'Cotización, guías y seguimiento de envíos', 70),
    ('WHATSAPP', 'WhatsApp', 'Automatización y atención por WhatsApp', 80),
    ('CORREO', 'Correo', 'Notificaciones y comunicación por correo', 90)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), descripcion=VALUES(descripcion), orden=VALUES(orden);
