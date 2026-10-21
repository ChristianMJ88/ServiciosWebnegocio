Ya leí el código y los scripts; ahora redacto el dictamen.

# Revisión de R3: código de anclas y correcciones del harness

**Veredicto:** el código de la aplicación es correcto y no encontré defectos. No lo puedo dar por verificado todavía por tres cosas: el probe real de anclas no detectaría el fallo original (A1), una aserción de la spec puede pasar aunque falle (M1) y falta cubrir un caso en Planes (M2). Las correcciones del harness C1-b, C1-c, H6, H7, F1 y el arranque de DevTools están bien.

Solo usé Read, Glob y Grep. Leí PROTOCOLO, ESTADO, r3-anclas, r3-cierre-parcial, la solicitud, `plans.component.ts/html/spec`, `fluora-home.component.ts/html/spec`, `browser-probe(.test).mjs`, `probe-page.mjs`, `csr-hydration-probe.mjs`, `home-sections-measure.mjs` y `navigation-hydration-probe.mjs`. No tengo Bash, así que no vi el `git diff`: revisé el estado actual de cada archivo. No ejecuté nada ni leí `.env`, `postman/` ni credenciales. No puedo escribir archivos; esto hay que guardarlo como `revisiones/008-f4-m3-r3-anclas-codigo.md`.

## 1. Aplicación: aprobada

- **`plans.component.ts:12-19`:** se cumple C2. El callback escribe `canScroll` y `firstRailMeasured` juntos, de forma síncrona, y comprueba `this.rail===value` para descartar avisos de un rail anterior. El setter del `@ViewChild` solo se ejecuta cuando cambia el resultado de la query, así que volver a `false` ocurre solo cuando cambia el rail. `layoutReady` (`:11`) no añade reglas propias.
- **`fluora-home.component.ts:40,65-73`:** se cumple C1. Convertir `pendingAnchor` en signal es necesario, y el motivo que da Codex es correcto: sin fragmento, el efecto solo observa `pendingAnchor`, y un fragmento posterior lo vuelve a ejecutar. Escribir `null` dentro del efecto provoca una sola re-ejecución que termina en la guarda, sin bucle. Los cancelaciones (wheel/touch/teclas) y su limpieza se mantienen.
- **Límite medio de la revisión anterior, cerrado:** `.fl-home-plans-rail` no tiene `display:none` en ningún breakpoint (`plans.component.css:4,12-13`).

## 2. Hallazgos

