INSERT INTO permiso (codigo,nombre,descripcion) VALUES ('COMISIONES_GESTIONAR','Gestionar comisiones','Permite configurar reglas de comisión y liquidaciones') ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),descripcion=VALUES(descripcion);
INSERT IGNORE INTO rol_permiso (rol_id,permiso_id) SELECT r.id,p.id FROM rol r JOIN permiso p WHERE r.codigo='ADMIN' AND p.codigo='COMISIONES_GESTIONAR';
INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id,permiso_id) SELECT re.id,p.id FROM rol_empresa re JOIN permiso p WHERE re.codigo='ADMIN' AND p.codigo='COMISIONES_GESTIONAR';
CREATE TABLE comision_regla (
 id BIGINT NOT NULL AUTO_INCREMENT, empresa_id BIGINT NOT NULL, nombre VARCHAR(160) NOT NULL, descripcion VARCHAR(500) NULL,
 concepto VARCHAR(20) NOT NULL, alcance VARCHAR(20) NOT NULL, servicio_id BIGINT NULL, producto_id BIGINT NULL, categoria_producto_id BIGINT NULL,
 beneficiario VARCHAR(20) NOT NULL, prestador_id BIGINT NULL, tipo_calculo VARCHAR(20) NOT NULL, base_calculo VARCHAR(30) NOT NULL,
 valor DECIMAL(12,4) NOT NULL, moneda VARCHAR(10) NULL, fecha_inicio DATE NULL, fecha_fin DATE NULL, prioridad INT NOT NULL DEFAULT 0,
 activa BOOLEAN NOT NULL DEFAULT TRUE, creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), CONSTRAINT fk_comision_empresa FOREIGN KEY(empresa_id) REFERENCES empresa(id), CONSTRAINT fk_comision_servicio FOREIGN KEY(servicio_id) REFERENCES servicio(id) ON DELETE CASCADE,
 CONSTRAINT fk_comision_producto FOREIGN KEY(producto_id) REFERENCES producto(id) ON DELETE CASCADE, CONSTRAINT fk_comision_categoria_producto FOREIGN KEY(categoria_producto_id) REFERENCES producto_categoria(id) ON DELETE CASCADE,
 CONSTRAINT fk_comision_prestador FOREIGN KEY(prestador_id) REFERENCES prestador_servicio(usuario_id) ON DELETE CASCADE,
 INDEX idx_comision_regla_resolucion(empresa_id,activa,concepto,prioridad,fecha_inicio,fecha_fin)
);
