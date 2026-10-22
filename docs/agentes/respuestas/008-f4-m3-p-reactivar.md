# Reactivación de P y repetición de P4

Codex, 21 de octubre de 2026. El usuario reconsidera el retorno a CSR y autoriza reactivar P conservando 012 y repetir P4 alternado 5 + 5, sin procesos de trabajo simultáneos, commit ni despliegue hasta su revisión.

## Implementación y comprobaciones previas

Restauración por fragmentos: un solo perfil marketing con servidor, outputMode static y tsconfig de servidor; el wrapper diagnóstico usa ese mismo perfil. Se conserva la protección contra salidas anidadas o contenedoras de la carpeta productiva. El manifiesto declara prerender y la guarda lo exige; postbuild y predeploy vuelven a exigir P. La guarda CSR sigue como herramienta para artefactos archivados, con su propia batería negativa. No se modificaron backend, migraciones, planes, permisos, datos ni código de 012.

Claude revisó la implementación en lectura en revisiones/008-f4-m3-p-reactivar.md: sin bloqueantes. R1 documental corregido; R2 (carpeta server fuera de browser) solo observado. No se hace el ajuste cosmético R3.

Build completo y guardas aprobados. Node: 189/189; focales: 82/82. La huella de navegador del candidato reactivado es exactamente da5fbb28f3934f98184b9dbce5a350dd5a10f15af3c4daf4260aab9ae3be16d9, igual al P anterior; la nueva serie no hereda su mediana de 82. Baseline CSR archivado antes de P: 86a0c01127aef407222cf499f367827fb426dc0d461b92f3ee31364e83085b21, verificado de nuevo sin reconstruirlo. Marca clientes 5a0096a2e059085218532bfe89e869b0d6ca41d96886fce8da21bfbe5e86a2fb. La igualdad del navegador permite conservar como evidencia del mismo artefacto la matriz P3 y el metafile previos, sin presentarlos como ejecuciones nuevas.

La verificación ejecutada por Claude en 008-f4-m3-p-012-verificacion completó tres suites Angular CSR sin los fallos de entorno anteriores. No se atribuyen a esta reactivación P; una nueva serie normal se prepara antes de medir, sin modificar límites de tiempo ni workers.

## Regla confirmada por Claude

La mediana Performance de P debe ser al menos 85. Cada una de sus cinco corridas debe cumplir A/BP/SEO 100, CLS ≤0.01, LCP en h1#home-title o el lead y Header ausente. Se informa también el CLS de las cinco corridas CSR; una anomalía de control se registra sin convertirla en aprobación o rechazo de P. Si P falla cualquier condición, decide el usuario, sin microajustes. Incluso si pasa, solo acredita el preview local, no Hosting real.

Se alternará AB/BA/AB/BA/AB con un warmup por artefacto separado de las diez corridas medidas. Un error de API/auditoría aborta y conserva la serie; cualquier repetición será completa, sin mezclar muestras. Se conservarán la serie anterior y la decisión histórica CSR, con el nuevo resultado en un bloque independiente. Se registrarán versiones, huellas y alimentación/configuración/térmica antes y después, sin inferencias causales por la batería.

## Estado previo a la medición

La sonda de hidratación se detuvo antes de abrir navegadores: API pública no disponible. 8080 tiene listener Java, pero salud y soporte agotan timeout; el proxy de 4200 tampoco obtiene soporte. Ninguna corrida P4 nueva iniciada, ni frontend pausado. Se solicitó reiniciar el backend habitual desde IntelliJ. No se levantó un segundo backend ni se modificó el existente.

Las condiciones operativas N1/N2 de 012 siguen siendo previas a cualquier despliegue y no se amplían en esta repetición. No se creó sitio, se conectó dominio ni se consultó producción. Todos los cambios siguen sin commit.

### Suites previas terminadas

Tres corridas Angular normales consecutivas con P reactivado: 387/387 pruebas y 92/92 archivos, 48.74 / 53.99 / 63.79 s, exit0. No se modificaron workers, tiempos ni asserts; no hubo ENOSPC. Logs privados fluora-p-reactivar-angular-1/2/3.log. No quedan suites ni Claude ejecutándose antes de P4.

Tras el aviso de reinicio del backend, el primer control cambió de timeout a ECONNREFUSED y 8080 no tenía listener. Se concede una espera de arranque acotada antes de pedir otro dato; ninguna corrida Lighthouse nueva se ejecutó sobre ese estado.

