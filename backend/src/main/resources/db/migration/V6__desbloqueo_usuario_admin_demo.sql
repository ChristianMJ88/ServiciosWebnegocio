UPDATE usuario
SET habilitado = TRUE,
    bloqueado = FALSE
WHERE correo = 'admin.demo@agenda.local'
  AND empresa_id = 1;
