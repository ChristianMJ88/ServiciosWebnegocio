# Revisión R3-a: configuración, compuerta y metadatos

Solo hice lectura. No ejecuté suites, builds ni navegador. No leí `.env`, `postman/` ni credenciales. Esta sesión no tiene herramienta de escritura, así que **no guardé `docs/agentes/revisiones/008-f4-m3-r3a-codigo.md`**; el script de revisión puede guardar este texto tal cual.

**Qué revisé:**
- La solicitud y el diseño R3.
- Archivos de esta etapa: `app.config.marketing.ts`, `marketing-locale.ts`, `main.marketing.ts`, `platform-host.service.ts`, `marketing-hydration-target.spec.ts`, `FluoraHome` (ts y html), `hero` (ts y html), `marketing-offer.service.ts`, `marketing-appearance.service.ts` y su spec, `fluora-home.component.spec.ts:35-58`, `marketing-server-shell.spec.ts:52`, `scripts/marketing-hydration-wiring.test.mjs` y `public/runtime-config.js`.
- En `node_modules` 21.0.6: `core.mjs` (`withDomHydration` y `withEventReplay`), `_debug_node-chunk.mjs` (`ApplicationRef.synchronize`, `AfterRenderImpl` y `ViewRef.detectChanges`), `testing.mjs` (`ComponentFixture.detectChanges`), el schema `define` de `@angular/build` y el parser de opciones objeto de `@angular/cli`.
- Los specs de T2 no los revisé a fondo y no forman parte de este veredicto.

## Hallazgos

| ID | Prior. | Ubicación | Hallazgo |
|---|---|---|---|
| A1 | **Alta** | `fluora-home.component.spec.ts:46-51` | El spec de la compuerta no mide el primer DOM y según el código fuente debería fallar (detalle abajo). |
| A2 | Media | `marketing-hydration-target.spec.ts:16` + `core.mjs:2391-2394` | En `ng serve` de marketing en `localhost:4200/` aparecerá **NG0505 como `console.warn`**: el proveedor está activo, no hay `__nghData__` y no hay `ngcm`. Es esperado; hay que documentarlo, no ocultarlo (punto 2). |
| A3 | Media-baja | `marketing-appearance.service.spec.ts` | Solo se prueba la lectura de los atributos `data-fl-*`. Falta comprobar que `activate()` los **escribe**, que es lo que el servidor serializará. |
| A4 | Baja | `platform-host.service.ts:67-72` | Si `runtime-config.js` no carga, `marketingOrigin` toma el `currentOrigin` y **cualquier host en `/` resulta hidratable**, incluido un dominio propio. |
| A5 | Baja | `scripts/marketing-hydration-wiring.test.mjs` | `createMarketingAppConfig(false)` solo se comprueba con regex. Es aceptable según el diseño §3; se puede mejorar con una comparación de proveedores. |
| A6 | Info | R3-a §2 | La diferencia de tamaño del bundle de marketing (runtime de hidratación y de replay) sigue pendiente de reportar. |

### A1 en detalle

**Por qué el spec no mide el primer DOM:**
1. El proyecto es *zoneless*: no hay `zone.js` en `package.json`, `angular.json` ni en el `runnerConfig`.
2. En ese modo, `fixture.detectChanges()` llama a `appRef.tick()` (`testing.mjs:205-208`).
3. `tick` → `synchronize()` ejecuta `afterRenderManager.execute()` dentro del mismo bucle (`_debug_node-chunk.mjs:11931-11979`). La secuencia de `afterNextRender` creada en el constructor se registra en la vista (`:4185`, `:5343`).
4. El hook hace `hydrated.set(true)`, la vista queda sucia y el bucle vuelve a renderizar.
5. Por eso, cuando `fixture.detectChanges()` devuelve el control, el DOM ya está en el estado hidratado: 1 chip, «Probar 11 días» y «Prueba sin tarjeta».
6. Las líneas 49-51 deberían fallar. Si salen en verde, hay una causa que no está explicada y la prueba no demuestra lo que dice. El comentario de la línea 47 tampoco es correcto.

**La compuerta en producción sí es correcta.** En la hidratación real, la primera pasada (`synchronizeOnce`) hidrata con `hydrated=false`, y el render con datos ocurre en la pasada siguiente, creando vistas nuevas en lugar de reutilizar DOM. El código no cambia; lo que hay que corregir es la prueba.

**Corrección propuesta (sin ampliar timeouts ni saltarse la espera):**
- Primera pasada: `fixture.changeDetectorRef.detectChanges()` en lugar de `fixture.detectChanges()`. `ViewRef.detectChanges` (`:5878-5881`) no ejecuta los hooks *after render*.
- Antes de las aserciones del primer DOM, añadir `expect(fixture.componentInstance.hydrated()).toBe(false)` como autocontrol contra falsos positivos.
- Después, `await fixture.whenStable(); fixture.detectChanges();`, comprobar `hydrated()===true` y las aserciones actuales de las líneas 53-56.

**Validación:** correr el spec focal antes y después del cambio. Con la versión actual se espera que falle en la línea 49; si pasa, hay que reportarlo con su causa.

## Respuesta a los cuatro puntos

### 1. Compuerta y anclas

**Lógica de la compuerta: aprobada.**
- `heroVerticals`, `offerReady` y `fallbackLabel` tienen una sola fuente (`marketing-offer.service.ts:15,18`).
- `dataReady` incluye `hydrated`, y tanto los `@defer … when` como el `afterEveryRender` de las anclas la esperan. No hay timers.
- En el servidor, `hydrated` y `dataReady` quedan en `false` (`marketing-server-shell.spec.ts:52`).
- El `[href]="offer.href()"` del CTA no tiene compuerta. Puede diferir en `planId`, pero es un atributo, no un error de estructura (R3-diseño §0).

