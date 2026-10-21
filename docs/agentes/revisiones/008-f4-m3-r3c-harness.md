# Revisión R3-c: harness de navegador (`browser-probe.mjs`, `hydration-probe.mjs`)

**Veredicto: el harness todavía no puede dar evidencia. Hay 2 bloqueantes y R3-c no está completa.** El aislamiento está bien hecho: perfil temporal, `--remote-debugging-port=0` en 127.0.0.1, `about:blank`, `shell:false`, ningún attach a Chrome del usuario ni a 4200/8080/4300 como servidor propio, preview solo GET/HEAD públicos en puerto efímero, consola estricta y salidas en modo 0o600.

Solo leí con Read, Glob y Grep. No ejecuté nada, no leí `.env`, `postman/` ni credenciales, y no pude guardar `docs/agentes/revisiones/008-f4-m3-r3c-harness.md` porque no tengo herramienta de escritura; el script de revisión puede guardar este texto tal cual. En `/private/tmp/fluora-m3-r3c` no hay archivos, lo que cuadra con A1.

## Hallazgos

| ID | Prior. | Ubicación | Hallazgo | Corrección y prueba |
|---|---|---|---|---|
| **A1** | **Alta** | `hydration-probe.mjs:35,42`; `browser-probe.mjs:74` | **No existen marcas llamadas `NgHydration` ni `NgEventReplay`.** Angular 21.0.6 crea `performance.mark('mark_feature_usage', {detail:{feature}})` (`_debug_node-chunk.mjs:4098-4106`). Por eso `waitFor` de la línea 35 siempre agota 15 s y no se escribe `results.json`. Además, esa marca solo significa "hidratación habilitada" (`core.mjs:2373-2376`), no "terminada". | Leer `getEntriesByType('mark').filter(e=>e.name==='mark_feature_usage').map(e=>e.detail?.feature)`. Para saber que terminó en producción, sin tocar la app: `window._ejsas.ng` existe antes del bootstrap y queda en `undefined` después de `whenStable()` y del replay (`core.mjs:1818-1853`, `primitives-event-dispatch.mjs:784`). Con el JS retenido hay que comprobar que sigue definido y no hay marca; después, que se limpió y que `NgHydration`/`NgEventReplay` están en `detail.feature`. |
| **A2** | **Alta** | `hydration-probe.mjs:47`; `browser-probe.mjs:42` | El `finally` llama `await chrome?.close()` antes de cerrar el servidor. Si `rm` falla (ENOTEMPTY), el error original queda tapado, el servidor sigue escuchando y el proceso no termina. Además, la espera de `exit` tras SIGKILL no tiene timeout. | Limpieza en dos pasos independientes que siempre cierra el servidor y junta errores en un `AggregateError`. Timeout también en la rama SIGKILL. Añadir un handler `SIGINT`/`SIGTERM` y un plazo máximo por muestra con `AbortSignal`. Prueba: fixture donde `close()` lanza error → el servidor se cierra y el error original se conserva. |
| M1 | Media | `browser-probe.mjs:35-43` | **Causa probable del ENOTEMPTY:** procesos auxiliares de Chrome (crashpad, red, GPU) siguen escribiendo en el perfil después de que muere el proceso principal. Los reintentos de `rm` solo lo disimulan. **Faltan flags para no tocar el sistema en macOS:** sin `--use-mock-keychain --password-store=basic`, Chrome puede acceder al Llavero del usuario. `stderr` se descarta, así que un arranque fallido no deja diagnóstico. | Cerrar primero con `Browser.close` (WebSocket de `/json/version`) y después recurrir a señales. Usar `spawn(...,{detached:true})` y `process.kill(-pid)` para matar el grupo entero. Añadir `--use-mock-keychain --password-store=basic --disable-background-networking --disable-component-update --disable-sync --disable-default-apps --no-service-autorun --disable-breakpad`. Guardar las últimas ~8 KB de `stderr` y adjuntarlas solo si falla el arranque. Prueba: 20 ciclos abrir/cerrar sin que quede `fluora-hydration-chrome-*`. |
| M2 | Media | `hydration-probe.mjs:28-34` | La retención con Fetch es frágil: el patrón `*main-*.js*` es laxo y solo se guarda el último `requestId`, así que cualquier otra petición pausada se queda colgada. `Fetch.disable` (línea 34) impide luego interceptar API para los escenarios. | Un único "router" de Fetch para toda la muestra. Toma la URL exacta de `main-*.js` desde `index.html`. Retiene `main` hasta liberarlo y decide cada petición `/api/v1/publico/*` según el escenario (continuar, retrasar o responder 503). Todo lo demás continúa. Nunca `Fetch.disable` a mitad de corrida. Falla si se pausa algo inesperado o si `main` se pausa más de una vez. |
| M3 | Media | `hydration-probe.mjs:30-37`; `browser-probe.mjs:25` | Esperas frágiles: `delay(100)` y `delay(300)` fijos; bucle de pendientes de 10 s sin ventana de calma (una GET que empieza después no se ve); `waitFor` no tolera "Execution context was destroyed" durante la navegación; no se revisa `Page.navigate().errorText`. **La línea 36 exige chips > 0:** depende de datos configurables y se cuelga si la navegación devuelve 0 verticales. | Espera compuesta "asentado": `_ejsas` limpio, 0 GET pendientes durante 500 ms seguidos, ninguna entrada `layout-shift` nueva en 500 ms, y dos `requestAnimationFrame`. Esperar a que termine la respuesta de `navegacion` y registrar el número de chips como dato, no como condición. Reintentar `evaluate` solo ante errores de contexto. |
| M4 | Media | `hydration-probe.mjs:32-33`; `index.html:24` del probe | La captura "antes" se hace tras `document.fonts.ready`, pero la hoja completa carga asíncrona (`media="print" onload`) y puede no estar aplicada todavía. Mezcla dos causas de desplazamiento (cambio de hoja e hidratación). Además, no se comprueba el estado servidor. | Esperar a que el `link` tenga `media==='all'` y su `sheet` exista, después `fonts.ready` y dos rAF. Comprobar en "antes": 0 chips, CTA `Crear mi cuenta`, `_ejsas.ng` definido, sin marca de hidratación y URL exacta de `main` retenida. Guardar cada shift con su tiempo, junto a `responseEnd` de la hoja y al momento de liberar `main`. Así se atribuye la causa sin excluir nada del total. |
| M5 | Media | `browser-probe.mjs:62,71`; `hydration-probe.mjs:41` | La comprobación "mismo nodo" de `.plans-placeholder` depende del viewport: el `@defer(on viewport)` lo reemplaza si queda visible. En la matriz de 16 tamaños daría fallos falsos. | Exigir reutilización solo si la sección no se activó (registrar `triggered`). Si se activó, registrar que el placeholder hidratado existió antes del reemplazo. |
| M6 | Media | `hydration-probe.mjs` (no existe) | No se comprueba el modo desarrollo: el diseño §3 exige la línea «Angular hydrated N component(s)… 0 component(s) were skipped». | Leer `mode` de `render-origins.json`. En `development`, exigir exactamente un mensaje con `0 component(s) were skipped` y 0 `NG0`. |
| B1 | Baja | `browser-probe.mjs:66,74` | `cls` es la suma de todos los shifts, no CLS por ventanas de sesión. `className` de un SVG se serializa como `{}`. | Reportar la suma y la ventana máxima (separación de 1 s, tope de 5 s). Usar `getAttribute('class')`. |
| B2 | Baja | `browser-probe.mjs:13,19-23,51` | `send` con el socket cerrado deja vivo el temporizador. `JSON.parse` no está protegido. `fetch('/json/list')` no tiene timeout. El `evaluate` de 30 s puede pasarse del plazo de `waitFor`. | Comprobar `readyState` y limpiar. Usar `AbortSignal.timeout`. Limitar cada `evaluate` al tiempo que le queda a `waitFor`. |
| B3 | Baja | `browser-probe.mjs:26,74` | Overflow medido con `scrollWidth>innerWidth`. No se registra el ancho de la barra de scroll, que importa porque MarketingNav aplica `scrollbar-gutter: stable`. 820 se emula sin táctil. | Usar `scrollWidth>clientWidth` y registrar `innerWidth-clientWidth`. Registrar la configuración de emulación (táctil y `mobile`) junto a la evidencia, sin ocultar barras. |
| B4 | Baja | `hydration-probe.mjs:15,39` | `requests` se acumula para todos los tamaños. `results.json` siempre se sobrescribe y no deja nada si falla un `waitFor`. | Asignar peticiones por muestra. Nombre de archivo con huella del build y marca de tiempo. Escribir un diagnóstico parcial (fase, logs, excepciones) en el `catch`. |
| B5 | Baja | `hydration-probe.mjs:15` | La API `localhost:8080` está fija en el código. Usarla como upstream solo de lectura es aceptable (es el patrón de los scripts Lighthouse y no es un attach), pero si está caída se ve como errores de consola confusos. | Opción `--api`, registrarla, y hacer una GET pública previa que corte la corrida con un mensaje claro. |

