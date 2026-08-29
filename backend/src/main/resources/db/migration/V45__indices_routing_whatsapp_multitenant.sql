CREATE INDEX idx_whatsapp_config_subaccount
    ON configuracion_whatsapp_empresa (subaccount_sid);

CREATE INDEX idx_whatsapp_config_account
    ON configuracion_whatsapp_empresa (account_sid);

CREATE INDEX idx_whatsapp_config_messaging_service
    ON configuracion_whatsapp_empresa (messaging_service_sid);

CREATE INDEX idx_whatsapp_config_numero_remitente
    ON configuracion_whatsapp_empresa (numero_remitente);

CREATE INDEX idx_whatsapp_config_sender_phone
    ON configuracion_whatsapp_empresa (sender_phone_number);
