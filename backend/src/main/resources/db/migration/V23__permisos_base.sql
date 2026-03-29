CREATE TABLE permiso (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL
);

CREATE TABLE rol_permiso (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id)
);

INSERT INTO rol (codigo)
VALUES ('RECEPCIONISTA'), ('CAJERO')
ON DUPLICATE KEY UPDATE codigo = VALUES(codigo);

INSERT INTO permiso (codigo, nombre, descripcion)
VALUES
    ('PANEL_ADMIN_ACCESO', 'Acceso al panel administrativo', 'Permite entrar al panel administrativo'),
    ('CONFIGURACION_EMPRESA_GESTIONAR', 'Gestionar configuración del negocio', 'Permite editar configuraciones de la empresa'),
    ('SUCURSALES_GESTIONAR', 'Gestionar sucursales', 'Permite crear y editar sucursales'),
    ('SERVICIOS_GESTIONAR', 'Gestionar servicios', 'Permite crear y editar servicios'),
    ('PRESTADORES_GESTIONAR', 'Gestionar prestadores', 'Permite crear y editar prestadores'),
    ('USUARIOS_INTERNOS_GESTIONAR', 'Gestionar usuarios internos', 'Permite crear y editar usuarios internos'),
    ('WHATSAPP_CONFIGURAR', 'Configurar WhatsApp', 'Permite administrar WhatsApp y Twilio'),
    ('CONTACTOS_ADMIN_VER', 'Ver contactos', 'Permite consultar solicitudes de contacto'),
    ('REPORTES_ADMIN_VER', 'Ver reportes administrativos', 'Permite consultar reportes del negocio'),
    ('CITAS_ADMIN_GESTIONAR', 'Gestionar citas administrativas', 'Permite confirmar, cancelar o finalizar citas desde admin'),
    ('RECEPCION_ACCESO', 'Acceso a recepción', 'Permite entrar al panel de recepción'),
    ('RECEPCION_CLIENTES_VER', 'Ver clientes en recepción', 'Permite buscar clientes desde recepción'),
    ('RECEPCION_CITAS_GESTIONAR', 'Gestionar citas en recepción', 'Permite crear, confirmar, cancelar y reagendar citas desde recepción'),
    ('RECEPCION_CHECKIN', 'Registrar check-in', 'Permite marcar llegadas de clientes'),
    ('CAJA_ACCESO', 'Acceso a caja', 'Permite entrar al panel de caja'),
    ('CAJA_COBRAR', 'Cobrar citas', 'Permite registrar pagos de citas'),
    ('CAJA_SESION_GESTIONAR', 'Gestionar sesión de caja', 'Permite abrir y cerrar caja'),
    ('CAJA_MOVIMIENTOS_GESTIONAR', 'Gestionar movimientos de caja', 'Permite registrar ingresos, gastos y ajustes'),
    ('STAFF_PANEL_ACCESO', 'Acceso a panel staff', 'Permite entrar al panel staff'),
    ('STAFF_AGENDA_VER', 'Ver agenda staff', 'Permite consultar agenda del prestador'),
    ('STAFF_CITAS_GESTIONAR', 'Gestionar citas staff', 'Permite confirmar, finalizar o marcar no asistencia'),
    ('STAFF_DISPONIBILIDAD_GESTIONAR', 'Gestionar disponibilidad staff', 'Permite editar reglas y excepciones de disponibilidad'),
    ('CLIENTE_PANEL_ACCESO', 'Acceso a panel cliente', 'Permite entrar al panel del cliente'),
    ('CLIENTE_CITAS_VER', 'Ver citas cliente', 'Permite consultar citas del cliente'),
    ('CLIENTE_CITAS_GESTIONAR', 'Gestionar citas cliente', 'Permite cancelar o reprogramar citas propias')
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    descripcion = VALUES(descripcion);

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p
WHERE r.codigo = 'ADMIN'
  AND p.codigo IN (
    'PANEL_ADMIN_ACCESO',
    'CONFIGURACION_EMPRESA_GESTIONAR',
    'SUCURSALES_GESTIONAR',
    'SERVICIOS_GESTIONAR',
    'PRESTADORES_GESTIONAR',
    'USUARIOS_INTERNOS_GESTIONAR',
    'WHATSAPP_CONFIGURAR',
    'CONTACTOS_ADMIN_VER',
    'REPORTES_ADMIN_VER',
    'CITAS_ADMIN_GESTIONAR',
    'RECEPCION_ACCESO',
    'RECEPCION_CLIENTES_VER',
    'RECEPCION_CITAS_GESTIONAR',
    'RECEPCION_CHECKIN',
    'CAJA_ACCESO',
    'CAJA_COBRAR',
    'CAJA_SESION_GESTIONAR',
    'CAJA_MOVIMIENTOS_GESTIONAR'
  );

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p
WHERE r.codigo = 'RECEPCIONISTA'
  AND p.codigo IN (
    'RECEPCION_ACCESO',
    'RECEPCION_CLIENTES_VER',
    'RECEPCION_CITAS_GESTIONAR',
    'RECEPCION_CHECKIN',
    'CAJA_ACCESO',
    'CAJA_COBRAR',
    'CAJA_SESION_GESTIONAR'
  );

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p
WHERE r.codigo = 'CAJERO'
  AND p.codigo IN (
    'CAJA_ACCESO',
    'CAJA_COBRAR',
    'CAJA_SESION_GESTIONAR',
    'CAJA_MOVIMIENTOS_GESTIONAR'
  );

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p
WHERE r.codigo = 'STAFF'
  AND p.codigo IN (
    'STAFF_PANEL_ACCESO',
    'STAFF_AGENDA_VER',
    'STAFF_CITAS_GESTIONAR',
    'STAFF_DISPONIBILIDAD_GESTIONAR'
  );

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p
WHERE r.codigo = 'CLIENTE'
  AND p.codigo IN (
    'CLIENTE_PANEL_ACCESO',
    'CLIENTE_CITAS_VER',
    'CLIENTE_CITAS_GESTIONAR'
  );
