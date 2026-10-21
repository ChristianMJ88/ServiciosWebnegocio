# Revisión R3: correcciones de parser e iconos, y harness

**Veredicto:** los dos CSS están bien, pero el harness todavía no sirve para cerrar R3. Tiene dos defectos P1 que pueden dar un pase falso o cortar la matriz en la primera muestra que falle, y uno de criterio que mezcla lo que causa el parser con lo que causan las fuentes. No doy aprobación final: la corrida AFTER52 está en curso y no le atribuyo resultados.

**Qué revisé:** solo lectura. PROTOCOLO, ESTADO, la revisión r3-font-residual, `layout-residual-probe.mjs`, `probe-page.mjs`, `public-preview.mjs`, `home-sections-measure.mjs`, `browser-probe.mjs`, la plantilla y el CSS de marketing-nav, y `pillars.component.css`. No ejecuté nada ni abrí `.env`. No tengo herramienta de escritura: Codex puede guardar este texto como `revisiones/008-f4-m3-r3-correcciones.md`.

## CSS: correcto

- **`marketing-nav.component.css:9`:** `.menu-trigger` está siempre en el DOM: está dentro de `.top-actions` y fuera de cualquier `@if` (`marketing-nav.component.html:11`). Por eso la regla no puede dejar la barra oculta para siempre. Que el `.menu-trigger` de dentro de `:has()` quede sin atributo de componente no tiene efecto, porque es descendiente del mismo componente.
- **`pillars.component.css:3`:** `width:1em` aplica, porque el `<i>` es un elemento flex.
- **Nombres de fuente:** `'Fluora Marketing Manrope'`, `'Fluora Marketing Jakarta'` y `bootstrap-icons` coinciden con los filtros de `releaseFonts`. Las rutas `manrope.`, `plusjakartasans.` y `bootstrap-icons` coinciden con `marketing-tokens.css:2-3`.

## Hallazgos

**P1-a. `home-sections-measure.mjs:31` todavía corta la matriz en la primera muestra que falla.**
- `assert.ok(initial.cls<=.01,'Critical initial CLS')` está dentro del bucle de secciones críticas.
- Las secciones críticas aparecen sobre todo en viewports altos, como 800×1280, que es justo donde está el residual. Esa muestra lanza error antes del `results.push` de la línea 38, así que la matriz se detiene, y `max` y `results` quedan sin esa muestra.
- **Corrección:** quitar solo esa aserción, porque la línea 30 ya registra la violación. Las demás aserciones críticas se mantienen (top estable, solo crecer, ninguna fuente en la sección).
- **Validación:** con `--sizes 800x1280 --repetitions 2`, el JSON debe tener las 2 muestras y la corrida debe terminar con `exit 1` solo al final si hay violaciones.

**P1-b. Los filtros por clase o id dejan pasar desplazamientos del nav y del workflow.**
- `browser-probe.mjs:96` guarda `cls` del nodo exacto. En un nodo de texto `cls` queda vacío.
- Los filtros `layout-residual-probe.mjs:30` (`/desktop-links|top-actions/` sobre `v.cls`) y `:42` (`v.id==='como-funciona'`) no detectan fuentes como `summary`, `a.sign-in`, `.top-cta`, `span.desktop-login`, nodos de texto ni hijos de `#como-funciona`. Chrome suele atribuir el cambio a los hijos.
- Resultado: puede salir `navShifts=0` aunque la barra se movió.
- **Corrección:** en el observer, agregar a cada fuente un campo aditivo calculado con `element.closest(...)`: `region:'nav'|'actions'|'workflow'|'pillars'|null` (el selector de pillars es `app-home-pillars`). Los filtros deben usar `region`. Es aditivo y no cambia los otros probes.
- **Validación:** en la fase BEFORE, el corte `details` a 1280 debe seguir marcando la fuente del nav con la regla nueva.

**P1-c. El criterio no separa el parser de las fuentes (línea 32).**
- Con `held=true`, la barra ya es visible con la fuente de respaldo cuando se liberan las fuentes. El cambio a Manrope mueve la barra unos ~4 px. Es lo mismo que vimos a los 272 ms, unos 0.00006.
- Tal como está, eso cuenta como violación «nav» en hasta 12 casos. Es parcialmente culpa de mi revisión anterior, donde pedí «0 fuentes en NAV» sin distinguir.
- **Corrección:** guardar `fontsReleasedAt` (`performance.now()` justo antes de `releaseFonts`) y `releasedAt`, y clasificar cada desplazamiento del nav:

