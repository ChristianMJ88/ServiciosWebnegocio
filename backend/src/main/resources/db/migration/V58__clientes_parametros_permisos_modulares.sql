INSERT INTO permiso (codigo, nombre, descripcion)
VALUES
    ('CLIENTES_GESTIONAR', 'Gestionar clientes', 'Permite consultar, crear y actualizar clientes del negocio'),
    ('PARAMETROS_SISTEMA_GESTIONAR', 'Gestionar parámetros', 'Permite configurar parámetros operativos del negocio')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p
WHERE r.codigo = 'ADMIN' AND p.codigo IN ('CLIENTES_GESTIONAR', 'PARAMETROS_SISTEMA_GESTIONAR');

INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id, permiso_id)
SELECT re.id, p.id FROM rol_empresa re JOIN permiso p
WHERE re.codigo = 'ADMIN' AND p.codigo IN ('CLIENTES_GESTIONAR', 'PARAMETROS_SISTEMA_GESTIONAR');

CREATE TABLE parametro_sistema_definicion (
    clave VARCHAR(80) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    tipo VARCHAR(20) NOT NULL,
    valor_predeterminado VARCHAR(500) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    opciones_json JSON NULL,
    orden INT NOT NULL DEFAULT 0,
    editable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE parametro_sistema_empresa (
    empresa_id BIGINT NOT NULL,
    clave VARCHAR(80) NOT NULL,
    valor VARCHAR(500) NOT NULL,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    actualizado_por_usuario_id BIGINT NULL,
    PRIMARY KEY (empresa_id, clave),
    CONSTRAINT fk_parametro_empresa_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_parametro_empresa_definicion FOREIGN KEY (clave) REFERENCES parametro_sistema_definicion(clave),
    CONSTRAINT fk_parametro_empresa_usuario FOREIGN KEY (actualizado_por_usuario_id) REFERENCES usuario(id)
);

INSERT INTO parametro_sistema_definicion
    (clave, nombre, descripcion, tipo, valor_predeterminado, categoria, opciones_json, orden, editable)
VALUES
    ('MONEDA_PREDETERMINADA', 'Moneda predeterminada', 'Moneda sugerida al crear servicios y mostrar importes.', 'SELECCION', 'MXN', 'Regional', JSON_ARRAY('MXN', 'USD', 'EUR'), 10, TRUE),
    ('FORMATO_HORA', 'Formato de hora', 'Formato usado en la agenda y comunicaciones.', 'SELECCION', '24H', 'Regional', JSON_ARRAY('12H', '24H'), 20, TRUE),
    ('DURACION_INTERVALO_MINUTOS', 'Intervalo de agenda', 'Separación predeterminada entre horarios disponibles.', 'NUMERO', '15', 'Agenda', NULL, 30, TRUE),
    ('ANTICIPACION_RESERVA_HORAS', 'Anticipación mínima', 'Horas mínimas requeridas para reservar una cita.', 'NUMERO', '2', 'Agenda', NULL, 40, TRUE),
    ('VENTANA_CANCELACION_HORAS', 'Ventana de cancelación', 'Horas antes de la cita en que el cliente puede cancelarla.', 'NUMERO', '24', 'Agenda', NULL, 50, TRUE)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre), descripcion = VALUES(descripcion), tipo = VALUES(tipo),
    valor_predeterminado = VALUES(valor_predeterminado), categoria = VALUES(categoria),
    opciones_json = VALUES(opciones_json), orden = VALUES(orden), editable = VALUES(editable);