La espera acotada de seis comprobaciones no recuperó soporte; al finalizar no había listener en8080. Se solicita el error final de IntelliJ o confirmar que terminó el arranque. La API no se sustituye por fixtures en P4. El script exterior de medición ya verifica PID/comando/propietario/cwd del frontend antes de STOP y restaura CONT en trap EXIT/INT/TERM, con comprobación de estado/respuesta, sin tocar backend ni MySQL. No se ha ejecutado aún ese script. Cero corridas P4 nuevas.

## Recuperación del entorno local

El usuario informó que el reinicio no funcionaba. Se comprobó que agenda-mysql estaba detenido (Exited0), con su volumen original backend_agenda_mysql_data y puerto3302:3306. Se arrancó ese mismo contenedor: no se recreó base, volumen ni contenedor. La consulta JDBC local y de solo lectura confirmó142 migraciones,0fallos y V141/V142 aplicadas; el arranque posterior validó todas y no aplicó ninguna migración.

IntelliJ mostró bienvenida y un aviso de suscripción Ultimate vencida. Se abrió el proyecto existente para el diagnóstico, sin cambiar licencia, aceptar suscripciones ni alterar ajustes globales. La CLI con Java21 permite ejecutar el proyecto independientemente de ese aviso.

Se corrigió `.run/Backend Spring Boot (dev).run.xml`: conserva solo el perfildev y deja de sustituir DB/JWT y los argumentos JVM delpom. La comparación privada confirmó que el JWT fijo delrunner era distinto al.env; no se cambiaron valores de.env ni credenciales. La guarda XML nueva comprueba metaMaven/cwd/perfil, ausencia de overrides y UTCenpom. El archivo estaba limpio al comenzar y estaba versionado; la búsqueda histórica devolvió2aa9911. No se reescribió historial ni se afirma uso del literal fuera del entorno local.

Claude aprobó la corrección en008-f4-p4-arranque-local.md. Se comprobó la presencia de las claves necesarias de.env, sin valores en salida, y se limpiaron por nombre los overrides del entorno del proceso. La bateríaNode final incluye esta guarda:190/190. La corrección no cambia aplicación, backend de negocio,012 ni reglas/permisos.

Durante el diagnóstico apareció otra instanciaJava en8080: el intento temporal terminó con puertoocupado y se conservó su log. Se verificaron salud/soporte/sitio200 y la página NailArt cargada en la pestaña original, sin crear reservas ni pagos. Después se preparó la instancia controlada de medición, sin tareasprogramadas ni DevTools. El primer método de pasar JVMarguments desde Maven no activó la propiedad porque elpom ya configuraba ese parámetro; se corrigió solo el entorno del launcher temporal con JAVA_TOOL_OPTIONS para los dos flags técnicos. Verificación real: UTC, spring.devtools.restart.enabled=false y0hilos scheduling. No se cambió.env.

## Preflight de navegador pendiente; ninguna corrida P4 nueva

La sonda posterior a recuperarAPI falló al abrir el Chrome aislado (puerto de depuración no disponible). Se conservó ese intento. La repetición con4200 pausado alcanzó el WebSocket deDevTools, pero su transporteHTTP no quedó listo dentro del límite existente. No se cambió timeout, versión, perfil ni código del sondeo para obtener éxito. El script abortó antes deLighthouse y restauró4200 aR, comprobando respuestaHTTP.

Evidencia privada: /private/tmp/fluora-p-p4-repeat-U0DSx4te, sinrun.log ni corridas Lighthouse. Es un intento de preparación, no una serie5+5 incompleta ni un resultado dePerformance. Power-before registró batería56%descargando y lowpowermode=1 tanto enbatería como enAC; térmica sin avisos registrados. No se atribuye el fallo deChrome a la alimentación. Se solicita al usuario conectar cargador, quitar temporalmente bajo consumo y dejar laMacdesbloqueada para repetir en condiciones controladas, sin cambiar esos ajustes por nuestra cuenta.

## P4 repetida: resultado completo para revisión

El usuario confirmó cargador conectado y bajo consumo desactivado. Se verificó AC Power y lowpowermode de corriente en 0 antes y después; batería pasó de 55% a 56%, cargando. La configuración de batería permaneció en 1, sin uso durante la serie. No se cambiaron ajustes globales ni se atribuye causalidad a esas condiciones. Sin avisos térmicos registrados.

