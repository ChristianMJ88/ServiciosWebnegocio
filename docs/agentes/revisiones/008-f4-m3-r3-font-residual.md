# Revisión R3: los dos saltos residuales sobre CLS ≤ 0.01

**Veredicto:** no apruebo R3. Las dos muestras tienen causas distintas, y la de 1280 no viene de las fuentes:

- **1280×800, repetición 1:** el navegador pintó el HTML prerenderizado cuando todavía no había terminado de leerlo.
- **800×1280, repetición 2:** el cambio a Manrope y la llegada de los iconos se aplicaron en fotogramas distintos. Los iconos de Pillars no tienen un ancho fijo.

Las dos se corrigen con CSS de presentación, sin anchos sacados de textos ni de datos.

Solo leí con Read, Glob y Grep: PROTOCOLO, las dos revisiones que pediste, el JSON completo y el resumen saneado, el HTML y CSS de Nav, Hero, Pillars y Workflow, los iconos generados, `probe-page`, `public-preview`, `font-hydration-probe`, `home-sections-measure` y el `index.html` del probe. No ejecuté nada ni leí `.env`. No tengo herramienta de escritura, así que este texto hay que guardarlo como `revisiones/008-f4-m3-r3-residual-fuentes.md`.

## 1. 1280×800, repetición 1 (0.012971 a los 226.7 ms)

**Qué muestra el JSON** (líneas 49613–49642):
- **Antes:** el `NAV.desktop-links` mide 67.02 × 22.39 en x=1156.98, con el borde derecho en 1224.
- **Después:** mide 423 × 48 en x=376.
- Es la única fuente del cambio.

**Por qué no son las fuentes:**
- Manrope y Jakarta ya habían llegado a los 164–166 ms. Los iconos llegaron a los 237 ms.
- El efecto de las fuentes aparece aparte, a los 272 ms: el NAV se mueve 4 px y la flecha −4 px, en total 0.00006. Es lo mismo que pasa en 1367 y 1440.

**Lo que encaja con la evidencia es un pintado con el DOM a medio leer:**
- **El borde derecho en 1224 es el borde del contenido de la barra:** 40 + 1200 − 16 de padding en la banda 1200–1350. Con `justify-content:space-between` (`marketing-nav.component.css:1`), el NAV solo queda pegado a la derecha si es el último hijo, es decir, si `.top-actions` todavía no existía.
- **El tamaño de 67 × 22.39 corresponde a un `<details>` sin su `<summary>`.** En ese caso Chrome dibuja su resumen por defecto «▸ Details» con la fuente heredada de 14 px y `line-height` 1.6, que da 22.4. Un `summary` real mediría 48 de alto (`min-height:48px`) y usaría 13 px.
- **Solo hay una fuente en el cambio** porque la marca no se movió y `.top-actions` aparece nuevo, sin desplazarse.
- **Nada en la primera pantalla obliga al navegador a esperar.** `styles-*.css` se carga de forma asíncrona (`media="print" onload`, `index.html:24`). Por eso Chrome puede pintar cuando el parser hace una pausa, y si lo hace o no depende de la corrida.

Esto no es un artefacto del entorno local. En una red real el HTML llega por partes y este estado es más probable que aquí.

**Cambio mínimo (P1, Codex, `marketing-nav.component.css`):**
```css
.marketing-topbar:not(:has(.menu-trigger))>:is(.desktop-links,.top-actions){visibility:hidden}
```
- `.menu-trigger` es el último elemento del `<header>`. Mientras no se haya leído, el NAV y las acciones no se pintan. Lo que no se pintó en el fotograma anterior no cuenta como desplazamiento.
- Cuando el HTML está completo, el aspecto es idéntico al actual. No necesita aprobación visual.
- No usa anchos, datos ni JS. Si el navegador no soporta `:has`, ignora la regla y queda el comportamiento actual.
- Angular 21 ya acepta `:has` con encapsulación emulada, y hay otros 5 componentes que lo usan. Aun así, hay que comprobar en el CSS compilado que el selector quedó con el atributo del componente.

Descarto pasar la barra a grid `1fr auto 1fr` o anclar los enlaces a la izquierda: cambia la posición final y, aun así, el NAV seguiría creciendo desde «Details».

## 2. 800×1280, repetición 2 (0.005409 + 0.006651)

**Secuencia** (líneas 56635–56869). Los tres archivos de fuente terminaron de llegar entre 233.3 y 233.7 ms, pero se aplicaron en dos fotogramas:

| Momento | Qué pasa |
|---|---|
| 284.8 ms | Se aplica Manrope. El texto del pilar de la derecha (DIV en x=447) cabe en una línea menos: pasa de 73.375 a 51.94 de alto. `fl-home-availability` y `#como-funciona` suben 21.44 px. |
| 297.8 ms | Se aplica la fuente de iconos. Los textos de los pilares se corren +6.11 px (49.89 → 56), el texto vuelve a ocupar una línea más y todo baja 21.44 px. |

En la repetición 1 los dos cambios cayeron en el mismo fotograma y el efecto neto fue cero. Por eso solo falla una de las dos repeticiones.

