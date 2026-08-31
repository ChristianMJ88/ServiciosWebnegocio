ALTER TABLE invitacion_usuario_empresa MODIFY token_hash VARCHAR(64) NOT NULL;
ALTER TABLE recuperacion_contrasena MODIFY token_hash VARCHAR(64) NOT NULL;
