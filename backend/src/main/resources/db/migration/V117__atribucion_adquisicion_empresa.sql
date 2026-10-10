CREATE TABLE empresa_adquisicion (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 empresa_id BIGINT NOT NULL,
 source VARCHAR(80) NULL,
 medium VARCHAR(80) NULL,
 campaign VARCHAR(80) NULL,
 content VARCHAR(80) NULL,
 term VARCHAR(80) NULL,
 landing_vertical VARCHAR(80) NULL,
 first_landing_path VARCHAR(81) NULL,
 acquired_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_adquisicion_empresa UNIQUE (empresa_id),
 CONSTRAINT fk_adquisicion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