| ID | Prioridad | Ubicación | Hallazgo | Corrección y validación |
|---|---|---|---|---|
| A1 | **Alta** | `navigation-hydration-probe.mjs:22-23` | La única aserción de posición es `target>=bar-1`. El fallo original (FAQ con `top=99.625`, esperado ≈78) **pasaría**. Además, en el caso de resize se hace un `scrollIntoView` manual (`:28`), así que no comprueba que no haya un segundo scroll automático. | Leer `getComputedStyle(target).scrollMarginTop` y exigir `abs(top − scrollMarginTop) ≤ 1`. Registrar si existe `.fl-home-rail-controls`, para saber si se probó el caso con o sin overflow en 375 y en 1440. Exigir que `scrollY` no cambie en 1 s después de `settled`. En el resize, guardar `scrollY` antes y después de `viewport()`+`settled()`, antes del scroll manual, y confirmar que no hubo reposicionamiento. Sin esto, la «verificación real de anclas» pendiente no prueba lo que se corrigió. |
| M1 | Media | `fluora-home.component.spec.ts:35` | El `expect` va dentro del mock de `scrollIntoView`, que se llama desde un hook de render de Angular. Si ese error termina en el ErrorHandler sin volver a lanzarse, Vitest no marca la prueba como fallida, y `toHaveBeenCalledOnce()` cuenta igual una llamada que lanzó. Es justo la prueba que respalda C2. | Guardar el valor, por ejemplo `const seen:boolean[]=[]; scroll=vi.fn(()=>seen.push(!!…querySelector('.fl-home-rail-controls')))`, y comprobar `expect(seen).toEqual([overflow])` fuera del mock. Como alternativa, `expect(scroll.mock.results[0].type).toBe('return')`. |
| M2 | Media | `plans.component.spec.ts:16-21` | La prueba demuestra `false` mientras `loading` es true, pero no demuestra que **el cambio de rail** vuelva a poner `firstRailMeasured` en false. Si `loading` termina y aparece el rail nuevo en la misma pasada, sin ese reset `layoutReady` sería true antes de medir. La solicitud afirma esta propiedad y ninguna prueba la cubre. | Después de `loading.set(true)`, volver a `loading.set(false)` (o reintentar y hacer flush de `[plan]`) y llamar a `detectChanges()`. Debe seguir `layoutReady()===false`; después de `notify()` y `whenStable()`, debe ser `true`. |
| B1 | Baja | Solicitud §Evidencia | La frase «código de ancla no entra en la ruta raíz sin fragmento» no es exacta. El `afterRenderEffect` se registra siempre, y el setter con `firstRailMeasured` siempre pasa de false a true, así que hay una pasada de detección extra cuando llega el ResizeObserver, aunque `canScroll` no cambie. No cambia el DOM ni debería aportar CLS, pero cambia el JS y la huella. | Redactarlo así: «sin cambio de DOM en la raíz; huella nueva». El plan ya es correcto: la matriz CSS se atribuye a su SHA y los 32 normales al artefacto final. |
| B2 | Baja | `csr-hydration-probe.mjs:13,68` | F1 depende de que el operador pase un `--output` distinto. Con el valor por defecto, `failure.json` sigue pudiendo sobrescribirse. | Añadir `values.logo` al `output` por defecto, o rechazar el arranque si ya existe `failure.json`. |
| B3 | Baja | `browser-probe.test.mjs:23-26` | `debuggerJson` tiene pruebas para el socket transitorio y para el 403. Falta probar un código no reintentable (por ejemplo `ENOTFOUND`) y `running()` en false. | Agregar dos aserciones. No bloquea. |

## 3. Harness: bien resuelto

- **C1-b** (`csr:57`): ventana de 1000 ms después de detectar la redirección; `appDocuments===1`, `documents===1` y un único aviso de modo dev.
- **C1-c** (`csr:60`): misma ventana, con `appDocuments===0` y `documents===1`.
- **F1:** `logoFixture` está en el `item` (`:62`).
- **H6** (`browser-probe.mjs:87`): `start` toma el valor del primer desplazamiento. El caso 600→5500 da 0.7, que es correcto.
- **H7:**
  - `settled()` espera los chunks `.js` del mismo origen (`probe-page.mjs:21,41`).
  - Antes del `initial` se monta cada componente que corta el viewport (`home-sections-measure.mjs:27-29`).
  - Se cumplen las cuatro condiciones del §4 en `normal` (`:30`).
  - Nota menor: el mapa de `source.section` no incluye `closing`. Hoy no es crítico en ningún tamaño.
- **DevTools** (`browser-probe.mjs:75-82`): reintenta solo durante el discovery, con un límite de 5 s, solo para ECONNREFUSED/RESET/UND_ERR_SOCKET/timeout y con Chrome vivo. Un HTTP que no sea `ok` o un error al parsear no se reintentan. Navegación y aserciones quedan fuera del reintento, y no hay reinicio silencioso de Chrome.
- **Evidencia:** las cifras de la solicitud aparecen en `evidencias/008-f4-m3-r3d-after.json`. No volví a calcularlas a partir de los desplazamientos sin procesar.

## 4. Orden propuesto

1. Codex corrige A1, M1 y M2; B1 a B3 son opcionales. Repetir las pruebas focales sin ampliar timeouts.
2. Compilación final y huella nueva, y luego el probe de anclas corregido: `/#planes` y `/#faq` en 375×812 y 1440×900, con resize. Criterio: `top` dentro de `scrollMarginTop` ±1, `aria-current` correcto, `scrollY` estable, 0 errores de consola y ningún NG05xx. Comparar con `failure.json` anterior.
3. Después, lo que ya estaba pendiente: fontHold (incluido 900), carga de planes, CSR dev derivado y real, los 32 normales del último JS, cinco suites, la UI en 4200 y la revisión final.

No apruebo el cierre de R3. No autorizo commit, push, deploy, prerender productivo ni cambios de backend, Functions, Firebase, RBAC o datos.
