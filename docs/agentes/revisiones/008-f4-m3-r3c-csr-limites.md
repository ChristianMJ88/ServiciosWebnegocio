# Revisión R3-c: límites del CSR real (hallazgos 1–3)

**Veredicto:** la frontera CSR de R3 ya está demostrada en todos los casos que se ejecutaron, en prod y en dev. Lo que falta es evidencia de dev: el log `-dev-3` es de una versión anterior del script. La prueba nueva de `/acceso` es correcta en lo esencial, pero le falta una aserción contra el bucle. Nada de esto necesita una decisión del usuario para R3.

Solo usé Read, Glob y Grep. No ejecuté nada ni leí `.env`, `postman/` ni credenciales. No tengo herramienta de escritura, así que Codex tiene que guardar este texto como `revisiones/008-f4-m3-r3c-csr-limites.md`.

## 1. NG0913 en el tenant real (dev)

**Qué es, comprobado en el código:**
- Viene de `ImagePerformanceWarning` del core de Angular. Solo arranca en modo desarrollo (`node_modules/@angular/core/fesm2022/core.mjs:665-667`, condicionado por `ngDevMode`).
- Al cargar la página revisa todos los `<img>` y avisa cuando una imagen mide 1200 px o más que su tamaño mostrado (`core.mjs:529-532,576-577`).
- No depende de `provideClientHydration`.
- `src` no tiene `IMAGE_CONFIG`, así que no hay nada que lo esté ocultando.
- Los dos avisos corresponden a dos `<img>` que usan el `logoUrl` que sube el tenant. Lo más probable es que sean `header.component.html:24` y `services.component.html:8`, o la línea 111, que vuelve a `logoUrl`.

**Clasificación:**
- **Severidad baja.** Es ajeno a R3: es un dato del tenant y solo aparece en desarrollo.
- En producción hay 0 mensajes de consola.
- La frontera ya está demostrada en ese caso: no hay `NgHydration` ni NG05xx, el sentinel desaparece y el primer hijo se reemplaza (`results-1792514889355.json`, `tenant-renderer`).
- **No hace falta el usuario para R3.** Optimizar los logos de los tenants (cambiar su tamaño al subirlos o usar `srcset`) sería una tarea aparte que el usuario priorizaría más adelante.
- **No ocultar el aviso con `disableImageSizeWarning` ni con una lista de excepciones.**

**Cómo documentarlo sin falsear el cierre (recomendación):**
- **No usar el fixture del test de SEO.** Su `logoUrl:'/logo.png'` no existe en el preview, así que daría un 404 en consola. Además mezclaría un tenant inventado con el catálogo real.
- **Usar un A/B de un solo campo.** El renderer inyecta `window.__FLUORA_TENANT__` y la app lo usa en lugar de pedirlo a la API (`tenant-site.service.ts:19-22`). Así se prueba con dos casos:
  - `tenant-renderer`: los datos reales sin tocar. Debe dar NG0913 ×2 y queda registrado como fallo de consola, con su causa.
  - `tenant-renderer-sin-logo`: `{...tenant, logoUrl:null}`, declarado como fixture derivado. El resto de las GET son reales. Debe dar 0 mensajes de consola en dev.
- La diferencia entre los dos casos demuestra la causa sin filtrar nada. Hay que registrar en el resultado qué campo se cambió y qué `<img>` quedan en el DOM (`src`, tamaño natural y tamaño mostrado).
- **Salida del probe:**
  - La consola estricta sigue haciendo fallar el probe.
  - El caso real no se convierte en éxito. La respuesta cita el `exit 1` del caso real junto con el `exit 0` de los demás casos de dev, o ejecuta el caso real por separado.
  - El criterio estricto de «0 mensajes de consola» se cumple en producción y en el fixture derivado. En el tenant real de dev queda documentado como aviso ajeno, no como cumplido.

