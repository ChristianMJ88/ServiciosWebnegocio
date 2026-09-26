CREATE TABLE producto_tienda_config (
    empresa_id BIGINT NOT NULL,
    habilitada BOOLEAN NOT NULL DEFAULT FALSE,
    menu_mostrar_productos BOOLEAN NOT NULL DEFAULT TRUE,
    titulo VARCHAR(160) NULL,
    descripcion VARCHAR(500) NULL,
    seo_titulo VARCHAR(70) NULL,
    seo_descripcion VARCHAR(170) NULL,
    texto_agregar VARCHAR(40) NULL,
    carrito_habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_stock BOOLEAN NOT NULL DEFAULT FALSE,
    permitir_pedido_whatsapp BOOLEAN NOT NULL DEFAULT TRUE,
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (empresa_id),
    CONSTRAINT fk_producto_tienda_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

INSERT INTO producto_tienda_config (empresa_id, habilitada, menu_mostrar_productos)
SELECT e.id, FALSE, TRUE
FROM empresa e
ON DUPLICATE KEY UPDATE empresa_id = VALUES(empresa_id);
