-- Ejecutar sin escrituras concurrentes de citas. Producción requiere antes el ensayo A1 sobre copia.
-- Las únicas modificaciones a cita son folio y su restricción final NOT NULL.
START TRANSACTION;
CREATE TEMPORARY TABLE tmp_007_huella AS SELECT id, SHA2(CAST(JSON_ARRAY(id,empresa_id,sucursal_id,servicio_id,prestador_id,cliente_id,estado,inicio,fin,precio,precio_lista,descuento,promocion_id,promocion_snapshot_json,moneda,notas,creada_por_usuario_id,creada_en,cancelada_en,motivo_cancelacion,reprogramada_desde_id,check_in_en,check_in_por_usuario_id,pago_expira_en,reserva_grupo_id,politica_cobro,importe_requerido,politica_version_id,politica_aceptada_en) AS CHAR),256) AS huella FROM cita;
CREATE TEMPORARY TABLE tmp_007_reserva (
 empresa_id BIGINT NOT NULL, clave VARCHAR(64) NOT NULL, miembros INT NOT NULL, base CHAR(8) NULL,
 PRIMARY KEY (empresa_id,clave), UNIQUE KEY (empresa_id,base)
);
INSERT INTO tmp_007_reserva (empresa_id,clave,miembros)
 SELECT empresa_id, COALESCE(reserva_grupo_id,CONCAT('CITA:',id)), COUNT(*) FROM cita WHERE folio IS NULL
 GROUP BY empresa_id,COALESCE(reserva_grupo_id,CONCAT('CITA:',id));
CREATE TEMPORARY TABLE tmp_007_cita (cita_id BIGINT PRIMARY KEY,empresa_id BIGINT NOT NULL,clave VARCHAR(64) NOT NULL,ordinal INT NOT NULL);
INSERT INTO tmp_007_cita
 SELECT id,empresa_id,COALESCE(reserva_grupo_id,CONCAT('CITA:',id)),
 ROW_NUMBER() OVER (PARTITION BY empresa_id,COALESCE(reserva_grupo_id,CONCAT('CITA:',id)) ORDER BY id)
 FROM cita WHERE folio IS NULL;
SET @folio_007_alfabeto = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
UPDATE IGNORE tmp_007_reserva SET base = CONCAT(SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1)) WHERE base IS NULL;
UPDATE tmp_007_reserva r SET r.base = NULL WHERE r.base IS NOT NULL AND EXISTS
 (SELECT 1 FROM cita c WHERE c.empresa_id=r.empresa_id AND (c.folio COLLATE utf8mb4_bin=CONCAT('FL-',r.base) COLLATE utf8mb4_bin OR c.folio COLLATE utf8mb4_bin LIKE CONCAT('FL-',r.base,'-%') COLLATE utf8mb4_bin));
UPDATE IGNORE tmp_007_reserva SET base = CONCAT(SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1)) WHERE base IS NULL;
UPDATE tmp_007_reserva r SET r.base = NULL WHERE r.base IS NOT NULL AND EXISTS
 (SELECT 1 FROM cita c WHERE c.empresa_id=r.empresa_id AND (c.folio COLLATE utf8mb4_bin=CONCAT('FL-',r.base) COLLATE utf8mb4_bin OR c.folio COLLATE utf8mb4_bin LIKE CONCAT('FL-',r.base,'-%') COLLATE utf8mb4_bin));
UPDATE IGNORE tmp_007_reserva SET base = CONCAT(SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1)) WHERE base IS NULL;
UPDATE tmp_007_reserva r SET r.base = NULL WHERE r.base IS NOT NULL AND EXISTS
 (SELECT 1 FROM cita c WHERE c.empresa_id=r.empresa_id AND (c.folio COLLATE utf8mb4_bin=CONCAT('FL-',r.base) COLLATE utf8mb4_bin OR c.folio COLLATE utf8mb4_bin LIKE CONCAT('FL-',r.base,'-%') COLLATE utf8mb4_bin));
UPDATE IGNORE tmp_007_reserva SET base = CONCAT(SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1)) WHERE base IS NULL;
UPDATE tmp_007_reserva r SET r.base = NULL WHERE r.base IS NOT NULL AND EXISTS
 (SELECT 1 FROM cita c WHERE c.empresa_id=r.empresa_id AND (c.folio COLLATE utf8mb4_bin=CONCAT('FL-',r.base) COLLATE utf8mb4_bin OR c.folio COLLATE utf8mb4_bin LIKE CONCAT('FL-',r.base,'-%') COLLATE utf8mb4_bin));
UPDATE IGNORE tmp_007_reserva SET base = CONCAT(SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1),SUBSTRING(@folio_007_alfabeto,(ASCII(RANDOM_BYTES(1)) & 31)+1,1)) WHERE base IS NULL;
UPDATE tmp_007_reserva r SET r.base = NULL WHERE r.base IS NOT NULL AND EXISTS
 (SELECT 1 FROM cita c WHERE c.empresa_id=r.empresa_id AND (c.folio COLLATE utf8mb4_bin=CONCAT('FL-',r.base) COLLATE utf8mb4_bin OR c.folio COLLATE utf8mb4_bin LIKE CONCAT('FL-',r.base,'-%') COLLATE utf8mb4_bin));
CREATE TEMPORARY TABLE tmp_007_guardia (diferencias BIGINT NOT NULL CHECK (diferencias=0));
INSERT INTO tmp_007_guardia SELECT COUNT(*) FROM tmp_007_reserva WHERE base IS NULL;
UPDATE cita c JOIN tmp_007_cita o ON o.cita_id=c.id
 JOIN tmp_007_reserva r ON r.empresa_id=o.empresa_id AND r.clave=o.clave
 SET c.folio=IF(r.miembros>1,CONCAT('FL-',r.base,'-',o.ordinal),CONCAT('FL-',r.base)) WHERE c.folio IS NULL;
INSERT INTO tmp_007_guardia SELECT COUNT(*) FROM cita WHERE folio IS NULL;
CREATE TEMPORARY TABLE tmp_007_posterior AS SELECT id, SHA2(CAST(JSON_ARRAY(id,empresa_id,sucursal_id,servicio_id,prestador_id,cliente_id,estado,inicio,fin,precio,precio_lista,descuento,promocion_id,promocion_snapshot_json,moneda,notas,creada_por_usuario_id,creada_en,cancelada_en,motivo_cancelacion,reprogramada_desde_id,check_in_en,check_in_por_usuario_id,pago_expira_en,reserva_grupo_id,politica_cobro,importe_requerido,politica_version_id,politica_aceptada_en) AS CHAR),256) AS huella FROM cita;
INSERT INTO tmp_007_guardia SELECT COUNT(*) FROM tmp_007_huella a LEFT JOIN tmp_007_posterior b ON a.id=b.id WHERE b.id IS NULL OR a.huella <> b.huella;
INSERT INTO tmp_007_guardia SELECT ABS((SELECT COUNT(*) FROM tmp_007_posterior)-(SELECT COUNT(*) FROM tmp_007_huella));
DROP TEMPORARY TABLE tmp_007_huella, tmp_007_posterior, tmp_007_guardia, tmp_007_reserva, tmp_007_cita;
COMMIT;
ALTER TABLE cita MODIFY COLUMN folio VARCHAR(16) NOT NULL;
