ALTER TABLE plataforma_plan
    ADD COLUMN destacado BOOLEAN NOT NULL DEFAULT FALSE AFTER publico,
    ADD COLUMN dias_prueba INT NOT NULL DEFAULT 14 AFTER destacado,
    ADD COLUMN dias_gracia INT NOT NULL DEFAULT 3 AFTER dias_prueba,
    ADD COLUMN vigente_desde DATETIME(6) NULL AFTER dias_gracia,
    ADD COLUMN vigente_hasta DATETIME(6) NULL AFTER vigente_desde;

CREATE TABLE plataforma_limite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    unidad VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0
);

CREATE TABLE plataforma_plan_limite (
    plan_id BIGINT NOT NULL,
    limite_id BIGINT NOT NULL,
    valor BIGINT NOT NULL,
    PRIMARY KEY (plan_id, limite_id),
    CONSTRAINT fk_plataforma_plan_limite_plan FOREIGN KEY (plan_id) REFERENCES plataforma_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_plataforma_plan_limite_catalogo FOREIGN KEY (limite_id) REFERENCES plataforma_limite(id)
);

INSERT INTO plataforma_modulo (codigo, nombre, descripcion, orden) VALUES
    ('CRM', 'Clientes y CRM', 'Directorio, historial y seguimiento de clientes', 25),
    ('RECEPCION', 'Recepción', 'Llegadas, espera y operación del día', 45),
    ('COMISIONES', 'Comisiones', 'Reglas y cálculo de comisiones para colaboradores', 55),
    ('REPORTES', 'Reportes', 'Indicadores y reportes operativos y financieros', 65),
    ('PROMOCIONES', 'Promociones', 'Campañas y promociones publicadas por el negocio', 75),
    ('MARKETPLACE', 'Marketplace', 'Publicación y descubrimiento dentro de Fluora', 85),
    ('MULTISUCURSAL', 'Operación multisucursal', 'Administración consolidada de varias sucursales', 95)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), descripcion=VALUES(descripcion), orden=VALUES(orden);

INSERT INTO plataforma_limite (codigo, nombre, descripcion, unidad, orden) VALUES
    ('SUCURSALES_MAX', 'Sucursales', 'Cantidad máxima de sucursales activas', 'sucursales', 10),
    ('USUARIOS_MAX', 'Usuarios internos', 'Usuarios internos habilitados dentro del negocio', 'usuarios', 20),
    ('PRESTADORES_MAX', 'Prestadores con agenda', 'Colaboradores que pueden recibir reservaciones', 'prestadores', 30),
    ('SERVICIOS_MAX', 'Servicios', 'Servicios activos o publicados por el negocio', 'servicios', 40),
    ('PRODUCTOS_MAX', 'Productos', 'Productos activos dentro del catálogo', 'productos', 50),
    ('IMAGENES_MAX', 'Imágenes totales', 'Archivos originales administrados por Fluora y Cloudinary', 'imágenes', 60),
    ('IMAGENES_SERVICIO_MAX', 'Imágenes por servicio', 'Imágenes originales asociadas a un servicio', 'imágenes/servicio', 70),
    ('IMAGENES_PRODUCTO_MAX', 'Imágenes por producto', 'Imágenes originales asociadas a un producto', 'imágenes/producto', 80),
    ('GALERIA_IMAGENES_MAX', 'Imágenes de galería', 'Imágenes publicadas dentro de la galería del sitio', 'imágenes', 90),
    ('PROMOCIONES_ACTIVAS_MAX', 'Promociones activas', 'Promociones simultáneas dentro del sitio público', 'promociones', 100),
    ('RESENAS_MAX', 'Reseñas manuales', 'Reseñas capturadas y publicadas por el negocio', 'reseñas', 110),
    ('ALMACENAMIENTO_MB', 'Almacenamiento', 'Almacenamiento original permitido para recursos multimedia', 'MB', 120),
    ('WHATSAPP_MENSAJES_INCLUIDOS', 'Mensajes WhatsApp incluidos', 'Mensajes incluidos antes de aplicar consumo adicional', 'mensajes/mes', 130)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), descripcion=VALUES(descripcion), unidad=VALUES(unidad), orden=VALUES(orden);
