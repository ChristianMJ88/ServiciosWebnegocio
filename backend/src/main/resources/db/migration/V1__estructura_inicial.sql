CREATE TABLE empresa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    zona_horaria VARCHAR(60) NOT NULL DEFAULT 'America/Mexico_City',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sucursal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    telefono VARCHAR(30),
    zona_horaria VARCHAR(60) NOT NULL DEFAULT 'America/Mexico_City',
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sucursal_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE rol (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    correo VARCHAR(150) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_acceso_en DATETIME NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_empresa_correo UNIQUE (empresa_id, correo),
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id, empresa_id),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
    CONSTRAINT fk_usuario_rol_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE cliente (
    usuario_id BIGINT PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    acepta_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    notas VARCHAR(500),
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE prestador_servicio (
    usuario_id BIGINT PRIMARY KEY,
    sucursal_id BIGINT NOT NULL,
    nombre_mostrar VARCHAR(150) NOT NULL,
    biografia VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    color_agenda VARCHAR(20),
    CONSTRAINT fk_prestador_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_prestador_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id)
);

CREATE TABLE servicio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    duracion_minutos INT NOT NULL,
    buffer_antes_minutos INT NOT NULL DEFAULT 0,
    buffer_despues_minutos INT NOT NULL DEFAULT 0,
    precio DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'MXN',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_servicio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_servicio_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id)
);

CREATE TABLE asignacion_servicio_prestador (
    prestador_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    duracion_personalizada_minutos INT NULL,
    precio_personalizado DECIMAL(10,2) NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (prestador_id, servicio_id),
    CONSTRAINT fk_asignacion_prestador FOREIGN KEY (prestador_id) REFERENCES prestador_servicio(usuario_id),
    CONSTRAINT fk_asignacion_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id)
);

CREATE TABLE regla_disponibilidad (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    tipo_sujeto VARCHAR(20) NOT NULL,
    sujeto_id BIGINT NOT NULL,
    dia_semana TINYINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    intervalo_minutos INT NOT NULL DEFAULT 15,
    vigente_desde DATE NULL,
    vigente_hasta DATE NULL,
    CONSTRAINT fk_regla_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE excepcion_disponibilidad (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    tipo_sujeto VARCHAR(20) NOT NULL,
    sujeto_id BIGINT NOT NULL,
    fecha_excepcion DATE NOT NULL,
    hora_inicio TIME NULL,
    hora_fin TIME NULL,
    tipo_bloqueo VARCHAR(20) NOT NULL,
    motivo VARCHAR(255),
    CONSTRAINT fk_excepcion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE cita (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    prestador_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    inicio DATETIME NOT NULL,
    fin DATETIME NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'MXN',
    notas VARCHAR(500),
    creada_por_usuario_id BIGINT NOT NULL,
    cancelada_en DATETIME NULL,
    motivo_cancelacion VARCHAR(255) NULL,
    reprogramada_desde_id BIGINT NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cita_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_cita_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    CONSTRAINT fk_cita_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id),
    CONSTRAINT fk_cita_prestador FOREIGN KEY (prestador_id) REFERENCES prestador_servicio(usuario_id),
    CONSTRAINT fk_cita_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(usuario_id),
    CONSTRAINT fk_cita_creada_por FOREIGN KEY (creada_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_cita_reprogramada FOREIGN KEY (reprogramada_desde_id) REFERENCES cita(id)
);

CREATE TABLE historial_estado_cita (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cita_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    cambiado_por_usuario_id BIGINT NOT NULL,
    motivo VARCHAR(255),
    cambiado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_cita FOREIGN KEY (cita_id) REFERENCES cita(id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (cambiado_por_usuario_id) REFERENCES usuario(id)
);

CREATE TABLE token_actualizacion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expira_en DATETIME NOT NULL,
    revocado_en DATETIME NULL,
    nombre_dispositivo VARCHAR(120),
    direccion_ip VARCHAR(45),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE bandeja_salida_notificacion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    tipo_agregado VARCHAR(50) NOT NULL,
    agregado_id BIGINT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    canal VARCHAR(30) NOT NULL,
    payload_json JSON NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    programada_en DATETIME NOT NULL,
    enviada_en DATETIME NULL,
    mensaje_error VARCHAR(500) NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bandeja_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE INDEX idx_cita_prestador_horario ON cita (prestador_id, inicio, fin, estado);
CREATE INDEX idx_cita_sucursal_horario ON cita (sucursal_id, inicio, estado);
CREATE INDEX idx_regla_sujeto ON regla_disponibilidad (tipo_sujeto, sujeto_id, dia_semana);
CREATE INDEX idx_excepcion_sujeto ON excepcion_disponibilidad (tipo_sujeto, sujeto_id, fecha_excepcion);

INSERT INTO empresa (nombre, slug) VALUES ('Empresa Demo', 'empresa-demo');
INSERT INTO sucursal (empresa_id, nombre, direccion, telefono) VALUES (1, 'Sucursal Centro', 'Por definir', '5550000000');
INSERT INTO rol (codigo) VALUES ('ADMIN'), ('STAFF'), ('CLIENTE');
INSERT INTO servicio (empresa_id, sucursal_id, nombre, descripcion, duracion_minutos, buffer_antes_minutos, buffer_despues_minutos, precio, moneda, activo)
VALUES
    (1, 1, 'Manicura', 'Servicio base de ejemplo', 60, 0, 10, 250.00, 'MXN', TRUE),
    (1, 1, 'Pedicura', 'Servicio base de ejemplo', 75, 0, 15, 320.00, 'MXN', TRUE);

