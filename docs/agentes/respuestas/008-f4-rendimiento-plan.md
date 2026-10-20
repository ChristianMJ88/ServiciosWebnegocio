# 008 F4 — ejecución del plan de rendimiento

Autor: Codex,2026-10-19. Encargo: commit de lo aprobado → M0 → M1 → M2, con medición/revisión por paso; M3 requiere decisión posterior del usuario. Sin push ni despliegue.

## Punto de retorno y procesos

- 011: `798137b`,28archivos de aislamiento MySQL/colecciones/arnés/documentación.
- 008: `e642571`,163archivos de soporte editable y experiencia pública revisada. F4 no se declaró cerrada por ese commit.
- 191candidatos escaneados antes del staging: sin patrones de claves privadas ni coincidencias con secretos locales. No entraron .env,dist/logs,.postman local ni revisiones ajenas005/006/007/AGENTS. `.postman/` ignorado. Tres espacios finales de licencias OFL normalizados sin cambiar texto; diff staged limpio.
- Detenidos preview4300 y mi backend8080/PID21152.4400/18080 ya libres. Después el usuario confirmó usar sus procesos activos4200/8080; se conservan. Las mediciones usan un preview comprimido4173 que termina en finally; no se mide ng serve. Para comparaciones UI se usa el mismo preview solo temporalmente, con GET/HEAD públicos y cotizar(readOnly) opcional, sin reservar, pagar ni autenticar.

## M0

Tooling reproducible: `scripts/lighthouse-home.mjs`, `scripts/performance/public-preview.mjs` y README. Lighthouse13.4.1 instalado fuera del proyecto; Node22.21.1 solo para CLI (Node20 del proyecto conservado). Chrome headless con perfil nuevo, móvil375×812, simulate150ms/1.6Mbps/CPU×4. Mac enAC100%, sin cambiar preferencias de Chrome personal. API pública real local; no se inventan respuestas/precios. Capturas de marketing locales no ejecutan las Functions de hosting; gzip, no brotli. Rutas de autenticación excluidas (el origen app del preview coincide con marketing).

Primera serie62/77/84/83/83, mediana83; A/BP/SEO100, CLSmáx.000706, Header ausente5/5. Claude revisó en `revisiones/008-f4-m0.md` y pidió corregir procedimiento antes de comparar. Se añadieron precompresión antes de listen, calentamiento de GET reales y una corrida fija aparte; redondeo de scores/null rechazado; chunkHeader único exigido; huella recursiva, dirty, ruta relativa, bytes por tipo/tenantCSS y timeout escalado. La copia previa está en `tmp/builds/m0` y `/private/tmp/fluora-008-m0-browser` (artefactos ignorados/sincredenciales).

Serie corregida, huellaSHA256 `15dbe1b31239bb01d1c2383d49d6aad485bff0a322535dc71d845e5a58682a01`: calentamiento74 separado; corridas51/85/83/82/87, **mediana83**. A/BP/SEO100, medianasFCP3018ms/LCP3628ms/TBT0/CLS.000706/SI3319ms. Una muestra tuvo CLS.014787; no se oculta y no cumple el criterio de CLS de cierre para todas las corridas. No comparar con el81/62 histórico de DevTools. Informes locales ignorados `tmp/lighthouse/m0-corrected/`. Aún hay variación; las comparaciones M1 serán alternadas contra la copia M0 usando `--compare-root`.

## M1 en verificación

Inventario de consumidores estáticos/dinámicos; ningún componente público del tenant importa Material. Hoja marketing con reboot original, subconjuntos Bootstrap/Icons generados con PostCSS8.5.6 fijado (la versión transitiva ya instalada), fuentes/tokens y base pública. La hoja `styles.css` de application no cambió. CSS generado conserva reglas/envoltorios/orden y keyframes usados, más todos los tokens TS que coinciden con clases; check reproducible y de deriva de public-base/token Material. Bordes de Booking conservan el token outline-variant extraído de azure-blue.