**Tu pregunta:** sí, el salto viene de las métricas de bootstrap-icons sobre un `<i>` sin caja estable.
- En `pillars.component.css:3`, `article>i` solo tiene `font-size:22px`.
- Mientras la fuente de iconos está bloqueada (`font-display:block`, `marketing-icons.css:11`), el glyph se mide con la fuente de respaldo: unos 15.9 px de ancho. Con la fuente cargada mide 22 px, que es 1em.
- En 800 la columna de texto pasa de 339 a 333 px, y eso basta para que una línea más pase a la siguiente.
- Es el mismo +6.1 px que se ve en 1280 en los pilares (0.0003–0.0004) y en `.fl-home-agenda-entry`.

**Cambio mínimo (P1, Codex, `pillars.component.css`):**
```css
article>i{width:1em}
```
- El `<i>` es un elemento flex, así que respeta el ancho, y `flex-shrink:0` ya está.
- 1em es la caja propia de la fuente de iconos (se midió 20 × 20 a 20 px), no un ancho sacado de un texto. Con la fuente cargada no cambia nada, porque mide lo mismo; con la de respaldo, la columna de texto ya mide 333.
- **Lo que queda pendiente de medir:** con la columna fija, puede quedar un ancho en el que Manrope y la fuente de respaldo partan el texto en distinto número de líneas. Eso sería un residual del cambio de fuente, no de los iconos. Si aparece, se reporta; no se compensa con `min-height`.

Por ahora no recomiendo una regla global `.bi::before{width:1em}`: amplía el alcance, y el archivo de iconos se genera automáticamente. Queda como seguimiento.

## 3. Pruebas deterministas (Codex)

**R-nav: HTML entregado en dos partes.**
- Agregar a `public-preview.mjs` una opción solo para pruebas, `htmlSplit:{after, holdMs}`. Para `/`, envía sin gzip los bytes hasta el marcador, espera `holdMs` y luego envía el resto, en `chunked` y sin `Content-Length`.
- Puntos de corte:
  1. justo después de la etiqueta de apertura de `details.business-menu`;
  2. después del `</nav>` de `desktop-links`;
  3. después del `.sign-in` dentro de `.top-actions`.
- Tamaños: 1200, 1280, 1350 y 1440 de ancho, con fuentes libres y con fuentes retenidas.
- **Antes de la corrección**, el corte 1 debe reproducir:
  - una fuente en `desktop-links` con el borde derecho igual al del contenido de la barra;
  - `summary` y `.top-actions` en `null` en un snapshot tomado durante la pausa.
- **Después**, no debe haber fuentes en NAV ni en `top-actions`, y `initial` debe quedar en ≤ 0.01.
- Como diagnóstico, sin corregirlo en R3, reportar también un corte después de `</h1>`. La grilla del hero usa `align-items:center` y podría tener el mismo riesgo.

**R-iconos: retener las fuentes y liberarlas por separado.**
- Extender `probe-page` con `releaseFonts({only:'manrope'|'icons'})`.
- Tomar snapshots en los cuatro estados (respaldo/respaldo, M/respaldo, respaldo/I, M/I) y en los dos órdenes de liberación.
- Tamaños: 360, 375, 575, 576, 767, 768, 800, 1000, 1023, 1024, 1180, 1280 y 1440. El probe actual solo cubre 768–1024.
- **Antes de la corrección**, en 800 debe reproducir ±21.44 en `#como-funciona`.
- **Después**, en todos los estados:
  - el ancho de `article>i` es 22;
  - la x y la altura de cada `article` no cambian;
  - no hay fuentes en `#como-funciona`.

**Aserción global en la matriz.**
- `home-sections-measure.mjs:30` ya tiene el `assert.ok(initial.cls<=.01)` por muestra. Como la corrida 71ad736b0719 registró 0.0149 sin fallar, esa evidencia **se generó sin esta línea**, así que no cuenta como validación de la aserción.
- Pedido: no detenerse en la primera muestra que falla.
  - Juntar `violations:[{width,height,repeat,initialCls,sources}]` en todas las muestras `normal`.
  - Escribirlas en el JSON y en el resumen saneado, junto con `{limit:.01,max,violations}`.
  - Terminar con `exit 1` si hay alguna.
- Nunca descartar ni repetir una muestra para que pase.

## 4. Orden

1. Cuando terminen las cinco suites que están corriendo:
   - correr R-nav y R-iconos **antes** de la corrección, para reproducir los dos saltos;
   - aplicar los dos cambios de CSS;
   - repetir R-nav y R-iconos.
2. Repetir la matriz de 32 muestras `normal` con la aserción global, comparar huellas y alturas, y después la de 100 si la secuencia de R3 lo pide.
3. Hay un límite que se mantiene: solo se probó con Chrome headless en macOS. El momento en que Chrome pinta durante la lectura del HTML depende del motor y de la red; la regla con `:has` hace que ese momento deje de importar.

No autorizo commit, push, deploy ni prerender productivo. No toqué datos, precios, reglas, la fuente global ni las precargas.
