# O-H1 — cierre de M1/M2

Solo lectura. Revisar MysqlAislado.java, MysqlAisladoTest.java, MysqlPrivilegiosTest.java y CobrosConcurrentesMysqlTest.java actuales. M1 ahora lista permitida exacta (USAGE global o privilegios exclusivamente fluora\\_test\\_%, sin GRANT OPTION), CURRENT_ROLE()=NONE, y la validación está en la guarda común antes de DDL. M2 deriva URL hija comprobada (requiere esquema explícito, rechaza catalog/database), abre conexión y exige catálogo igual al generado antes de construir SessionFactory(create-drop). Unitarios incluyen grants globales/roles/otraBD y URL sin base/ruta; sin casos que retornan sin aserción. Última suite completa después de estas correcciones exit0 (ver XML target/surefire-reports); pruebas MySQL ejecutadas con fluora_tests, no root. No leer archivos privados o .env.

Arnés Postman en elaboración todavía no se considera cerrado; esta revisión solo cierra O-H1. No tocar revisiones ni aplicación.
