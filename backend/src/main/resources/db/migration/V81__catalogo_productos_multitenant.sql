INSERT INTO permiso (codigo, nombre, descripcion)
VALUES
    ('PRODUCTOS_GESTIONAR', 'Gestionar productos', 'Permite administrar productos, categorias, proveedores y precios por zona'),
    ('INVENTARIO_GESTIONAR', 'Gestionar inventario', 'Permite administrar existencias y movimientos de productos')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p
WHERE r.codigo = 'ADMIN' AND p.codigo IN ('PRODUCTOS_GESTIONAR', 'INVENTARIO_GESTIONAR');

INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id, permiso_id)
SELECT re.id, p.id FROM rol_empresa re JOIN permiso p
WHERE re.codigo = 'ADMIN' AND p.codigo IN ('PRODUCTOS_GESTIONAR', 'INVENTARIO_GESTIONAR');

CREATE TABLE producto_categoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    categoria_padre_id BIGINT NULL,
    nombre VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    descripcion VARCHAR(500) NULL,
    orden INT NOT NULL DEFAULT 0,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_categoria_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_producto_categoria_padre FOREIGN KEY (categoria_padre_id) REFERENCES producto_categoria(id),
    CONSTRAINT uk_producto_categoria_empresa_slug UNIQUE (empresa_id, slug),
    INDEX idx_producto_categoria_empresa_orden (empresa_id, activa, orden)
);

CREATE TABLE producto_proveedor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    clave VARCHAR(60) NULL,
    contacto VARCHAR(160) NULL,
    correo VARCHAR(180) NULL,
    telefono VARCHAR(30) NULL,
    sitio_web VARCHAR(500) NULL,
    notas VARCHAR(1000) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_proveedor_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT uk_producto_proveedor_empresa_clave UNIQUE (empresa_id, clave),
    INDEX idx_producto_proveedor_empresa_nombre (empresa_id, activo, nombre)
);

CREATE TABLE producto_zona_precio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    clave VARCHAR(60) NOT NULL,
    descripcion VARCHAR(500) NULL,
    prioridad INT NOT NULL DEFAULT 0,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_zona_precio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT uk_producto_zona_precio_empresa_clave UNIQUE (empresa_id, clave),
    INDEX idx_producto_zona_precio_empresa_prioridad (empresa_id, activa, prioridad)
);

CREATE TABLE producto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    categoria_id BIGINT NULL,
    proveedor_id BIGINT NULL,
    media_id BIGINT NULL,
    sku VARCHAR(80) NOT NULL,
    codigo_barras VARCHAR(80) NULL,
    nombre VARCHAR(180) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    descripcion VARCHAR(1500) NULL,
    imagen_url VARCHAR(1000) NULL,
    precio_base DECIMAL(12,2) NOT NULL,
    costo DECIMAL(12,2) NULL,
    moneda VARCHAR(10) NOT NULL,
    tasa_impuesto DECIMAL(7,4) NOT NULL DEFAULT 0,
    controla_inventario BOOLEAN NOT NULL DEFAULT TRUE,
    stock_minimo DECIMAL(14,3) NOT NULL DEFAULT 0,
    visible_publico BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES producto_categoria(id),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES producto_proveedor(id),
    CONSTRAINT fk_producto_media FOREIGN KEY (media_id) REFERENCES empresa_archivo_media(id) ON DELETE SET NULL,
    CONSTRAINT uk_producto_empresa_sku UNIQUE (empresa_id, sku),
    CONSTRAINT uk_producto_empresa_slug UNIQUE (empresa_id, slug),
    INDEX idx_producto_empresa_catalogo (empresa_id, activo, categoria_id, nombre),
    INDEX idx_producto_empresa_publico (empresa_id, visible_publico, activo)
);

CREATE TABLE producto_precio_zona (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    zona_precio_id BIGINT NOT NULL,
    precio DECIMAL(12,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_precio_zona_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_producto_precio_zona_producto FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE CASCADE,
    CONSTRAINT fk_producto_precio_zona_zona FOREIGN KEY (zona_precio_id) REFERENCES producto_zona_precio(id),
    CONSTRAINT uk_producto_precio_zona UNIQUE (empresa_id, producto_id, zona_precio_id),
    INDEX idx_producto_precio_zona_resolucion (empresa_id, zona_precio_id, producto_id, activo)
);

CREATE TABLE producto_existencia (
    empresa_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    existencia DECIMAL(14,3) NOT NULL DEFAULT 0,
    reservado DECIMAL(14,3) NOT NULL DEFAULT 0,
    stock_minimo DECIMAL(14,3) NULL,
    stock_maximo DECIMAL(14,3) NULL,
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (empresa_id, producto_id, sucursal_id),
    CONSTRAINT fk_producto_existencia_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_producto_existencia_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT fk_producto_existencia_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    INDEX idx_producto_existencia_sucursal (empresa_id, sucursal_id, producto_id)
);

CREATE TABLE producto_movimiento_inventario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad DECIMAL(14,3) NOT NULL,
    existencia_anterior DECIMAL(14,3) NOT NULL,
    existencia_nueva DECIMAL(14,3) NOT NULL,
    referencia VARCHAR(120) NULL,
    notas VARCHAR(500) NULL,
    creado_por_usuario_id BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_movimiento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_producto_movimiento_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT fk_producto_movimiento_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    CONSTRAINT fk_producto_movimiento_usuario FOREIGN KEY (creado_por_usuario_id) REFERENCES usuario(id),
    INDEX idx_producto_movimiento_consulta (empresa_id, producto_id, sucursal_id, creado_en)
);
