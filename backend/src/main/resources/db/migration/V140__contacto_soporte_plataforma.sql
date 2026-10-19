CREATE TABLE plataforma_contacto (
 id TINYINT PRIMARY KEY,
 correo VARCHAR(150) NOT NULL,
 correo_legal VARCHAR(150) NOT NULL,
 telefono VARCHAR(16) NOT NULL,
 dias JSON NOT NULL,
 hora_inicio TIME NOT NULL,
 hora_fin TIME NOT NULL,
 zona_horaria VARCHAR(64) NOT NULL,
 visible BOOLEAN NOT NULL,
 actualizado_en DATETIME(6) NOT NULL,
 actualizado_por BIGINT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_plataforma_contacto_unica CHECK (id = 1),
 CONSTRAINT fk_plataforma_contacto_actor FOREIGN KEY (actualizado_por) REFERENCES plataforma_usuario(id)
);
INSERT INTO plataforma_contacto(id,correo,correo_legal,telefono,dias,hora_inicio,hora_fin,zona_horaria,visible,actualizado_en)
VALUES(1,'soporte@refluora.com','contacto@refluora.com','+522213688033','[1,2,3,4,5]','09:00','17:00','America/Mexico_City',TRUE,UTC_TIMESTAMP(6));
