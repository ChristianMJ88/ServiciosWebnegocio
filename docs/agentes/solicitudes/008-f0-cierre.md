# 008 F0 — correcciones y cierre de código

Solo lectura. Leer revisión008-f0-implementacion. Codex corrigió:
- A1: Footer empieza no elegible hasta NavigationEnd, salvo router.navigated ya verdadero. Spec router.url='/' sin navegación no solicita; tras NavigationEnd pide, /e/x y host propio no. Tenantcontactoconservado.
- M1: proceso propio25739 detenido antes de compilar para evitarrecarga. Consulta SQL habitual demuestraFlyway139 success1, sinV140. Solo BD efímera fluora_test_008 conusuariorestringido aplicóV140. Antes de reactivarhabitual se registrará explícitamente laaplicaciónlocal aprobada deV140; sin producción.
- M2: ContactoSeguridadTest usa ConfiguracionSeguridad y FiltroAutenticacionJwt reales, JWT firmado real, repositoriousuariomock; tenantinclusoconmismopermiso403, plataforma sinpermiso403, conpermiso200, anónimo401; público igualcontenantysin. Home spec200/404/hostpropio.
- M3: añadidosGETlegal/soportepúblico y administración403 acolecciónlocalguardada. RunnerFlywaytrue únicamente despuésdecomprobarnombre/grants/copiaguardada; fullPostmanencurso.
- B1: quitado%de regex y caso%3fbcc pruebarechazo.
- B2: advice limitado aControladorSoportePlataforma, cuerpoJSON/horailegible400 sintraza; prueba.
- B3: MySQLusa ServicioAuditoriaPlataforma real y JpaRepository real, tablaauditconFK y mapeovalidate; solorepositorylecturabarrier añadeinterceptor paraasegurarcarrera. DosTXmismaVersion200/409 y1audit.
- B5: editoravisaActualizar descartaediciónnoguardada.

Evidencia final de código: backend621pruebas/620aprobadas/1omitidacorreoreal,0fallos/errores,157suites,exit0. TodoslosMySQLsinomitir. Angular210/70,exit0,1workerconfigtemporal, sinampliartimeouts. GuardasPostman8/8. BuildAngularcorrecto(warningspreviosajenos). Revisión visual yPostman finalencurso, no afirmar completos.

Confirma correcciones y cobertura, con limitaciones expresas. Revisa también la nueva configuración del runner(únicamente copia fluora_test_011_*) ycontratosGETPostman. NoescribirappniarchivosdeCodex. Sincommit/push/despliegue.
