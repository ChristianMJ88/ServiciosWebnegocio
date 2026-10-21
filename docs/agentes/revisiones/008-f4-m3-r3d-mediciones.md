# Revisión R3-d: mediciones, hero y reservas. Diseño correctivo

**Veredicto:** el CLS de 0.169 en 820 no viene de App ni de la hidratación. Tiene tres causas medidas y cada una se corrige con CSS acotado en marketing, sin datos ni backend:

| Causa | CLS en 820 | Momento |
|---|---|---|
| Cambio de fuente que reacomoda el encabezado del preview | 0.1364 | antes de que corra el JS |
| Chips de verticales que pasan de 1 a 2 filas | 0.0312 | después de que llegan los datos |
| Etiqueta del CTA | 0.00145 | después de que llegan los datos |

Solo el punto 1 cambia el aspecto visible, así que es el único que necesita que el usuario lo vea antes. Los puntos 2 y 3 entran en la regla de §1.7 que ya está aprobada.

Solo leí con Read, Glob y Grep: el JSON y el resumen de `/private/tmp`, el `index.html` del probe de producción, el CSS del hero, de Home, de los tokens y de los iconos, y los scripts `probe-page`, `browser-probe` y `home-sections-measure`. No ejecuté nada ni leí `.env`, `postman/` ni credenciales. No tengo herramienta de escritura, así que este texto hay que guardarlo como `revisiones/008-f4-m3-r3d-mediciones.md`.

## 1. Primer pintado: no es B3, es el título auxiliar del preview que cambia de fila

**Evidencia en el JSON:**
- **812×375, desplazamiento a los 267.4 ms** (antes de que se libere `main`): `.fl-home-preview-caption` («Vista ilustrativa») estaba en la segunda fila, en x=463.9 / y=241.2. Pasa a la primera fila, en x=681.3 / y=195.5, y su ancho baja de 86.6 a 84.6 px.
- **El encabezado encoge 31.1875 px.** Es la línea del caption (12 px × 1.6 = 19.1875) más el `gap:12px` entre filas de `.fl-home-preview-heading` (`hero.component.css:12`, con `flex-wrap:wrap` desde `:22`). En 820 eso arrastra a `hero-copy` 15.6 px (la mitad, por `align-items:center`), a `agenda-lines` y a `app-home-pillars` 31.19 px.
- **El caption está justo en el límite.** El hero ya cargado mide 610.94 = 579.75 + 31.19 en 768 y 800: ahí el caption queda en la segunda fila con las dos fuentes. En 812, 820 y 844 mide 579.75: con Manrope cabe en la primera fila. El cambio de fila con Manrope ocurre entre 800 y 812 px; con la fuente de respaldo (`system-ui` en macOS headless), entre 820 y 844.
- **Fuera de 768–1023 apenas hay desplazamiento antes del JS:** < 0.0008 en 1024, 1180, 1280, 1367 y 1440, y 0 en móvil. A partir de 1024 el preview mide al menos unos 449 px y sobran más de 90 px.
- **Iconos:** `bootstrap-icons` usa `font-display:block` y su caja pasa de 14.45×23 a 20×20 al cargar. Solo produce movimientos horizontales < 0.001.
- **El texto del hero no cambia de forma:** `hero-copy` mide 425.67 px con las dos fuentes, y el H1 tampoco cambia en esta muestra.
- **Es un efecto nuevo del prerender.** En el CSR actual no se pinta nada hasta que corre el JS, y para entonces las fuentes casi siempre ya llegaron.

**Opciones que pediste comparar:**

| Opción | Evaluación |
|---|---|
| Precargar fuentes solo en la entrada de marketing | **No en R3.** `src/index.html` es compartido: cambiaría `index.csr.html`, que el criterio de R3 exige idéntico byte a byte, y el build por defecto. En las rutas CSR (`/e/**`, `/acceso`) Chrome avisaría de un «preload not used», y la consola estricta del probe fallaría. Además no garantiza nada en una red lenta. Encaja en el paso de prerender productivo, inyectado solo en el HTML del Home. |
| Ajustar las métricas de la fuente de respaldo (`size-adjust` / `*-override`) | Los parámetros se pueden calcular de las tablas de la fuente, pero el ajuste de ancho es aproximado y depende de la fuente local de cada sistema. Con un margen de unos 10 px no asegura que el caption no cambie de fila. Se descarta como solución principal. |
| `font-display: optional` | Elimina el desplazamiento por completo, pero la primera visita con red lenta se vería con la tipografía del sistema. Eso es una decisión de marca que corresponde al usuario. No lo recomiendo ahora. |
| **Fijar la posición del caption en 768–1023** | **Recomendada.** Es determinista y no depende de la fuente, del sistema ni de la red. |

