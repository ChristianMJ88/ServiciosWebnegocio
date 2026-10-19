# 011 — revisión de cierre antes de actualizar colecciones

Solo lectura. No leer .env, archivos privados ni propiedades de XML con variables del entorno.

A1/M1: cabecera tenant única, eliminada de rutas internas; excepción de salud acotada a GET localhost:8080/18080/actuator/health. Ocho regresiones de guardas pasan. A2: asignación de rol global/porempresa/sucursal exige1fila; fixtures copian rol_empresa/rol_empresa_permiso del mismo catálogo del onboarding. Login comprueba rol y permisos efectivos no vacíos. Staff tiene GETagenda positivo.

M2: `node scripts/test-postman-guards-cli.cjs` ejecutado con CLIreal, exit0: exactamente1GETmarcador y0peticiones protegidas tras marcador DATABASEagenda_db. Callback skipRequest funciona. --no-report-events confirmado en ayuda1.75 y utilizado en todas las corridas; reportes localesdesdearchivo.

M3: captura refreshcookie antes del logout y lo reenvía expresamente, esperando401 por tokenrevocado; unGETperfil documenta JWTacceso aún vigente. M4: estados fijos y códigoERROR_NEGOCIO en negativas, ANTICIPO_NO_DISPONIBLE específico; casos comprueban precios/saldos/versiones/noescritura. M5: entorno de proceso porlista permitida, perfilapi-aislado sin.env, MYSQL_PWD solo clienteMySQL, proxy HTTP(S) JVMlocalhost9, jobs/correo/WhatsApp/Stripeoff;Pointfakeúnico proveedorinvocado.

M6: MP verifica users/me paraTESTyAPP_USR; APP_USRademástagtest_user. Escriturasexigen epochfuturo≤120s, vacíoenrepo y soloCLIporcorrida; nada persistente. Setters de scripts cambiados a pm.variables.set. NingunapeticiónexternaMPejecutada.

B1 corregido sincabeceraAenloginB. B3 envatómico600. B4 classpathMavenactual. B5 comparacitaajena/inexistente. B6 ventacruzada409segúncontrato,Pointrealdeotrotenant404. Se agregaPointmulticita. B2reglacheck-infuturoactual: no se modifica negocio; alcance de esta colección acredita el comportamiento existente, no decide restricción nueva.

Importante: la primera negativa 'recepcionnoabreCaja' eraunaasunciónincorrecta: catálogo rol_permiso actual SÍconcedeCAJA_SESION_GESTIONAR aRECEPCIONISTA. Se sustituyó por GETpagos/configuracion(noautorizado),sin cambiarpermisos. MétodoPoint seobtienedesdeTerminalPointResponse.metodoPago, no del catálogo manual(diseñoactual).

Últimacorrida95peticiones: reglasdenegocioenverde,1errorenunscriptdecapturacookieaplicadoporerroralcaso401. Corregido; corrida99finalencurso, nosedeclaraaprobadaantesderesultado. Revisarcolecciones/arnés/seguridad; pendientecomparaciónremotaantesdelPUT. No commit011/008, pusho despliegue.
