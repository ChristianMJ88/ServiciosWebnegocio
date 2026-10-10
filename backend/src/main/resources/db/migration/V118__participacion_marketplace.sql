CREATE TABLE empresa_marketplace (
 empresa_id BIGINT NOT NULL PRIMARY KEY,
 participante BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT fk_marketplace_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