**Cambio propuesto (P1, `hero.component.css`, Codex):** dentro del rango 768–1023, que ya usan `product.component.css:14` y `pillars.component.css:5`:

```css
.fl-home-preview-caption{flex-basis:100%}
```

- Deja siempre el caption en la segunda fila, que es como ya se ve hoy entre 768 y unos 805 px.
- **Cuesta 31.19 px más de altura del hero entre unos 805 y 1023 px.** Ese es el cambio visual que el usuario tiene que aprobar, con capturas antes y después en 820 y 1000.
- Si no lo aprueba, la alternativa es aceptar ese desplazamiento como residual y dejar la precarga para el prerender productivo.
- No se toca el `:root`, las fuentes, `marketing-icons.css` (es generado), application ni tenant.

**Prueba antes y después (H1 del harness):** un escenario nuevo `fontHold` que retiene `/fonts/marketing/*.woff2` y `media/bootstrap-icons-*`, toma un snapshot con la fuente de respaldo y luego libera las fuentes. En 768, 800, 812, 820, 844, 900, 1000, 1023 y 1024:
- antes de la corrección debe reproducir los 0.136 y 0.115;
- después, la altura de `.fl-home-preview-heading` tiene que ser igual en los dos estados y no debe haber fuentes de desplazamiento en el hero antes de liberar `main`.

**B3, decidido sin corrección** (criterio de §1.6):
- El único desplazamiento grande antes del JS es el del caption.
- Después de liberar `main`, por encima del pliegue solo se mueven los chips y el CTA.
- Lo único que cambia es `--tenant-heading-font`, y afecta al `h3.footer-brand-title`, que está oculto visualmente (1×1).
- En los componentes de marketing no se usa `--tenant-*` ni `--primary-*`; el único uso, en `footer.component.css:307`, excluye explícitamente el footer de marketing.

## 2. Chips del hero en 768–1023 (P1)

- Medido: pasan de 44 a 96 px en los seis anchos del rango (768, 800, 812, 820, 844 y 900). 96 = 2 × 44 (`min-height` del chip) + 8 (`gap`).
- En el mismo rango (`hero.component.css`), con el mismo patrón que el `min-height:94px` móvil ya aprobado (2×44+6):

```css
.fl-home-hero-verticals{min-height:calc(2 * 44px + 8px)}
```

- Esto no cuenta chips ni usa datos. Como el `min-height` se queda en el mismo nodo, si el contenido mide menos solo queda espacio vacío, nunca un desplazamiento.
- **Hay que medir antes de aplicarlo en 960, 1000 y 1023.** Cerca de 1023 la columna mide unos 500 px y quizá los cinco chips caben en una fila. En ese caso quedarían 52 px vacíos, y con `nav503` (0 chips) también 96 px vacíos. Se muestra en las capturas.
- Una tercera fila, si los datos crecen, se reporta como residual.
- **CTA: aceptar el residual, sin `min-width`.** Es horizontal, entre 0.0009 y 0.0023 según el tamaño. Reservar ancho obligaría a suponer el largo de la etiqueta que llega de la API.

**Resultado esperado:** CLS de carga ≤ 0.0023 en los 16 tamaños en estado `normal` (lo que queda es el CTA más los iconos). En 820 se espera 0.169 → ~0.0015, y en 812×375 0.130 → ~0.

## 3. Reservas de secciones: mínimo por rango más espaciado del token (P1)

**Respuesta a tu pregunta:**
- Se conserva el mínimo y solo se aplica al placeholder.
- Se escribe con `--fl-space`, porque esa variable explica casi toda la variación dentro de cada rango: `clamp(40px,8vw,96px)` y `32px` en móvil (`marketing-tokens.css:9,33`).
- **No se pone `min-height` al contenedor ya cargado.** Dejaría espacio vacío y congelaría el layout.

**Por qué funciona:** si a la altura cargada se le resta 2·`--fl-space`, en 768–1023 queda casi constante. Por ejemplo, producto: 768 → 563.96; 820 → 563.96; 844 → 564.80; 900 → 567.08. Planes, FAQ y cierre se comportan igual, con menos de 3.1 px de diferencia.

**Fórmula:** en `fluora-home.component.css`,

