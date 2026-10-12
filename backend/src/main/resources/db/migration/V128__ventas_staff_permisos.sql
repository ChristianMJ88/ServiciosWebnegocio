INSERT IGNORE INTO permiso (codigo,nombre,descripcion) VALUES
('STAFF_VENTAS_VER','Mis ventas','Consultar únicamente las ventas atribuidas al colaborador autenticado'),
('STAFF_VENTAS_REPORTES_VER','Reporte de mis ventas','Consultar el reporte personal de ventas con filtros');
INSERT IGNORE INTO rol_permiso (rol_id,permiso_id)
SELECT r.id,p.id FROM rol r JOIN permiso p WHERE r.codigo IN ('ADMIN','STAFF')
AND p.codigo IN ('STAFF_VENTAS_VER','STAFF_VENTAS_REPORTES_VER');
INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id,permiso_id)
SELECT r.id,p.id FROM rol_empresa r JOIN permiso p WHERE r.codigo IN ('ADMIN','STAFF')
AND p.codigo IN ('STAFF_VENTAS_VER','STAFF_VENTAS_REPORTES_VER');