La sonda del artefacto P pasó en 375×812, 820×1180 y 1440×900: DOM reutilizado, hidratación/replay presentes, sin errores de consola; CLS .001519 / .006663 / .001723. A continuación se ejecutó una única serie completa AB/BA/AB/BA/AB, Lighthouse 13.4.1, perfil móvil 375×812 y simulate, sin builds, suites ni revisiones de Claude simultáneos; había dos procesos CLI de Claude inactivos presentes. API y MySQL reales disponibles, backend controlado sin tareas programadas ni reinicio DevTools. 4200 se pausó con comprobación de identidad/estado T y se restauró mediante trap a R y respuesta HTTP. Los previews 4173/4174 terminaron.

| Resultado | CSR archivado | Prerender P |
| --- | --- | --- |
| Performance, cinco corridas | 82, 83, 83, 82, 82 | 88, 85, 81, 75, 87 |
| Mediana Performance | 82 | **85** |
| Mediana LCP | 3819.91 ms | 3693.17 ms |
| Mediana CLS | .00193748 | .00977409 |
| Warmup, separado de la mediana | 81 | 86 |

P cumple los siete criterios de la revisión: mediana ≥85, A/BP/SEO 100 en cada corrida, CLS ≤.01, LCP h1#home-title y ausencia de Header. Las diez muestras también cumplen CLS. No se descarta la corrida P de 75 ni se repite para mejorar la cifra. CLS de P conserva un margen estrecho respecto al límite: no equivale a holgura garantizada. Las huellas antes/después son idénticas a las verificadas; no se heredan puntuaciones previas.

La primera lectura del resumen no encontró el nodo porque lcp-discovery-insight existe sin details cuando el LCP es texto. Se corrigió únicamente el extractor temporal para recorrer también lcp-breakdown-insight; los cinco informes originales contienen el h1. No cambió el artefacto ni se repitió Lighthouse.

Evidencia portable: evidencias/008-f4-m3-p4-repeticion-resumen.json. Informes completos privados en fluora-p-p4-repeat-vMKCD0FI. Se conservan sin sobrescribir baseline, prerender y userDecision históricos, y los intentos de preparación fallidos. **P4 acredita la meta solamente en preview local**. 012 se conserva y sus condiciones operativas N1/N2 siguen previas al despliegue. Sin commit, sitios nuevos, DNS real ni despliegue; se detiene para revisión del usuario.

## Revisión final y restauración comprobada

Claude confirma en revisiones/008-f4-m3-p4-repeticion-cierre.md que P4 cumple localmente, sin bloqueantes. H1/H2 documentados: no hubo revisiones de Claude en curso, pero había dos CLI inactivos; la instantánea previa también registró carga de fondo de mysqld de Homebrew (43.1%, distinto del MySQL Docker requerido), syspolicyd (69.5%), mds_stores (21%) y mediaanalysisd (20%). No se afirma que la Mac estuviera sin procesos ni se atribuye causalidad; no se usan estas observaciones para repetir ni descartar muestras. No se detuvieron servicios ajenos.

H3: el resumidor calculó las huellas después de la serie y su aserción de igualdad pasó, verificado en consola. No se persistió una instantánea independiente de hash posterior; se registra esa limitación sin recalcular ahora sobre dist. H4: method y su observación de batería del resumen anterior son históricos; methodScope lo explicita y remite a p4Repeat para esta serie.

La mediana85 está justo en el umbral y P osciló75–88; el CLS de cuatro corridas .00977409 deja .000226 de margen. Esto requiere vigilancia ante cambios futuros, sin microajustes ahora. Los dos warmups y todas las muestras se conservan.

Backend normal restaurado en8080/PID49912, UTC y DevTools habitual, sin override de desactivación; un hilo scheduling confirmado. Salud, soporte, planes y proxy4200 devolvieron200; la ruta pública NailArt con Accept HTML devolvió200. La primera sonda HTTP genérica de esa ruta dio404 por negociación de navegación, conservada como diagnóstico y verificada después con la cabecera de documento. 142 migraciones validadas, ninguna aplicada. Frontend original4200/PID82535 activo;4173/4174/4300 libres. El backend se deja funcionando para el usuario; log privado fluora-backend-regular-restored.log. git diff --check sin errores. No se repitieron suites después de cambios únicamente documentales.

**Cierre local de P4 listo para revisión del usuario; sin commit ni despliegue.** La autorización vigente exige detenerse aquí.