**Evidencia de la compuerta: pendiente** hasta corregir A1.

**Las anclas** no cambian de semántica en las pruebas actuales (líneas 29-31). Nota para R3-c: con HTML prerenderizado, el navegador hará un primer salto nativo a `#planes` sobre el placeholder antes del JS, y después la ancla diferida corregirá la posición. Hay que medirlo; no es un defecto de R3-a.

### 2. Root CSR con el proveedor

**No añadir un detector propio del estado serializado.** Duplicaría la comprobación nativa (`transferState.get(NGH_DATA_KEY)`, `core.mjs:2373`), y un *fallback* propio podría esconder un SSR roto.

Lo que verifiqué en el código:
- En producción no hay `ngDevMode`: la hidratación se desactiva sola en silencio y `PRESERVE_HOST_CONTENT` es `false`, así que el CSR limpia `<app-root>` como hoy.
- `withEventReplay` queda inactivo sin `window._ejsas` (`core.mjs:1768`).

Lo que hay que hacer:
- **Documentar A2** en la respuesta de R3-a como aviso esperado solo en desarrollo, en `/` y en marketing.
- **No añadir `ngcm` a `src/index.html`.** Pasaría al HTML prerenderizado y suprimiría justo el aviso que delata un SSR roto.
- El criterio de R3-c de «0 warn en desarrollo» se aplica al **probe**, que trae `__nghData__`, y a las rutas CSR distintas de `/`, que no tienen proveedor. No se aplica a `ng serve` en `/`.
- Probar el `ngcm` nativo de `index.csr.html` en el probe es suficiente.

### 3. Fuente única de orígenes para el probe

**Apruebo la alternativa** frente a escribir dos literales en `angular.json`. Verifiqué que la CLI 21 acepta `--define KEY=VALUE` como mapa de strings (`@angular/cli/.../json-schema.js:14-42,259-262`).

Condiciones:
- **Un solo módulo** (p. ej. `scripts/lib/marketing-origins.cjs`) evalúa `public/runtime-config.js` y lo importan **tanto** el script de build **como** `check-hydration-probe.cjs`. No debe haber dos evaluadores distintos.
- La evaluación usa `vm.createContext` con solo `{window:{location:{hostname:'build.invalid',origin:'https://build.invalid'}}}`: sin `__FLUORA_*` previos y sin `process` ni `require`. `vm` no es una frontera de seguridad, pero el archivo es del propio repositorio.
- Validación: los dos orígenes con protocolo `https:`, distintos, sin ruta ni barra final, y que no sean `build.invalid`.
- Valores con `JSON.stringify` y lanzamiento con `spawn`/`execFile` usando un array de argumentos y `shell:false`.
- Una guarda Node que confirme que la configuración del probe en `angular.json` **no tiene `define`** ni literales `https://`.
- El servidor sigue fallando cerrado sin `define` (R3-diseño §1.5).

Nota para el paso productivo: el HTML prerenderizado congela los valores por defecto. Si un host sobrescribe `__FLUORA_*` en tiempo de ejecución, el primer CD solo corrige atributos. Es aceptable, pero hay que registrarlo.

### 4. `createMarketingAppConfig(false)` y la metadata

- **Sin hidratación del árbol del tenant: confirmado.** Con `false` no entra `withDomHydration`, así que `IS_HYDRATION_DOM_REUSE_ENABLED` queda en su valor por defecto `false` (`app.config.marketing.ts:16`). Para A5 propongo `expect(createMarketingAppConfig(true).providers.length).toBe(createMarketingAppConfig(false).providers.length+1)`.
- **Atributos `data-fl-*` sin interferencias: confirmado.**
  - El contador compartido hace que Nav con `activate(true)` y Home con `activate()` solo restauren con el último usuario (spec :10-18).
  - El original vacío se distingue de `created` (:19-23).
  - Un tag sin atributos (caso CSR) sigue usando `content` (:30).
  - `restore` limpia los dos atributos.
- **Para A3**, añadir dos aserciones de escritura:
  - con `theme-color` = X, después de `activate()` el tag tiene `data-fl-original="X"` y no tiene lease;
  - sin el tag, después de `activate()` el tag tiene `data-fl-lease="created"`.

**A4 (opcional y de bajo coste):** en `isHydratableMarketingHome`, devolver `false` si falta `runtime.__FLUORA_MARKETING_ORIGIN__`, y cambiar la línea 16 del spec para pasar `{__FLUORA_MARKETING_ORIGIN__:'http://localhost:4200'}`. En local, `runtime-config.js` ya lo define (`:11-14`). No duplica la regla de host; solo hace fallar cerrado la defensa en profundidad.

## Veredicto R3-a

**Aprobado condicionado.** El código de configuración, la decisión por host y ruta, la compuerta y Appearance son correctos y están dentro del alcance. `app.config.base`, `main.ts`, `main.application.ts` y `app.config.ts` no tienen cambios en el working tree.

**Antes de R3-b:**
1. **A1 (obligatorio):** corregir la captura del primer DOM con el autocontrol `hydrated()===false` y registrar la corrida focal antes y después.
2. **A3:** añadir las aserciones de escritura.
3. **A2:** documentar el aviso de desarrollo en la respuesta.
4. Cerrar la ejecución focal de Angular, los builds y las guardas que siguen en curso, y reportar la diferencia de tamaño (A6). Cinco corridas consecutivas siguen siendo el criterio de R3-e.

**Opcionales:** A4 y A5.

El punto 3 queda aprobado para R3-b con las condiciones de arriba. Esta revisión no autoriza commit, push, deploy ni activar el prerender productivo.
