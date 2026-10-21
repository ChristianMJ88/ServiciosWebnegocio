# Revisión R3: código, criterios y residuales antes de cerrar las suites

**Veredicto:** el CSS final cumple lo aprobado, pero no doy la aprobación final porque faltan pruebas. Encontré dos defectos de código, los dos en el harness y ninguno en la aplicación. Los criterios 4 y 5 se pueden decidir sin pedir nada al usuario, siempre que el residual al desplazarse se le informe con sus causas.

Solo usé Read, Glob y Grep: protocolo, ESTADO, las revisiones R3, R3-c y R3-d, el CSS de hero y Home, el HTML de Home, la evidencia saneada, `csr-hydration-probe.mjs`, `probe-page.mjs`, `home-sections-measure.mjs`, `browser-probe.mjs` y el JSON de la matriz posterior, que todavía va por 844×391. No revisé en detalle `navigation-`, `font-`, `loading-plans-` ni `caption-proposal`. No ejecuté nada ni leí `.env`, `postman/` ni credenciales. No tengo herramienta de escritura: este texto hay que guardarlo como `revisiones/008-f4-m3-r3-cierre-parcial.md`.

## 1. CSS final: sin defectos

- **`hero.component.css:28-31`**: solo afecta a 768–1023. El caption ocupa toda la fila (`flex-basis:100%`) y los chips tienen `min-height:96px` sin contarlos. No choca con la regla `max-width:1199`, que va antes. El hueco de 52 px en 1023 y de 96 px con `nav503` es el residual que ya se declaró.
- **`fluora-home.component.css:9-28`**: las cuatro reservas de los cuatro rangos están **truncadas hacia abajo** respecto a los mínimos del JSON (por ejemplo 563.9549… → 563.954 y 639.328125 → 639.328). Así ningún redondeo puede hacer que el placeholder quede más alto que el contenido cargado.
  - El escritorio va en la regla base y se aplica a ≥1200, como corresponde.
  - No hay `height` ni `min-height` en el contenido cargado.
- **La evidencia saneada no tiene URL, host, `@` ni rutas locales.** Las huellas son de la API pública.

## 2. Defectos de código del harness

| ID | Prior. | Ubicación | Hallazgo | Corrección y prueba |
|---|---|---|---|---|
| C1-b | **Media** | `csr-hydration-probe.mjs:57` | En `/acceso`, el bucle termina en cuanto existen `redirectState` y `redirectUrl`, y los conteos se comprueban en ese mismo instante. Si hay una segunda redirección o una recarga unos cientos de ms después, no se detecta. Es el mismo bucle de antes, solo que más tarde. | Después de la primera detección, esperar una ventana de calma de 1000 ms sin Documents nuevos. Solo entonces exigir `appDocuments===1`, `documents===1` y que haya un único «development mode». |
| C1-c | **Media** | `csr-hydration-probe.mjs:58-60` | En las otras seis rutas no se exige `appDocuments===0`. Si una ruta CSR redirigiera por error al origen de la app, esa navegación se queda pausada sin terminar, el Document anterior sigue vivo y `waitFor('app-root[ng-version]')` lee **el estado anterior**. Sería un éxito falso igual que el de `/acceso`. | Con la misma ventana de calma, exigir `appDocuments===0` y `documents===1` en las seis rutas. Registrar los dos números en `item`, como ya se hace. |
| H6 | Baja | `browser-probe.mjs:76` | `clsWindow` empieza con `start=0` en lugar de usar el tiempo del primer desplazamiento. Si el primero ocurre antes de los 1000 ms, la ventana de 5 s se corta antes de tiempo. El error siempre **baja** la cifra, es decir, favorece que pase. Con los datos actuales no cambia nada, porque todo ocurre en menos de 3 s. | Inicializar `start` con el primer desplazamiento y agregar a `browser-probe.test.mjs` un caso que empiece en 600 ms y termine en 5500 ms. |
| H7 | Baja | `probe-page.mjs:41` | `settled()` solo espera las peticiones `/api/`, no los chunks de `@defer`. En los casos críticos del §4, el snapshot `initial` podría tomarse antes de que la sección se monte. Entonces su desplazamiento se atribuiría al scroll. | Ver el criterio del §4: exigir el componente montado en `initial` cuando el placeholder corta el viewport. |
| F1 | Baja | `csr-hydration-probe.mjs:62` | Cuando `--logo null` cambia el marketplace, `directory-renderer` y `/negocios` conservan el mismo nombre que en la corrida real. Solo los distingue `publicApiOverrides`. Además, `failure.json` se sobrescribe si las dos variantes usan el mismo `--output`. | Agregar `logoFixture: values.logo` a cada `item` y usar un `--output` distinto para cada variante. |

**Lo que está bien en el harness:**
- C1 cuenta Documents con `Fetch.requestPaused`.
- La captura en `beforeunload` lee el DOM anterior.
- La intercepción del marketplace copia la respuesta real y anula un solo campo (`imagenUrl`), registrando el campo y la cantidad.
- Los resultados se escriben **antes** de las aserciones.
- La consola estricta junta los fallos sin filtrar nada.

**Para que el límite quede honesto en la respuesta:**
- Producción: 7/7 limpios.
- Dev con el fixture derivado: el criterio de 0 mensajes debe cumplirse.
- Dev con datos reales: `exit 1` por NG0913. Es un aviso de tamaño de imagen que solo existe en desarrollo y aparece en el tenant y en el directorio. Queda **documentado como aviso ajeno, no como cumplido**. La optimización de logos es una tarea futura que decide el usuario.
- Repetir las dos variantes después de corregir C1-b y C1-c.

