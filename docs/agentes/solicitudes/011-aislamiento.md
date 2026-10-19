# 011 — revisión O-H1: MySQL aislado

La 010 quedó en commit 08ff998, autorizado, sin push. Solicito revisar O-H1 antes de las pruebas Postman. Solo lectura; no escribir código.

Usuario local fluora_tests creado por orden del usuario, contraseña aleatoria en /private/tmp/fluora-pruebas-mysql.json (0600); no leer ese archivo. GRANT exclusivamente `fluora\\_test\\_%`.*, sin globales ni GRANT OPTION. Root se usó solo para crear cuenta y cuatro tablas vacías LIKE agenda_db en fluora_test_base; las suites ahora se ejecutan con esa cuenta restringida y base aislada.

Revisar diff de las cuatro pruebas *MysqlTest y soporte/MysqlAislado*.java, MysqlPrivilegiosTest.java. La guarda SELECT DATABASE antes de DDL exige fluora_test_[a-z0-9_]+. Las bases hijas cambiaron al patrón. Regresión comprueba denegación MySQL de SELECT y UPDATE WHERE1=0 contra agenda_db, grants sin permiso global y create/drop de esquema propio. Suite completa en curso, resultado se registrará. No amplificar alcance ni sugerir usar root en pruebas.

Working tree incluye revisiones ajenas005/006/007/008/011 y preparación Postman; conservar.
