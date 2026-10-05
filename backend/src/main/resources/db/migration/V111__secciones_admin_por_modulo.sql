CREATE TABLE plataforma_seccion_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    requiere_modulo BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0
);

CREATE TABLE plataforma_modulo_seccion_admin (
    modulo_id BIGINT NOT NULL,
    seccion_codigo VARCHAR(80) NOT NULL,
    PRIMARY KEY (modulo_id, seccion_codigo),
    CONSTRAINT fk_modulo_seccion_admin_modulo FOREIGN KEY (modulo_id) REFERENCES plataforma_modulo(id) ON DELETE CASCADE,
    CONSTRAINT fk_modulo_seccion_admin_seccion FOREIGN KEY (seccion_codigo) REFERENCES plataforma_seccion_admin(codigo) ON DELETE CASCADE
);

INSERT INTO plataforma_seccion_admin (codigo, nombre, descripcion, requiere_modulo, orden) VALUES
    ('resumen', 'Dashboard', 'Indicadores principales del negocio', FALSE, 10),
    ('citas', 'Agenda', 'Operación y seguimiento de reservaciones', TRUE, 20),
    ('clientes', 'Clientes', 'Directorio e historial de clientes', TRUE, 30),
    ('contactos', 'Contactos', 'Seguimiento de contactos comerciales', TRUE, 40),
    ('mensajes', 'Mensajes', 'Conversaciones de WhatsApp', TRUE, 50),
    ('whatsapp', 'WhatsApp', 'Configuración del canal de WhatsApp', TRUE, 60),
    ('correo', 'Correo', 'Configuración de correo transaccional', TRUE, 70),
    ('servicios', 'Servicios', 'Catálogo de servicios', TRUE, 80),
    ('productos', 'Productos', 'Catálogo de productos', TRUE, 90),
    ('inventario', 'Inventario', 'Existencias y movimientos', TRUE, 100),
    ('sucursales', 'Sucursales', 'Sedes operativas del negocio', FALSE, 110),
    ('pagos', 'Pagos', 'Configuración de cobros en línea', TRUE, 120),
    ('liquidaciones', 'Liquidaciones', 'Conciliación y reportes de liberaciones', TRUE, 130),
    ('comisiones', 'Comisiones', 'Reglas de comisiones para colaboradores', TRUE, 140),
    ('suscripcion', 'Plan Fluora', 'Plan, capacidad y facturación SaaS', FALSE, 150),
    ('envios', 'Envíos', 'Tarifas, guías y seguimiento', TRUE, 160),
    ('prestadores', 'Prestadores', 'Colaboradores que atienden servicios', TRUE, 170),
    ('usuarios', 'Usuarios y accesos', 'Identidades y acceso del negocio', FALSE, 180),
    ('permisos', 'Permisos', 'Roles y capacidades del negocio', FALSE, 190),
    ('reglas', 'Horarios base', 'Disponibilidad semanal', TRUE, 200),
    ('excepciones', 'Bloqueos', 'Cierres y excepciones de disponibilidad', TRUE, 210),
    ('sitio', 'Sitio web', 'Identidad y publicación del sitio público', TRUE, 220),
    ('parametros', 'Parámetros', 'Configuración operativa del negocio', FALSE, 230),
    ('caja', 'Caja', 'Punto de venta presencial', TRUE, 240),
    ('recepcion', 'Recepción', 'Operación de llegadas y sala de espera', TRUE, 250)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), descripcion=VALUES(descripcion), requiere_modulo=VALUES(requiere_modulo), orden=VALUES(orden);

INSERT INTO plataforma_modulo_seccion_admin (modulo_id, seccion_codigo)
SELECT id, 'sitio' FROM plataforma_modulo WHERE codigo='SITIO_PUBLICO'
UNION ALL SELECT id, 'correo' FROM plataforma_modulo WHERE codigo='CORREO'
UNION ALL SELECT id, 'whatsapp' FROM plataforma_modulo WHERE codigo='WHATSAPP'
UNION ALL SELECT id, 'mensajes' FROM plataforma_modulo WHERE codigo='WHATSAPP'
UNION ALL SELECT id, 'contactos' FROM plataforma_modulo WHERE codigo='CRM'
UNION ALL SELECT id, 'clientes' FROM plataforma_modulo WHERE codigo='CRM'
UNION ALL SELECT id, 'servicios' FROM plataforma_modulo WHERE codigo='AGENDA'
UNION ALL SELECT id, 'prestadores' FROM plataforma_modulo WHERE codigo='AGENDA'
UNION ALL SELECT id, 'reglas' FROM plataforma_modulo WHERE codigo='AGENDA'
UNION ALL SELECT id, 'excepciones' FROM plataforma_modulo WHERE codigo='AGENDA'
UNION ALL SELECT id, 'citas' FROM plataforma_modulo WHERE codigo='AGENDA'
UNION ALL SELECT id, 'productos' FROM plataforma_modulo WHERE codigo='PRODUCTOS'
UNION ALL SELECT id, 'inventario' FROM plataforma_modulo WHERE codigo='INVENTARIO'
UNION ALL SELECT id, 'pagos' FROM plataforma_modulo WHERE codigo='PAGOS_ONLINE'
UNION ALL SELECT id, 'liquidaciones' FROM plataforma_modulo WHERE codigo='PAGOS_ONLINE'
UNION ALL SELECT id, 'envios' FROM plataforma_modulo WHERE codigo='ENVIOS'
UNION ALL SELECT id, 'comisiones' FROM plataforma_modulo WHERE codigo='COMISIONES'
UNION ALL SELECT id, 'caja' FROM plataforma_modulo WHERE codigo='CAJA'
UNION ALL SELECT id, 'recepcion' FROM plataforma_modulo WHERE codigo='RECEPCION';
