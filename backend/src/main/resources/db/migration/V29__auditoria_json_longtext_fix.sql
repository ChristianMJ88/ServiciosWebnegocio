ALTER TABLE auditoria_rol_empresa
    MODIFY COLUMN detalle_antes_json LONGTEXT NULL,
    MODIFY COLUMN detalle_despues_json LONGTEXT NULL;

ALTER TABLE auditoria_configuracion_empresa
    MODIFY COLUMN detalle_antes_json LONGTEXT NULL,
    MODIFY COLUMN detalle_despues_json LONGTEXT NULL;