## Diseño para completar R3-c y R3-d de forma eficiente

**1. Módulos:**
- `browser-probe.mjs`: infraestructura con Chrome, Cdp con `sessionId` y el router de Fetch.
- `probe-page.mjs`: abre una muestra con tamaño, escenario y opción de retener `main`, junta diagnósticos y expone la espera "asentado".
- `hydration-probe.mjs`: verificaciones de R3-c.
- `home-sections-measure.mjs`: medición de R3-d.
- Selectores en una sola constante compartida.

**2. Eficiencia:** un Chrome por modo y un `Target.createBrowserContext` por muestra. Cada contexto tiene caché y almacenamiento propios, equivalente a una carga fría, y se conecta por `Target.attachToTarget{flatten:true}`. Lanzar un Chrome por muestra queda solo como control en 375, 820 y 1440, comparando los resultados. Muestras secuenciales, sin builds ni suites en paralelo.

**3. Escenarios del router** (el resto de GET siempre reales):

| Escenario | Qué hace |
|---|---|
| `normal` | Sin intervención. |
| `plans503` | 503 en `/plataforma/planes`. |
| `support503` | 503 en `/plataforma/soporte`; termina en `null` por el `catchError` de `platform-contact.service.ts:22`. |
| `nav503` | 503 en `/marketing/landings/navegacion`. |
| `apiSlow` | Retiene las respuestas de API un tiempo fijo, registrado en la evidencia. |
| `netSlow` | `Network.emulateNetworkConditions` + `Emulation.setCPUThrottlingRate`, con los parámetros registrados. |

