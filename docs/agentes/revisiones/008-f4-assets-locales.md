# Revisión 008 F4: recursos heredados locales

La revisión fue solo de lectura. No modifiqué código y no ejecuté builds, pruebas ni Lighthouse. Esta sesión no puede escribir archivos, así que propongo guardar este texto como `docs/agentes/revisiones/008-f4-recursos-locales.md`.

**Veredicto: el P1 queda aprobado en código. No encontré problemas de prioridad alta ni media.** El cierre de F4 todavía depende de los builds, la revisión de UI y las corridas de Lighthouse que tienes en curso.

## Lo que revisé y está correcto

**Copia y licencias**
- `booking-animate.css` coincide con animate.css 4.1.1 en los tres efectos que usa Booking:
  - **Base:** `.animate__animated` con duración de 1 s y relleno `both`.
  - **Keyframes:** `fadeIn`, `headShake` en 6.5/18.5/31.5/43.5/50 % y `fadeInUp` con `translate3d(0,100%,0)`.
  - **Extras:** se conservan las versiones `-webkit-` y el `ease-in-out` de headShake.
- El texto MIT está completo y no queda `:root` ni `var(--animate-*)`.
- `PlayfairDisplay-OFL.txt` incluye el copyright y el nombre reservado "Playfair Display". Usar los binarios sin modificar con el mismo nombre es compatible con la OFL, porque la restricción del nombre reservado solo aplica a versiones modificadas.

**Encapsulación de las animaciones**
- El proyecto usa Angular 21 (`package.json:36`), que en encapsulación emulada renombra `@keyframes` y `@-webkit-keyframes`. También reescribe `animation-name` y `-webkit-animation-name` cuando están en la misma hoja, y en este caso todas las referencias están en `booking-animate.css`.
- `booking.component.css` y `booking-experience.css` no usan esos nombres.
- El `fadeIn` de `admin-inventory-section.component.css:22` ya tenía su propio ámbito, así que no hay colisión. Antes compartía nombre con la versión global del CDN.
- Ningún otro archivo de `src/` usa clases `animate__`. El efecto queda limitado a Booking (`booking.component.ts:67`).
- Reducción de movimiento: la regla de `booking.component.css:1405` usa `animation: none !important`, que se impone al `animation-name` de la hoja de animaciones sin importar el orden de `styleUrls`.

**Aislamiento de las fuentes**
- Las fuentes se declaran con `@font-face` en `styles.css:8`, compartido por los dos builds, y no se precargan. Solo se descargan si algún elemento usa Playfair: `app.ts:158`, `admin-site-section.component.ts:187` y `.css:131`.
- La familia se llama exactamente `'Playfair Display'`, con peso 700, `font-display: swap` y cuatro subconjuntos.
- Las rutas son absolutas (`/fonts/tenant/...`) y del mismo origen, así que funcionan en la Home, en `/e/**` (que se sirve con el HTML de marketing) y en el build application. `public/` se copia en ambos.
- `index.html` e `index.app.html` ya no hacen ninguna petición a CDN.

**Guarda `check-public-assets`:** está bien enfocada. Revisa las entradas de los dos builds, los cuatro subconjuntos con su cabecera `wOF2` real y que cada efecto del template tenga su clase y su keyframe.

## Observaciones bajas

**B1 – Plantilla de Functions desactualizada (sin efecto en producción).**
- `functions/template/index.html:25,27` todavía incluye Google Fonts con Poppins y el CDN de animate.css.
- No afecta a producción: está en `.gitignore` (`functions/.gitignore:2`), y `loadCurrentTemplate` (`functions/index.js:25`) descarga el `index.html` de marketing en cada petición.
- Recomendación: regenerarla con `functions/scripts/prepare-template.mjs` después del build, o dejar constancia de que es un artefacto obsoleto, para que nadie la tome como referencia.

**B2 – La guarda solo revisa las fuentes, no el resultado del build.**
- Mi revisión anterior pedía hacer grep sobre `dist/*/browser/index.html`.
- Recomendación: después de los dos builds, ejecutar un grep de `googleapis|gstatic|cdnjs` sobre los dos `dist/*/browser/index.html` y registrar el resultado. No hace falta cambiar el script.

**B3 – Caché de `/fonts/tenant/*` (solo observación).**
- `firebase.json` no define cabeceras para `/fonts/**`, así que Firebase aplica su caché por defecto (una hora). Google servía estas fuentes con caché de un año.
- Como los nombres de archivo no llevan versión, ahora no conviene marcarlas como `immutable`. Si más adelante se quiere caché larga, habría que poner `v40` en el nombre del archivo y agregar la cabecera. Esto es un cambio de despliegue que queda fuera de este alcance.

**B4 – `stats.json`.**
- Confirma que se genera en `dist/marketing/stats.json`, fuera de `browser/`, y que no se agrega a ningún commit. Si es así, no se despliega, porque `public` apunta a `dist/marketing/browser`.

## Evidencia de Playfair sin cambiar datos

Comparto la decisión de no modificar ningún tenant para forzar la prueba. Hay dos alternativas que no tocan datos:

1. **Editor de sitio (build application), tema elegante:** la vista previa aplica Playfair por CSS (`admin-site-section.component.css:131`). Basta con cambiar la opción en el formulario y **no guardar**. Network debería mostrar solo `/fonts/tenant/playfair-display-700-latin.woff2`.
2. **Tenant INTER en `/e/<slug>`:** aplicar `font-family: "Playfair Display"` a un título desde Elements en DevTools. Eso confirma que se descarga el WOFF2 local y que se ve correctamente, sin cambiar la configuración.

En la misma revisión hay que confirmar que **no se descarga Playfair** en la Home ni en el tenant INTER sin cambios.

## Validación pendiente para cerrar

1. Builds de marketing y application con exit 0 y el delta de CSS/JS inicial, más el grep de B2.
2. Booking en 375×812, 820×1180 y 1440×900 con estilos calculados:
   - el nombre de animación debe ser el de ámbito, que termina en `fadeIn`, `headShake` o `fadeInUp`;
   - duración de 1 s y relleno `both`;
   - con reducción de movimiento emulada, la animación debe quedar en `none`.
3. Lighthouse: cinco corridas por URL, reportando mediana y rango. La auditoría de recursos que bloquean el render no debe listar terceros. Tal como dices, no se debe prometer un puntaje de 85; el TBT sigue pendiente del diagnóstico A con `stats.json`.