`tenant-public.css` global no inyectada, sin hash en webpack; conserva Bootstrap/Icons completos, base pública/fuentes/tokens. Servicio idempotente solo provisto en main.marketing, link detrás del stylesheet global y delante de estilos de componentes; resolver espera su carga, error permite navegación/reintento. Application/tests usan proveedor no-op. App reutiliza la decisión existente de contexto/host. **Ajuste respecto al pseudocódigo de Claude:** la compuerta ready queda dentro del defer para descargar Header en paralelo con CSS, sin pintarlo antes de que CSS termine; ponerla por fuera retrasaría también su descarga. Footer/overlays esperan ready. No se duplican permisos, precios o capacidades.

Build marketing212554dd89383177,exit0: inicial569.89kB/147.07kB estimados; stylesheet42.47kB/8.95kB frente a362kB; tenantCSS321.82kB/35.98kB fuera del inicial. SuiteAngular262/79 verde,exit0, un worker temporal/sin ampliar timeouts; guards de assets/deriva verdes. Un arranque de suite junto a build se interrumpió de inmediato (exit130) y se ejecutó completa después del build. Applicationbuildexit0; CSS idéntico byte a byte a M0:SHA256 `0e87672435ffa43c0034795cac5a4982584acb20d5fd4b9085867e40a2814aab`, mismo nombre `styles.ae85c1cbd23cd712.css`.

Capturas/datos de referencia M0 en `/private/tmp/fluora-008-m0-ui/`: inicio, servicios, productos, reservas y checkout375×812/820×1180/1440×900, sin overflow. Carrito de referencia solo local en4173, un producto real, sin datos personales ni envío de pedido; carrito4200 intacto. Comparación M1 en curso; se distinguen diferencias por transiciones de scroll-reveal/resize antes de atribuirlas a CSS. No se afirma equivalencia completa antes de finalizar.

Pendientes: revisión/capturas/medición alternadaM1, después M2, P2frío/caliente, matriz16 del home y estados de marketing. D1 de Claude (cache del archivo tenant sin hash) debe resolverse antes de un despliegue; no se modificó hosting. Host propio físico/iOS no disponibles. Avisos de ng serve añadidos por Claude: ECONNREFUSED transitorio de reinicio, Java27 del backend del usuario distinto de21 validado, NG8107 requiere tipo honesto y prueba de lista vacía; no se altera ni detiene el runtime del usuario por iniciativa propia. DEP0060 se valida al pasar dev-server enM2.

## M1: verificaciones y medición completadas

Corregidos los hallazgos de m1-codigo/m1-correcciones: keyframes Bootstrap, límite de espera sin duplicar links, reintento sin desmontar Header, inserción tras global incluso si es último hijo y procedencia SHA256 del medidor. Angular completo: **265 pruebas/79 archivos**, exit0. Cobertura de clases runtime pasa **15 rutas/estados**; guardas CSS/assets/builds verdes. Builds finales marketing `b32ac83480b72514` y application `0ca4a48a6bbe29c3`, exit0.

Comparación alternada AB/BA/AB/BA/AB, mismos scripts/perfil/API real: M0 **82/82/85/83/83, mediana83**; M1 **81/85/79/84/85, mediana84**. A/BP/SEO100 en todas, CLS máximo.000707, sin Header ni tenantCSS en Home. Medianas M0→M1: FCP3310→3154ms, LCP3674→3604ms, TBT0→103ms. No se afirma mejora causal general ni meta85 alcanzada. Warmups83/83 separadas, A primero3veces/B2veces. Informes ignorados `tmp/lighthouse/m1-paired`; huellas M0 `15dbe1b31239bb01d1c2383d49d6aad485bff0a322535dc71d845e5a58682a01`, M1 `bf2df20d06173dc73f874964df2b3fa80ef7a34ca1761c8d50013a9de0e038c7`. Previews4173/4174 cerrados en finally.

Revisión solicitada en008-f4-m1-medicion antes de M2. Las capturas con scroll/transiciones/carrito diferentes siguen sin acreditar equivalencia exacta; no se oculta ese límite.

## M2 implementado y comprobado

