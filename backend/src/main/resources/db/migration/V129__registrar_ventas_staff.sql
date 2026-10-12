INSERT IGNORE INTO permiso(codigo,nombre,descripcion) VALUES
('STAFF_VENTAS_REGISTRAR','Registrar venta de Staff','Preparar ventas de productos para cobro en Caja'),
('STAFF_VENTAS_CLIENTES_GESTIONAR','Clientes de ventas Staff','Buscar y registrar clientes para una venta');
INSERT IGNORE INTO rol_permiso(rol_id,permiso_id)
SELECT r.id,p.id FROM rol r JOIN permiso p WHERE r.codigo IN ('ADMIN','STAFF') AND p.codigo IN ('STAFF_VENTAS_REGISTRAR','STAFF_VENTAS_CLIENTES_GESTIONAR');
INSERT IGNORE INTO rol_empresa_permiso(rol_empresa_id,permiso_id)
SELECT r.id,p.id FROM rol_empresa r JOIN permiso p WHERE r.codigo IN ('ADMIN','STAFF') AND p.codigo IN ('STAFF_VENTAS_REGISTRAR','STAFF_VENTAS_CLIENTES_GESTIONAR');
CREATE INDEX idx_venta_pendientes_sucursal ON venta(empresa_id,sucursal_id,estado,creada_en);

ALTER TABLE usuario MODIFY correo VARCHAR(150) NULL;
