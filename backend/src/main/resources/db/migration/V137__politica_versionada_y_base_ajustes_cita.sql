-- 007-1: versiones reservadas tras fetch y revisión de referencias; no publica políticas ni acepta por clientes.
CREATE TABLE politica_empresa (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 empresa_id BIGINT NOT NULL,
 version INT NOT NULL,
 modo_no_show VARCHAR(20) NOT NULL,
 dias_reagendar INT NULL,
 dias_aclaracion INT NOT NULL,
 reagendados_max INT NULL,
 recordatorio_1_dias INT NULL,
 recordatorio_2_dias INT NULL,
 texto_adicional VARCHAR(2000) NULL,
 contacto_reclamos_correo VARCHAR(255) NULL,
 contacto_reclamos_telefono VARCHAR(30) NULL,
 plantilla_texto VARCHAR(30) NOT NULL,
 texto_generado TEXT NOT NULL,
 texto_sha256 CHAR(64) NOT NULL,
 publicada_en DATETIME(6) NOT NULL,
 publicada_por_usuario_id BIGINT NOT NULL,
 UNIQUE KEY uk_politica_empresa_version (empresa_id, version),
 UNIQUE KEY uk_politica_empresa_id (empresa_id, id),
 CONSTRAINT fk_politica_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
 CONSTRAINT fk_politica_actor FOREIGN KEY (publicada_por_usuario_id) REFERENCES usuario(id),
 CONSTRAINT ck_politica_version CHECK (version > 0),
 CONSTRAINT ck_politica_contacto CHECK (NULLIF(TRIM(contacto_reclamos_correo), '') IS NOT NULL OR NULLIF(TRIM(contacto_reclamos_telefono), '') IS NOT NULL),
 CONSTRAINT ck_politica_modo CHECK (modo_no_show IN ('CREDITO_REAGENDAR', 'RETENER')),
 CONSTRAINT ck_politica_plazos CHECK (dias_aclaracion > 0 AND (
  (modo_no_show = 'RETENER' AND dias_reagendar IS NULL AND reagendados_max IS NULL AND recordatorio_1_dias IS NULL AND recordatorio_2_dias IS NULL) OR
  (modo_no_show = 'CREDITO_REAGENDAR' AND dias_reagendar IS NOT NULL AND reagendados_max IS NOT NULL AND recordatorio_1_dias IS NOT NULL AND recordatorio_2_dias IS NOT NULL
   AND dias_reagendar > 0 AND dias_aclaracion >= dias_reagendar AND reagendados_max > 0
   AND dias_reagendar > recordatorio_1_dias AND recordatorio_1_dias > recordatorio_2_dias AND recordatorio_2_dias > 0)))
);

ALTER TABLE cita
 ADD COLUMN folio VARCHAR(16) NULL,
 ADD COLUMN politica_version_id BIGINT NULL,
 ADD COLUMN politica_aceptada_en DATETIME(6) NULL,
 ADD UNIQUE KEY uk_cita_empresa_folio (empresa_id, folio),
 ADD UNIQUE KEY uk_cita_empresa_id (empresa_id, id),
 ADD CONSTRAINT fk_cita_politica_empresa FOREIGN KEY (empresa_id, politica_version_id) REFERENCES politica_empresa(empresa_id, id),
 ADD CONSTRAINT ck_cita_aceptacion CHECK ((politica_version_id IS NULL) = (politica_aceptada_en IS NULL));

ALTER TABLE checkout_orden
 ADD COLUMN politica_version_id BIGINT NULL,
 ADD COLUMN politica_aceptada_en DATETIME(6) NULL,
 ADD CONSTRAINT fk_checkout_politica_empresa FOREIGN KEY (empresa_id, politica_version_id) REFERENCES politica_empresa(empresa_id, id),
 ADD CONSTRAINT ck_checkout_aceptacion CHECK ((politica_version_id IS NULL) = (politica_aceptada_en IS NULL));

-- Base única para 007-2/4. No modifica saldos, ventas ni pagos en esta entrega.
CREATE TABLE ajuste_saldo_cita (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 empresa_id BIGINT NOT NULL,
 cita_origen_id BIGINT NOT NULL,
 cita_destino_id BIGINT NULL,
 tipo VARCHAR(30) NOT NULL,
 monto DECIMAL(10,2) NOT NULL,
 moneda VARCHAR(10) NOT NULL,
 politica_version_id BIGINT NULL,
 revierte_ajuste_id BIGINT NULL,
 origen VARCHAR(20) NOT NULL,
 actor_usuario_id BIGINT NULL,
 motivo VARCHAR(500) NOT NULL,
 efectivo_en DATETIME(6) NOT NULL,
 registrado_en DATETIME(6) NOT NULL,
 clave_idempotencia VARCHAR(120) NOT NULL,
 UNIQUE KEY uk_ajuste_empresa_id (empresa_id,id),
 UNIQUE KEY uk_ajuste_idempotencia (empresa_id,clave_idempotencia),
 UNIQUE KEY uk_ajuste_reversion (revierte_ajuste_id),
 KEY ix_ajuste_origen (empresa_id,cita_origen_id),
 KEY ix_ajuste_destino (empresa_id,cita_destino_id),
 KEY ix_ajuste_tipo_fecha (empresa_id,tipo,efectivo_en),
 CONSTRAINT fk_ajuste_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
 CONSTRAINT fk_ajuste_cita_origen FOREIGN KEY (empresa_id,cita_origen_id) REFERENCES cita(empresa_id,id),
 CONSTRAINT fk_ajuste_cita_destino FOREIGN KEY (empresa_id,cita_destino_id) REFERENCES cita(empresa_id,id),
 CONSTRAINT fk_ajuste_politica FOREIGN KEY (empresa_id,politica_version_id) REFERENCES politica_empresa(empresa_id,id),
 CONSTRAINT fk_ajuste_reversion FOREIGN KEY (empresa_id,revierte_ajuste_id) REFERENCES ajuste_saldo_cita(empresa_id,id),
 CONSTRAINT fk_ajuste_actor FOREIGN KEY (actor_usuario_id) REFERENCES usuario(id),
 CONSTRAINT ck_ajuste_tipo CHECK (tipo IN ('CUSTODIA','TRANSFERENCIA','VENCIMIENTO','REEMBOLSO_FUERA_SISTEMA','CONDONACION','RETENCION','REVERSION')),
 CONSTRAINT ck_ajuste_monto CHECK (monto > 0),
 CONSTRAINT ck_ajuste_motivo CHECK (CHAR_LENGTH(TRIM(motivo)) > 0),
 CONSTRAINT ck_ajuste_destino CHECK ((tipo = 'TRANSFERENCIA') = (cita_destino_id IS NOT NULL)),
 CONSTRAINT ck_ajuste_citas_distintas CHECK (cita_destino_id IS NULL OR cita_destino_id <> cita_origen_id),
 CONSTRAINT ck_ajuste_reversion CHECK ((tipo = 'REVERSION') = (revierte_ajuste_id IS NOT NULL))
);