## 3. Decisión sobre el punto 4: qué significa «CLS de carga ≤ 0.01»

**Decisión:** el criterio bloqueante es el **`initial`**: la ventana máxima desde la navegación (incluye la fase sin JS y la de fuentes) hasta `settled`, antes de cualquier scroll. Aplica a los 16 tamaños, en `normal`, **en las dos repeticiones**.
- Los escenarios de fallo se reportan, pero no bloquean.
- El scroll (`sections.*.deltaCls` y `final`) es informativo y no bloquea R3. Así se definió en `008-f4-m3-r3-diseno.md:274` y en R3-d §3. No hace falta otra decisión del usuario.

**Lo que no se puede ocultar:** el CLS de usuarios reales (CrUX) sí cuenta los desplazamientos durante el scroll; Lighthouse no. La respuesta al usuario tiene que mostrar la cifra **por sección** junto con su causa medida. La cifra `final` no sirve para eso: en 375, el 0.01637 junta producto (0.0056) y FAQ (0.0108) en una sola ventana porque el scroll del script avanza en menos de 1 s, lo que es un artefacto del harness.

Causas que leí en la matriz posterior:

| Tamaño y sección | Delta | Causa |
|---|---|---|
| 375 producto | 0.0056 | Carga 748.95 frente a reserva 680.55 (+68 px), así que el siguiente bloque se desplaza. El mínimo del rango ≤767 lo marcan 740×360 y 767. |
| 375 y 360 FAQ | 0.0108 / 0.0079 | 588.13 frente a 549.78 (+38 px), por el mismo motivo. |
| 360 planes | 0.0183 | **Es interno de `plans.component`, no viene del placeholder.** `canScroll` se activa con `ResizeObserver` después del render (`plans.component.ts:9-14`). Las flechas aparecen y el subtítulo baja una línea (13 px × 1.7 = 22.1 px). Pasaba desde antes de R3. |
| 360 `plans503` | 0.0539 | El estado de error mide 552 frente a 820 de reserva, así que la página encoge. Es un residual declarado; no se debe inflar el estado de error. |

**Correcciones acotadas para después de R3, no ahora** (cualquiera de ellas obliga a repetir la matriz de 100):
- **(a)** Dividir el rango móvil en dos: vertical y horizontal. Con los mínimos de 360, 375 y 391 más los anchos de borde, las reservas se recalcularían con la misma regla. En 375 y 391 el desfase quedaría en unos 0 px, y en 360 en ≤20 px.
- **(b)** Mantener siempre el espacio de los controles del carril, con `visibility:hidden` y `inert` mientras `!canScroll`. Esto cambia la aplicación, así que lo prioriza el usuario.

Mi recomendación es presentarlas en la respuesta como seguimiento, antes de la serie de CLS de campo del prerender productivo, y no reabrir R3.

## 4. Decisión sobre el punto 5: criterio para las secciones que cortan el primer viewport

Esto aplica a 1024×1367 y 900×1440, **recalculado con la matriz posterior**. El encabezado cambió en 768–1023, así que `top` podría haber cambiado.

En `normal`, con n=2, se tienen que cumplir las cuatro condiciones:
1. **`before`:** el placeholder sigue presente y se registran su `top` y su `height` computados.
2. **`initial`:** el componente (`app-home-product`) ya está montado, lo que cierra H7. El `top` de la sección es igual al de `before` con una diferencia de ≤0.5 px, es decir, no se movió nada por encima.
3. La altura cargada es **≥** el `min-height` del placeholder en ese ancho. La reserva solo puede crecer.
4. `initial.cls` ≤ 0.01 y ningún `source` del primer viewport viene de la sustitución de la sección. Solo se aceptan el CTA y los iconos ya conocidos.

Además, hay que incluir 900×1440 en el `fontHold` posterior, porque está en la banda del caption. En `plans503`, `support503` y `nav503`, reportar `top` y alturas como residual, sin exigir el punto 3.

## 5. Sobre los 20 ciclos (M1)

Los ciclos de la matriz, más los de CSR, navegación y fuentes, pasan por el mismo `isolatedChrome().close()` (`browser-probe.mjs:46-53`). **Los acepto en lugar de una serie dedicada de 20 ciclos**, si se registran tres cosas:
- la lista de `fluora-hydration-chrome-*` en `os.tmpdir()` antes y después: debe ser igual a la inicial (solo el `gnTOxA` conocido, que no se toca);
- ningún proceso con `--user-data-dir` de ese prefijo al terminar;
- ningún `AggregateError` ni «failed to exit» en los logs de las corridas.

## 6. Orden y lo que todavía no está acreditado

1. Codex corrige C1-b, C1-c, H6 y F1, con su prueba Node.
2. Terminar la matriz de 100 y comparar huellas, pares, alturas, `initial` en los 16 tamaños y residuales por sección.
3. `fontHold` posterior (incluye 900), estado de carga de planes en los 16 tamaños, anclas, GSAP y resize, y CSR dev derivado y real.
4. UI en 4200, cinco suites normales y luego la revisión final.

No apruebo el cierre de R3. No autorizo commit, push, deploy, prerender productivo ni cambios de dominio, credenciales, backend o RBAC.
