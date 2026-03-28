INSERT IGNORE INTO rol (codigo) VALUES ('RECEPCIONISTA'), ('CAJERO');

CREATE TABLE caja_sesion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    monto_inicial DECIMAL(10,2) NOT NULL,
    monto_esperado DECIMAL(10,2) NULL,
    monto_contado DECIMAL(10,2) NULL,
    diferencia DECIMAL(10,2) NULL,
    observaciones VARCHAR(500) NULL,
    abierta_por_usuario_id BIGINT NOT NULL,
    abierta_en DATETIME NOT NULL,
    cerrada_por_usuario_id BIGINT NULL,
    cerrada_en DATETIME NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_caja_sesion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_caja_sesion_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    CONSTRAINT fk_caja_sesion_abierta_por FOREIGN KEY (abierta_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_caja_sesion_cerrada_por FOREIGN KEY (cerrada_por_usuario_id) REFERENCES usuario(id)
);

CREATE TABLE pago_cita (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    cita_id BIGINT NOT NULL,
    caja_sesion_id BIGINT NULL,
    monto DECIMAL(10,2) NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    referencia VARCHAR(120) NULL,
    observaciones VARCHAR(500) NULL,
    registrado_por_usuario_id BIGINT NOT NULL,
    registrado_en DATETIME NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cita_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_pago_cita_cita FOREIGN KEY (cita_id) REFERENCES cita(id),
    CONSTRAINT fk_pago_cita_caja_sesion FOREIGN KEY (caja_sesion_id) REFERENCES caja_sesion(id),
    CONSTRAINT fk_pago_cita_usuario FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id)
);

CREATE TABLE movimiento_caja (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    caja_sesion_id BIGINT NOT NULL,
    cita_id BIGINT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    metodo_pago VARCHAR(30) NULL,
    monto DECIMAL(10,2) NOT NULL,
    concepto VARCHAR(255) NOT NULL,
    referencia VARCHAR(120) NULL,
    observaciones VARCHAR(500) NULL,
    registrado_por_usuario_id BIGINT NOT NULL,
    registrado_en DATETIME NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimiento_caja_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_movimiento_caja_sesion FOREIGN KEY (caja_sesion_id) REFERENCES caja_sesion(id),
    CONSTRAINT fk_movimiento_caja_cita FOREIGN KEY (cita_id) REFERENCES cita(id),
    CONSTRAINT fk_movimiento_caja_usuario FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_caja_sesion_empresa_sucursal_estado ON caja_sesion (empresa_id, sucursal_id, estado);
CREATE INDEX idx_pago_cita_empresa_cita ON pago_cita (empresa_id, cita_id, registrado_en);
CREATE INDEX idx_movimiento_caja_empresa_sesion ON movimiento_caja (empresa_id, caja_sesion_id, registrado_en);