Claude008-f4-m1-medicion permitió comenzar M2. `angular.json`: builder `@angular/build:application`, `browser` en las tres entradas y `outputPath` objeto manteniendo `dist/{marketing,application,ServiciosWebnegocio}/browser`; serve pasa a `@angular/build:dev-server`. Index, assets, fileReplacements, budgets y estilos conservados. Sin SSR, hosting, backend ni actualización de dependencias. `@angular-devkit/build-angular` se conserva; retirarlo no forma parte del encargo.

Tres builds production exit0: marketing **472.40kB/129.43kB** estimados (M1:569.89/147.07), application **823.03/167.21**, default **846.22/176.12**. El primer intento dentro del sandbox terminó134 sin diagnóstico; el mismo build fuera del sandbox pasó. Sin avisos NG8107/CommonJS en las salidas finales. NG8107 corregido con `.at(0) ?? null`, conservando `?.`; prueba renderiza plantilla sin capacidades y exige h1/p vacíos, con cargas pendientes. **Angular266/80, Functions11/11, exit0**; check:public-builds/public-assets/marketing-css verdes. Se regeneró la plantilla SEO ignorada y se verificó runtime-config clásico antes de módulos y bootstrap de tenant antes de main.

El minificador cambia los bytes de CSS. Inventario compilado anterior/nuevo coincide: marketing196clases/2animaciones, tenant4142/5, application4317/5. `styles.css` original intacto. No se presenta este inventario como equivalencia de píxeles. Servidor Vite propio4175: GET del proxy público200 application/json y sinDEP0060, retirado con exit130.4200 del usuario conservado: **al reiniciarlo usará Vite**. Dominios no locales/túneles del frontend requerirían `allowedHosts` explícitos; no se habilitaron hosts arbitrarios.

UI M2: Home en16dimensiones/orientaciones sin overflow y tipografía mínima12px; cinco pantallas tenant (inicio/servicios/booking/productos/checkout) en375×812,820×1180,1440×900 cargadas y sin overflow, un linkCSS por documento. Carrito temporal de previews con producto real, sin cliente, reserva, cobro ni pedido;4200 intacto. Menú móvil abre, Escape devuelve foco y contrae; tablet→escritorio cierra menú. Capturas privadas en `/private/tmp/fluora-008-m2-ui/`. Comparación adicional de15pares de snapshots M0/M2, con11–26nodos muestreados por página y carrito igualado; se conservan las diferencias de estado/geometría en los datos crudos, sin declarar equivalencia exacta ni atribuirles una causa no demostrada. Pestañas propias cerradas y viewports restaurados.

Claude008-f4-m2-codigo no encontró bloqueantes M2; los tres medios del medidorP2 se corrigieron y Claude008-f4-p2-correcciones los cerró. P2 simula **solo localmente** D1 no-cache+ETag/304 paratenantCSS; no cambia hosting. Pruebas del preview2/2: política normal intacta, revalidación304 yPOSTauth405. Guardas verifican tenantCSS/API real, caché realmente fría y código caliente; métricas completas y procedencia compartida. Warmup descartada antes de cada tamaño, resumen aclara Lantern/simulación y una muestra por tamaño/cache. Huellas M0/M1 recalculadas por helper compartido coinciden con informes previos byte por byte. M2 yP2 en medición, sin builds/suites/revisiones simultáneas.

## M2: resultado de rendimiento

Serie alternada completa `tmp/lighthouse/m2-paired`: **M1 79/84/84/80/87, mediana84; M2 82/84/84/80/80, mediana82**. Warmups81/59 excluidas de las medianas y conservadas. A/BP/SEO100 en las diez muestras; CLS<=.000707, Header/tenantCSS ausentes en Home. Medianas M1→M2: FCP3156→3045ms, LCP3573→3678ms, TBT85→59ms. El paquete menor no demuestra una mejora global del score; rangos solapados y resultado insuficiente para85. No se afirma cumplimiento de la meta.

Huella M2 `41ddf9b3f7419ee38691a1394950eea723f6361f5329070341b6bd0a553a6f64`; ambos builds medidos con el mismo orquestador/perfil y backend real, sin CPU de builds/suites/revisiones concurrentes. Previews4173/4174 retirados en finally. **M3 no implementado:** según el plan, el usuario debe decidir entre diseñar/autorizar prerender (toca hosting/Functions) o aceptar el rendimiento alcanzado; no continuar con microajustes sin esa decisión. P2 del tenant en curso.

