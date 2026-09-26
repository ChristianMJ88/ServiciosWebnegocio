INSERT INTO permiso (codigo, nombre, descripcion)
VALUES ('PAGOS_CONFIGURAR', 'Configurar pagos', 'Permite conectar proveedores de pago y configurar el checkout de la empresa')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'PAGOS_CONFIGURAR'
WHERE r.codigo = 'ADMIN';

INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id, permiso_id)
SELECT re.id, p.id
FROM rol_empresa re
JOIN permiso p ON p.codigo = 'PAGOS_CONFIGURAR'
WHERE re.codigo = 'ADMIN';
