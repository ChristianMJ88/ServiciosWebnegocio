INSERT INTO usuario (empresa_id, correo, contrasena_hash, habilitado, bloqueado)
VALUES (1, 'staff.demo@agenda.local', '$2a$10$2BfQzK4xL2K5m5VY9Q0k6uQJwN8o7N6q4gG0gE9i5LwA0eP4xYp5a', TRUE, FALSE);

INSERT INTO prestador_servicio (usuario_id, sucursal_id, nombre_mostrar, biografia, activo, color_agenda)
VALUES ((SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 1, 'Prestador Demo', 'Prestador inicial de pruebas', TRUE, '#2563eb');

INSERT INTO usuario_rol (usuario_id, rol_id, empresa_id)
VALUES (
    (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1),
    (SELECT id FROM rol WHERE codigo = 'STAFF'),
    1
);

INSERT INTO asignacion_servicio_prestador (prestador_id, servicio_id, activa)
SELECT
    (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1),
    s.id,
    TRUE
FROM servicio s
WHERE s.empresa_id = 1 AND s.sucursal_id = 1;

INSERT INTO regla_disponibilidad (empresa_id, tipo_sujeto, sujeto_id, dia_semana, hora_inicio, hora_fin, intervalo_minutos)
VALUES
    (1, 'SUCURSAL', 1, 1, '09:00:00', '19:00:00', 15),
    (1, 'SUCURSAL', 1, 2, '09:00:00', '19:00:00', 15),
    (1, 'SUCURSAL', 1, 3, '09:00:00', '19:00:00', 15),
    (1, 'SUCURSAL', 1, 4, '09:00:00', '19:00:00', 15),
    (1, 'SUCURSAL', 1, 5, '09:00:00', '19:00:00', 15),
    (1, 'SUCURSAL', 1, 6, '09:00:00', '16:00:00', 15);

INSERT INTO regla_disponibilidad (empresa_id, tipo_sujeto, sujeto_id, dia_semana, hora_inicio, hora_fin, intervalo_minutos)
VALUES
    (1, 'PRESTADOR', (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 1, '09:00:00', '18:00:00', 15),
    (1, 'PRESTADOR', (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 2, '09:00:00', '18:00:00', 15),
    (1, 'PRESTADOR', (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 3, '09:00:00', '18:00:00', 15),
    (1, 'PRESTADOR', (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 4, '09:00:00', '18:00:00', 15),
    (1, 'PRESTADOR', (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 5, '09:00:00', '18:00:00', 15),
    (1, 'PRESTADOR', (SELECT id FROM usuario WHERE correo = 'staff.demo@agenda.local' AND empresa_id = 1), 6, '09:00:00', '15:00:00', 15);