## Caché del tenant: diagnóstico y medición adicional del inicio

Los dos primeros intentos se invalidaron por un502 registrado durante la corrida fría, aunque su informe de red mostraba la petición actual200. Se corrigió el registro del preview: cada petición se incorpora **al iniciarse**, actualizando su resultado, para que una respuesta pendiente de otra navegación no entre en el intervalo siguiente. Regresión real con dos peticiones solapadas: **3/3 pruebas del preview**, exit0. Se conserva diagnóstico público sin cabeceras/cuerpos en `tmp/lighthouse/m2-tenant-cache-diagnostic/failed-run.json`; no usar esos intentos incompletos. Un arranque posterior se interrumpió antes de medir para asegurar que las pruebas hubiesen terminado; tampoco cuenta.

Serie adicional completa del **inicio** `/e/nail-art`, no confundir con el catálogo de P2: `tmp/lighthouse/m2-tenant-cache-scoped/summary.json`, exit0. Una muestra por tamaño/estado, calentamientos separados y D1simulado localmente. CLS frío/caliente:390×844 **.081328/.164032**;820×1180 **.122669/.121292**;1440×900 **.272168/.272398**. No cumple. A97/BP100/SEO100 en esa página; no extender el100deHome marketing al tenant. Caché caliente32–33recursos locales, CSStenant304 real y guardas verdes. Lighthouse señala principalmente `body > app-root > main.min-vh-100`; algunas entradas asocian carga de fuente de iconos, sin probar causalidad única ni regresión causada porM2. No existe una serieP2M0 comparable.

La revisión008-f4-header-cierre pide concretamente **Servicios**. Se ejecuta esa ruta por separado en `tmp/lighthouse/m2-services-cache`; las cifras del inicio no sustituyen P2del catálogo. Sin cambios de código de negocio ni parches visuales especulativos.

## P2 del catálogo: resultado final

`tmp/lighthouse/m2-services-cache/summary.json`, exit0 de **medición**, no de cumplimiento. Mismo buildSHA256 que HomeM2, API real, tres warmups separadas y una pareja fría/caliente por tamaño. Los seis casos cumplen las guardas de red/caché;34recursosJS/CSS calientes, CSStenant304. Perfil mobile/simulate también para viewportdesktop1440. D1solo simulado en preview, no hosting.

| Viewport | CLS frío | CLS caliente | Performance frío/caliente |
|---|---:|---:|---:|
|390×844|0.081198|0.081668|73/92|
|820×1180|0.060761|0.061449|66/63|
|1440×900|0.220519|0.220519|64/89|

TenantServicios A96/BP100/SEO100. **P2 no cumple CLS<=.01.** Lighthouse señala el contenedor principal; es evidencia de desplazamiento, no prueba de que M2 lo haya introducido ni autorización para inventar alturas de placeholder. Se conserva el riesgo descrito en008-f4-header-cierre. La equivalencia visual completa tampoco se certifica: comparación calculada final igualó carrito/viewport;11de15casos no tienen diferencias de estilos ni rectángulos mayores a.1px en los elementos muestreados, pero Booking conserva estados de cotización/scroll distintos y Serviciosdesktop dos diferencias de posición transitorias. No equivalen a comparación de toda la página ni a prueba de iOS físico.

**Estado: M0/M1/M2 implementados, medidos y revisados en código; F4 abierta.** Faltan Performance85, CLS del tenant y completar equivalencia visual. M3requiere diseño y autorización del usuario porque afecta hosting/Functions. No se continúa con microajustes de rendimiento después de este resultado. Se solicita revisión de cierre de evidencia a Claude.

Limpieza: los previews propios4173/4174/4175, ChromeCLI y pestañas propias se cerraron; viewport restaurado.4300libre.4200/8080del usuario conservados. Sin nuevo commit deM1/M2, push, despliegue, credenciales cambiadas, reservas, ventas ni pagos creados.

