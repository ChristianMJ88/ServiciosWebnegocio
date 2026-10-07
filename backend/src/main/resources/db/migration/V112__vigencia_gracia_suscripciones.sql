ALTER TABLE plataforma_suscripcion ADD COLUMN gracia_hasta DATETIME(6) NULL;

-- Conserva la ventana original de los negocios que ya estaban en mora.
UPDATE plataforma_suscripcion s
JOIN plataforma_plan p ON p.id=s.plan_id
SET s.gracia_hasta=DATE_ADD(s.actualizada_en, INTERVAL p.dias_gracia DAY)
WHERE s.estado='GRACIA' AND s.gracia_hasta IS NULL;
