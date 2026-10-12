ALTER TABLE usuario MODIFY correo VARCHAR(150) NULL;
ALTER TABLE usuario ADD COLUMN nombre_usuario VARCHAR(80) NULL,
 ADD COLUMN requiere_cambio_contrasena BOOLEAN NOT NULL DEFAULT FALSE,
 ADD UNIQUE KEY uk_usuario_empresa_nombre (empresa_id,nombre_usuario);
ALTER TABLE invitacion_usuario_empresa ADD COLUMN prestador_json LONGTEXT NULL;
