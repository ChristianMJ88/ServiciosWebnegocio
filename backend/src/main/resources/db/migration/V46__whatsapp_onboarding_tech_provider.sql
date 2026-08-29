CREATE TABLE whatsapp_onboarding (
    id CHAR(36) NOT NULL,
    empresa_id BIGINT NOT NULL,
    usuario_actor_id BIGINT NULL,
    estado VARCHAR(40) NOT NULL,
    paso_actual VARCHAR(80) NULL,
    telefono_e164 VARCHAR(40) NULL,
    display_name VARCHAR(150) NULL,
    waba_id VARCHAR(100) NULL,
    phone_number_id VARCHAR(100) NULL,
    channel_sender_sid VARCHAR(80) NULL,
    intentos INT NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(1000) NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_whatsapp_onboarding_empresa (empresa_id),
    KEY idx_whatsapp_onboarding_estado (estado),
    CONSTRAINT fk_whatsapp_onboarding_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);
