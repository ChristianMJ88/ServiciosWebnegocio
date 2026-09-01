ALTER TABLE usuario
    ADD COLUMN correo_verificado_en DATETIME(6) NULL AFTER correo;

UPDATE usuario
SET correo_verificado_en = COALESCE(ultimo_acceso_en, CURRENT_TIMESTAMP(6))
WHERE habilitado = TRUE;

CREATE TABLE empresa_onboarding_progreso (
    empresa_id BIGINT PRIMARY KEY,
    categoria VARCHAR(120) NOT NULL,
    tamano_equipo VARCHAR(30) NOT NULL,
    paso_recomendado VARCHAR(40) NOT NULL,
    horario_completado BOOLEAN NOT NULL DEFAULT FALSE,
    servicio_completado BOOLEAN NOT NULL DEFAULT FALSE,
    personal_completado BOOLEAN NOT NULL DEFAULT FALSE,
    sitio_completado BOOLEAN NOT NULL DEFAULT FALSE,
    cita_prueba_completada BOOLEAN NOT NULL DEFAULT FALSE,
    omitido BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_empresa_onboarding_progreso_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
