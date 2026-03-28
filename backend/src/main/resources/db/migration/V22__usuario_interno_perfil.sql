CREATE TABLE usuario_interno_perfil (
    usuario_id BIGINT PRIMARY KEY,
    sucursal_id BIGINT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NULL,
    puesto VARCHAR(80) NULL,
    notas VARCHAR(500) NULL,
    CONSTRAINT fk_usuario_interno_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_usuario_interno_perfil_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id)
);

CREATE INDEX idx_usuario_interno_perfil_sucursal ON usuario_interno_perfil (sucursal_id);
