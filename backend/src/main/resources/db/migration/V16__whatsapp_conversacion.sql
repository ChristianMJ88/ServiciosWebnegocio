CREATE TABLE whatsapp_conversacion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    telefono_normalizado VARCHAR(30) NOT NULL,
    flujo VARCHAR(40) NOT NULL,
    paso VARCHAR(40) NOT NULL,
    sucursal_id BIGINT NULL,
    servicio_id BIGINT NULL,
    fecha_seleccionada DATE NULL,
    hora_seleccionada VARCHAR(10) NULL,
    nombre_cliente VARCHAR(150) NULL,
    correo_cliente VARCHAR(150) NULL,
    actualizada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_whatsapp_conversacion_empresa_telefono UNIQUE (empresa_id, telefono_normalizado),
    CONSTRAINT fk_whatsapp_conversacion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