## Confirmación de Claude y límites finales

Claude `revisiones/008-f4-m2-cierre.md` contrastó los JSON: **sin bloqueantes nuevos de código, M1/M2 verificados como implementación y F4 abierta**, cifras coherentes. Se incorporan sus precisiones:

- La pareja820caliente tiene TBT1810ms y SI3678ms, una muestra anómala; no permite concluir que la caché no ayude en tablet.
- Los desplazamientos persisten con caché caliente: la políticaD1 no resuelve el layout y es un asunto separado.
- En el catálogo frío,11de13imágenes son de terceros y rondan3.45MB; LCP6.2–7.4s. Es un coste importante observado, sin demostrar causalidad única. **M3de marketing no arregla el catálogo ni su CLS.**
- Bookingmóvil tiene scroll0/0 pero content1/style41 y block-size +266px; atribuirlo a cotización es solo hipótesis. Tablet/escritorio tienen scroll distinto. No se han descartado esos casos para declarar equivalencia.
- El preview cambió de hash entre Home yP2 por el registro de peticiones; no cambiaron los bytes de assets ni la huella del build. La políticaD1 deP2 es opt-in y se declara por separado.
- Se añadió para **próximas** mediciones `pendingApiRequests`, para hacer visibles las peticiones enestado0 al terminar la auditoría. Las series guardadas antes no registraban ese contador; no se ha rellenado retrospectivamente ni repetido muestras para mejorar cifras. Los errores recibidos y las API esenciales200 sí fueron comprobados. El test específico del camino502 queda opcional y no ejecutado.

Siguiente revisión técnica sugerida por Claude: una línea baseP2sobreM0, con modo explícito sin tenantCSS, y análisis de trace antes de proponer corrección. Es recomendación pendiente, no implementación automática ni aprobación de un placeholder con altura supuesta. M3sigue reservado a decisión del usuario. Verificación final de huellas Home/P2 idénticas, guards/diff--check verdes y estilosapplication originales sin diff. Sin nuevas modificaciones de negocio.

## Actualización posterior: CLS y T1 antes de M3

La investigación posterior de CLS está cerrada localmente en `respuestas/008-f4-cls.md`, con revisión de Claude y30 muestras. Las cifras anteriores se conservan como historia de M2, no como estado final del tenant. HomeMarketing85 sigue pendiente.

T1 corregido por selector puro real de Admin, cobertura de Booking y aislamiento por archivo permanente; no aumenta timeouts ni reduce workers. Tres corridas consecutivas del comando normal `env PATH=/Users/christianmejia/.nvm/versions/node/v20.19.0/bin:/opt/homebrew/bin:/usr/bin:/bin TZ=America/Mexico_City npm test -- --watch=false`, Node20.19.0/Vitest4.0.16, exit0 en todas:

| Corrida | Pruebas/archivos | Duración | Tres pruebas más lentas |
|---|---|---:|---|
|1|304/82|36.34s|Booking precarga2321ms; promociones expandir1885ms; promociones guardar1618ms|
|2|304/82|40.74s|Promociones expandir2282ms; Booking precarga2018ms; Home contacto1949ms|
|3|304/82|35.62s|Booking precarga2112ms; Staff intervalo1979ms; promociones expandir1838ms|

Margen mínimo2679ms frente a5000ms, sin pruebas individuales≥4s. Referencia anterior301/83 con un worker temporal9.87s no es una comparación controlada: aislamiento y concurrencia difieren. Variante de template mínimo falló antes de la solución final (41.27s); no se oculta. Detalle y logs en `respuestas/008-f4-t1.md`.

Después de las suites se recompilaron marketing, application y producción predeterminada: exit0, sin NG8107; inicial476.17/824.99/848.17kB y transferencias estimadas130.28/167.70/176.59kB. Functions11 ypreview3 (14/14) verdes; guardas de CSS/assets/salidas y preparación de plantilla SEO correctas. No se atribuye una nueva medición Lighthouse a estos builds. Usuario autoriza commits y, después, comprobar dominios y M3 con su revisión por paso. No hay push ni despliegue.
