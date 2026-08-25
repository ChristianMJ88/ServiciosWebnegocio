ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN plantilla_menu_bienvenida_sid VARCHAR(80) NULL
    AFTER plantilla_espacio_disponible_walkin_sid;