```css
min-height:calc(var(--fl-reserve-X) + 2 * var(--fl-space))
```

con un `--fl-reserve-X` por rango igual al mínimo medido de (altura cargada − 2·space(w)). Las cifras provisionales, de una sola corrida en `normal`, se pueden comprobar con el JSON 5e39195ebd04:

| Rango | producto | planes | FAQ | cierre | Diferencia que queda (máx − mín) |
|---|---|---|---|---|---|
| ≤767 | 616.55 | 756.42 | 485.78 | 607.23 | 87 / 63 / 38 / 46 (lo marca 740×360; solo vertical: ≤20) |
| 768–1023 | 563.96 | 779.01 | 577.78 | 578.23 | ≤3.1 |
| 1024–1199 | 582.61 | 787.16 | 585.93 | 515.99 | 50 / 6.3 / 6.4 / 10.9 |
| ≥1200 | 653.84 | 805.64 | 596.41 | 533.84 | ≤6 |

**Notas:**
- **El placeholder actual ya encoge**, lo que va contra la regla de «solo crecer». En móvil, FAQ mide 700 frente a 549.78–588.13 cargado, y producto 720 frente a 680.55 en 740×360.
- **No dejar estas cifras como definitivas.** El script tiene que calcular las reservas con:
  - `normal` n=2, que debe dar alturas idénticas; en el JSON, 1024×768 y 1024×1367 ya coinciden, lo que confirma que dependen solo del ancho;
  - los anchos de borde 767, 1023, 1199 y 1200, para que el mínimo cubra todo el rango; se agregan a los 16 solo para calcular las reservas;
  - los escenarios de fallo y «Consultando planes…», que se reportan como residual porque probablemente encogen.
- El comentario en el CSS apunta a la huella del build y al JSON.
- Los datos son configurables, así que CLS ≤ 0.01 al desplazarse por las secciones sigue siendo informativo, no garantizado.

## 4. Harness

| ID | Prior. | Ubicación | Hallazgo y corrección |
|---|---|---|---|
| H1 | Media | `probe-page.mjs:53,58` | El snapshot `before` espera a `fonts.ready`, así que nunca captura el estado con la fuente de respaldo, y la causa se atribuye solo por tiempos (201.6 frente a 213.2 ms). Hay que agregar el escenario `fontHold` (patrones de Fetch para las fuentes) y registrar `responseEnd` de cada fuente y de la hoja desde `performance.getEntriesByType('resource')`. |
| H2 | Baja | `browser-probe.mjs:90` | Agregar `.fl-home-preview-heading` y `.fl-home-preview-caption` a `layout`, para verificar la corrección del punto 1 directamente. |
| H3 | Baja | `home-sections-measure.mjs:31` | `cls` y `shifts` por sección son acumulados. Hay que reportar el delta, es decir, los desplazamientos con `time` posterior al inicio de cada sección. |
| H4 | Baja | `probe-page.mjs:23` | `getResponseBody` y el hash no se esperan dentro de `settled()`. Las huellas salieron completas, pero no está garantizado: hay que juntar esas promesas y esperarlas. |
| H5 | Baja | `probe-page.mjs:63`, `home-sections-measure.mjs:38` | Si la preparación falla, `page` es `undefined` y no queda diagnóstico. Hay que adjuntar `error.diagnostic` (fase, logs, peticiones, stderr) antes de `close()`. |

Siguen pendientes para cerrar R3-c: la prueba de 20 ciclos sin perfiles residuales (M1), las anclas, GSAP, el resize y la verificación de CSR en desarrollo. Usar un Chrome por muestra es aceptable como límite declarado.

## 5. Orden y si hay que detenerse

1. Codex: H1–H5, la corrida con `fontHold` antes de corregir, la matriz completa (n=2, fallos, anchos de borde) y aplicar los puntos 2 y 3.
2. **El punto 1 se detiene para que el usuario lo vea**, con capturas en 820 y 1000 y la cifra de +31.19 px. Si no lo aprueba, el residual de 0.136/0.115 se documenta y la precarga pasa al prerender productivo.
3. Repetir la matriz con la misma huella de datos. Luego R3-e.

**Límites:**
- Solo se probó con Chrome headless en macOS. Otros sistemas tienen otras fuentes de respaldo; la corrección del punto 1 no depende de eso, pero la banda de 768–1023 se validó solo en este entorno.
- El tiempo de llegada de las fuentes en local (unos 200 ms) no representa una red real.

Esta revisión no autoriza commit, push, deploy ni prerender productivo.
