ALTER TABLE configuracion_whatsapp_empresa
    ADD COLUMN plantillas_list_picker_sids VARCHAR(1000) NULL
    AFTER plantilla_menu_bienvenida_sid;