**Falta de evidencia en dev (media):**
- El log `-dev-3` aborta en la aserción de la antigua línea 49. Por eso `landing-renderer` y `directory-renderer` **no se ejecutaron en dev**. Solo pasaron las rutas del preview `/privacidad`, `/negocios` y `/barberias`.
- El `/acceso` de ese log muestra 7 veces «Angular is running in development mode». Eso es el bucle que describes en el punto 2: un éxito falso.
- Hay que volver a ejecutar todo dev con el script actual, que junta los fallos en lugar de abortar (`csr-hydration-probe.mjs:56,59`).

## 2. Prueba de `/acceso`

Sí es la prueba correcta del contrato:
- En el build de marketing, `/acceso` sigue siendo CSR y redirige al origen de la aplicación (`app.routes.marketing.ts:92`, `platform-redirect.component.ts:26`).
- Retener el Document de destino antes de que se confirme la navegación y leer el estado del documento anterior evita seguir a otro servidor.
- Exigir `app-platform-redirect` más la URL exacta (`:49`) corrige el éxito falso, que solo se basaba en `ng-version` y `router-outlet`.

| ID | Prior. | Ubicación | Hallazgo | Corrección y prueba |
|---|---|---|---|---|
| C1 | Media | `:38,49` | Si la redirección se dispara dos veces, `redirectUrl` se sobrescribe y nada detecta un bucle. Es justo el modo de fallo que ya apareció. | Contar los Documents pausados del origen de la aplicación (exactamente 1) y los Documents de `documentUrl` (exactamente 1). En dev, exigir exactamente un «development mode». |
| C2 | Baja | `:37` | El `runtime-config` interceptado se aplica a **los 7 casos**, no solo a `/acceso`. Con eso, `hasDedicatedAppHost()` pasa a ser `true` en todos (`platform-host.service.ts:102-106`). Afecta poco: `fluora-access` y la redirección de Home no participan en estos casos, y en la práctica se parece más a producción. Aun así, ya no es «el preview original». | Registrar `runtimeConfig:'intercepted'` y los dos orígenes en cada resultado, o limitarlo a `/acceso`. Cualquiera de las dos sirve si queda declarada. |
| C3 | Baja | fuera de alcance | `PlatformRedirectComponent` no protege contra un destino igual a la URL actual. Si `appOrigin` coincide con el origen actual, entra en bucle. En el preview pasa porque los dos orígenes son iguales; en producción los orígenes son distintos. | Solo dejarlo registrado como seguimiento. No se toca la app ni las guardas en R3. |

**Orden:** primero que termine la matriz R3-d y después ejecutar el CSR, un Chrome a la vez. Estoy de acuerdo.

## 3. Caption y fontHold

Acepto la corrección: el caption en la segunda fila agrega +31.1875 px al **encabezado del preview**, no al hero. Con los datos de la API cargados, `heroIncrease` es 0 en 820 y en 1000. Esto reemplaza lo que dije en `008-f4-m3-r3d-mediciones.md:42`. La línea base de `fontHold` concuerda con lo que medí antes (0.1366 frente a 0.1364 en 820; 768 y 800 sin cambio).

**Validación después de aplicar la CSS:**
- **En `fontHold`:**
  - la altura de `.fl-home-preview-heading` debe ser igual antes y después de liberar las fuentes;
  - antes de liberar `main` no debe haber desplazamientos en el hero en 812, 820, 844, 900, 1000 ni 1023;
  - 768 y 800 deben quedar sin cambio.
- **En la matriz `normal`:** la altura del hero en 768–1023 debe ser igual a la línea base, y la del encabezado del preview debe aumentar 31.1875 px.
- **En `nav503`, con 0 chips:** la columna de texto es más baja y el encabezado podría pasar a mandar en la altura del hero. Hay que registrar `heroIncrease` como residual y no dar por hecho que es 0.

Esta revisión no autoriza commit, push ni deploy, y R3-c sigue abierta hasta repetir dev completo y ejecutar C1.