| Clase | Momento | Criterio |
|---|---|---|
| `parser` | antes de `fontsReleasedAt` (o todos, si `held=false`) | debe ser 0 |
| `font` | entre `fontsReleasedAt` y `releasedAt` | se reporta con su valor; solo cuenta para `initial≤.01` |
| `hydration` | desde `releasedAt` | debe ser 0 |

- No hay que descartar nada: guardar las tres listas en el JSON.

**P2-a. El criterio de iconos (línea 41) mezcla el icono con el texto y con lo que pasa más arriba.**
- `workflow.top` es una posición en la ventana. Si Manrope cambia los saltos de línea del hero, `stable` sale `false` y se reporta como problema del icono.
- Lo mismo con la altura de `article` en las transiciones de Manrope. Ese es justo el residual de texto que pedí reportar aparte.
- **Corrección:** evaluar cada transición por separado.
  - Las que solo cambian iconos (F/F→F/I y M/F→M/I) deben dar delta 0 en x y altura de cada `article`, y en `workflow.top` medido desde la caja de `app-home-pillars`.
  - Las que solo cambian Manrope se guardan como `textResidual` con sus valores. No son una violación del cambio de iconos.

**P2-b. Las comprobaciones pasan solas cuando no encuentran nada.**
- `s.icons.every(...)` y `s.articles.every(...)` dan `true` si el selector no encuentra elementos.
- **Corrección:** agregar `assert.ok(s.icons.length>0 && s.icons.length===s.articles.length)` y comprobar que `workflow` no sea `null`.

**P2-c. Falta comprobar la barra en la pausa y al final.**
- En la pausa solo se mira `.desktop-links` (línea 32). Falta `getComputedStyle(.top-actions).visibility==='hidden'`, que importa en el corte `sign-in`.
- Después de `settled` falta comprobar que `.top-actions` está visible, y también `.desktop-links` a 1200 px o más. Esto evita que una regresión deje la barra oculta para siempre.

**P3-a. El nombre `initialCls` significa cosas distintas.**
- En el caso nav es `final.cls`, que incluye la hidratación. En el caso iconos es `before.cls`, que es antes del bootstrap.
- **Corrección:** renombrar el de iconos a `preBootstrapCls`, o medir igual que `home-sections-measure` (después de `release` y `settled`).

**P3-b. Limpieza de Chrome (`browser-probe.mjs:50-51`).**
- El `if(running())` es correcto y no oculta el EPERM.
- Pero ya no se mata el grupo cuando el proceso principal termina y quedan procesos auxiliares huérfanos.
- **Corrección:** después de `exited()`, llamar `process.kill(-pid,0)`.
  - Si da ESRCH, todo bien.
  - Si tiene éxito, quedan procesos: enviar SIGKILL y lanzar error si siguen vivos.
  - Cualquier otro código, EPERM incluido, se reporta como error de limpieza.
- `browser-probe.test.mjs` no tiene ninguna prueba de `signalGroup` ni del guard: falta una con `kill` inyectado.

**P3-c. Una aserción de la pausa corta toda la corrida (línea 27).**
- Si la espera más los dos `rAF` pasan de 1500 ms, el `assert` de dentro de `onParserPause` corta las 52 muestras.
- Es correcto que eso no cuente como violación, pero conviene registrarlo como `invalidSample` y seguir, o subir `holdMs` a 3000.

## Sin defectos

- `htmlSplit` en `public-preview.mjs`: valida un marcador único y `holdMs` entre 1 y 5000, solo actúa en GET de `/`, va sin gzip y en chunked, y cancela el temporizador al cerrarse la conexión. Las demás rutas no cambian.
- `releaseFonts({only})`: el handler de la línea 29 deja pasar las solicitudes futuras de las clases ya liberadas, y la API sin argumentos no cambia.

## Condiciones para cerrar R3

1. Corregir P1-a, P1-b y P1-c antes de usar AFTER52 como evidencia. La corrida en curso usa los filtros de P1-b, así que si sale «0 desplazamientos en nav o workflow» no cuenta como validación.
2. Que la evidencia BEFORE armada con dos corridas (las 48 muestras y la carpeta con las 4 restantes) tenga la misma huella de build y que las muestras no se repitan. Que la huella AFTER sea distinta y que su diff contenga solo los dos CSS.
3. Matriz `normal` de 32 muestras con P1-a corregido: la corrida termina, el JSON tiene `{limit,max,violations}` completo y todas las muestras están guardadas.
4. Si queda un residual de texto por Manrope, como el `textResidual` de P2-a o la clase `font` de P1-c, se reporta con su valor y no se compensa.
5. Límite que se mantiene: solo se probó con Chrome headless en macOS y una red local. El corte después de `</h1>` en el hero sigue siendo solo diagnóstico.

No autorizo commit, push, deploy ni prerender productivo.