- **Consola sin suprimir:** solo se acepta el «Failed to load resource: 503» de la URL inyectada, exactamente una vez por petición inyectada, y se reporta. Cualquier otro error o aviso hace fallar la muestra. Si la app registra algo propio ante el 503, es un hallazgo, no se filtra.

**4. Pendientes de R3-c, cada uno en prod y en dev:**
- **Event replay a 375 (`normal`, `apiSlow`, `netSlow`):**
  - con `main` retenido, clic real con `Input.dispatchMouseEvent` en «Más» y liberar;
  - un `MutationObserver` cargado antes del bootstrap registra `performance.now()` del clic y del `dialog[open]`;
  - comprobar `aria-expanded=true` y que Escape devuelve el foco a «Más»;
  - informar la latencia (H2), sin umbral inventado.
- **H4:** nodo del footer reutilizado, rama de contacto presente y 0 NG05xx.
- **Anclas y resize:**
  - `/#planes` y `/#faq` en 375 y 1440: con la página asentada, el destino queda bajo la barra (tolerancia de 1 px) y tiene `aria-current`;
  - resize 1440→375→1440 con el menú cerrado y el scrollspy recalculado.
- **GSAP:** identificar su chunk en `dist` desde Node (texto del banner). A 1440 debe pedirse; con `Emulation.setEmulatedMedia` y reduced-motion, no.
- **CSR:**
  - `/acceso`, `/privacidad`, `/negocios` y `/barberias` desde el preview;
  - HTML de Functions servido por `Fetch.fulfillRequest` con los renderers importados en Node sobre `index.csr.html` y los fixtures de sus tests, sin emulador;
  - resultado: sin `NgHydration` en `detail.feature`, sin NG0505 y el primer hijo de `<app-root>` reemplazado.

**5. R3-d, matriz de secciones:**
- **Tamaños:** los 16 de `008-f4.md:63-76`, en una constante con su fuente citada.
- **Con `main` retenido:** `top` y `height` de cada placeholder. Si alguno corta el viewport inicial, es caso crítico.
- **Después de asentar, sección por sección** (`#producto`, `#planes`, `#faq`, cierre):
  - `scrollTo` hasta que el placeholder entre en el viewport, para activar el `on viewport` real; sin fragmento, `when renderAnchors()` no participa;
  - esperar el componente diferido con contenido propio del escenario (incluido «Consultando planes…» con `apiSlow` limitado a planes, que se libera después de medir), luego la calma de shifts y dos rAF;
  - registrar alturas antes y después, la diferencia, los shifts con sus fuentes y el `top` de una referencia, para detectar el scroll anchoring.
- **Hero:** número de líneas del CTA y de la reaseguranza, y chips, antes y después de los datos.
- **Datos:** huella SHA-256 de cada GET real más conteos (planes, FAQ, verticales, soporte sí/no), y la huella del build con `build-fingerprint.mjs`. Las alturas solo valen para esa huella.
- **Repeticiones:** `normal` n=2 (las alturas deben ser idénticas; si no, se reporta la varianza) y cada fallo n=1. Son unas 80 muestras.
- **Regla de reserva** (sin emitir cifras hasta medir): mínimo de las alturas en estado `normal` cargado dentro de cada rango de la CSS (≤767, 768–1023, 1024–1199, ≥1200). Los estados de fallo y la diferencia máx−mín por datos configurables se reportan como residual, sin congelar datos.
- Después de aplicar las reservas, repetir la matriz con la misma huella de datos. CLS por ventanas ≤ 0.01 es informativo; sin Lighthouse y sin prerender productivo.

## Orden

1. A1, A2 y M1–M4.
2. Correr 3 tamaños en prod y después en dev, y comprobar que no queda ningún perfil temporal.
3. M5, M6 y los bajos, junto con el router y los contextos.
4. Pendientes de R3-c, revisión y luego R3-d.

No hay que tocar workers, tests ni filtros de consola. Esta revisión no autoriza commit, push ni deploy, y R3-c sigue abierta.
